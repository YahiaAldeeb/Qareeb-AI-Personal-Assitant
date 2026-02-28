import os
import json
import uuid
import logging
import asyncio
import time
from typing import Optional

import whisper
from dotenv import load_dotenv
from groq import Groq
from llama_index.llms.groq import Groq as LlamaGroq
from droidrun import AgentConfig, DroidAgent, DroidrunConfig, CodeActConfig, AdbTools
from sqlalchemy.orm import Session

try:
    import adbutils
except ImportError:
    adbutils = None

from app.models.task import TaskRecord
from app.models.transaction import FinanceRecord
from app.controllers.task import create_task_controller
from app.controllers.transaction import create_transaction_controller

load_dotenv()

logger = logging.getLogger(__name__)

groq_client = Groq(api_key=os.environ.get("GROQ_API_KEY"))

llm = LlamaGroq(
    model="moonshotai/kimi-k2-instruct-0905",
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0,
)

# Fast LLM for UI automation using Groq (text-only, no vision needed)
# DroidRun works great without vision by using UI element descriptions
automation_llm = LlamaGroq(
    model="moonshotai/kimi-k2-instruct-0905",
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0,
)

# DroidRun configuration optimized for Groq (vision disabled for speed)
config = DroidrunConfig(
    agent=AgentConfig(
        after_sleep_action=0.5,  # Wait after each action for UI stability
        wait_for_stable_ui=0.3,  # Wait for UI to stabilize
        max_steps=20,  # Reduce max steps to avoid context overflow
        reasoning=False,  # Disable reasoning for faster execution
        streaming=False,  # Disable streaming in backend
        codeact=CodeActConfig(
            vision=False,  # Groq model doesn't support vision, but DroidRun works with UI tree
        ),
    )
)

# AdbTools with adbutils-based text input (bypasses Portal 401)
class AdbToolsWithAdbutilsInput(AdbTools):
    """Use adbutils for reliable text input via ADB shell."""

    async def input_text(self, text: str, index: int = -1, clear: bool = False) -> str:
        """
        Force real text injection (Content Provider mode) with verification.
        - Focus the element (tap)
        - Clear existing text (key events)
        - Inject text via ADB shell (adbutils if available)
        - Verify the text appears in the latest UI state; fail if not.
        """
        await self._ensure_connected()
        serial = getattr(self.device, "serial", None)

        async def _adb_shell(cmd: str):
            if adbutils and serial:
                def _run():
                    c = adbutils.AdbClient()
                    d = c.device(serial=serial)
                    return d.shell(cmd)
                return await asyncio.get_event_loop().run_in_executor(None, _run)
            if not getattr(self, "device", None):
                raise RuntimeError("ADB device not connected")
            return await self.device.shell(cmd)

        # 1) Focus
        if index != -1:
            await self.tap_by_index(index)
            await asyncio.sleep(0.3)

        # 2) Clear
        if clear:
            try:
                await _adb_shell("input keyevent KEYCODE_MOVE_END")
                for _ in range(40):
                    await _adb_shell("input keyevent KEYCODE_DEL")
                await asyncio.sleep(0.2)
            except Exception as e:
                logger.warning(f"input_text clear failed, continuing: {e}")

        # 3) Inject
        escaped = text.replace(" ", "%s").replace("'", "\\'")
        try:
            await _adb_shell(f"input text '{escaped}'")
        except Exception as e:
            logger.error(f"input_text adb shell failed: {e}")
            raise

        # 4) Verify
        try:
            state = await self.get_state_full()
            if isinstance(state, dict) and text and text in json.dumps(state):
                return f"Typed text via adb shell: {text}"
            raise RuntimeError("Text not reflected in UI after input_text")
        except Exception as e:
            logger.error(f"input_text verification failed: {e}")
            raise


# AdbTools singleton manager
class AdbToolsManager:
    """Singleton manager for AdbTools - single device connection."""
    _instance: Optional[AdbTools] = None
    _initialized: bool = False

    @classmethod
    def get_tools(cls) -> AdbTools:
        """Get or create AdbTools instance for the first connected device."""
        if cls._instance is None:
            try:
                cls._instance = AdbToolsWithAdbutilsInput(
                    serial=None,
                    use_tcp=False,
                    remote_tcp_port=8080,
                )
                cls._initialized = True
                logger.info("AdbTools instance created")
            except Exception as e:
                logger.error(f"Failed to create AdbTools: {e}")
                cls._instance = None
                cls._initialized = False
                raise RuntimeError(f"Could not create AdbTools: {e}")
        return cls._instance
    
    @classmethod
    def is_initialized(cls) -> bool:
        """Check if AdbTools is initialized."""
        return cls._initialized
    
    @classmethod
    def reset(cls):
        """Reset the AdbTools instance (useful for testing or reconnection)."""
        cls._instance = None
        cls._initialized = False
        logger.info("AdbTools reset")

UI_AUTOMATION_PROMPT = """
You are a mobile UI automation agent. Execute the task efficiently using minimal steps.

RULES:
1. Analyze the current screen state
2. Take direct actions: click(), type(), scroll(), swipe(), open_app()
3. Complete tasks in the fewest steps possible
4. If an element is visible, interact with it immediately
5. Use complete() when task is finished
6. Use fail() only if task is impossible

Be decisive and fast. No unnecessary explanations.
""".strip()

try:
    whisper_model = whisper.load_model("base")
    logger.info("Whisper model loaded successfully")
except Exception as e:
    logger.exception("Failed to load whisper model: %s", e)
    whisper_model = None


def transcribe(audio_path: str) -> str:
    import json
    from pathlib import Path
    
    DEBUG_LOG_PATH = Path(r"e:\Graduation project\Qareeb-AI-Personal-Assitant\.cursor\debug.log")
    
    def _write_debug_log(location: str, message: str, data: dict, hypothesis_id: str = None):
        try:
            log_entry = {
                "id": f"log_{int(__import__('time').time() * 1000)}",
                "timestamp": int(__import__('time').time() * 1000),
                "location": location,
                "message": message,
                "data": data,
                "runId": "debug_run"
            }
            if hypothesis_id:
                log_entry["hypothesisId"] = hypothesis_id
            
            with open(DEBUG_LOG_PATH, "a", encoding="utf-8") as f:
                f.write(json.dumps(log_entry) + "\n")
        except Exception as e:
            logger.error(f"Failed to write debug log: {e}")
    
    # #region agent log
    _write_debug_log("ai.py:52", "transcribe: ENTRY", {"audio_path": audio_path, "file_exists": os.path.exists(audio_path), "whisper_model_loaded": whisper_model is not None}, "D")
    # #endregion
    
    logger.info("transcribe: starting, audio_path=%s", audio_path)
    if not whisper_model:
        # #region agent log
        _write_debug_log("ai.py:56", "transcribe: Whisper model NOT loaded", {}, "D")
        # #endregion
        logger.error("transcribe: Whisper model not loaded")
        raise RuntimeError("Whisper model not loaded")
    
    # #region agent log
    _write_debug_log("ai.py:61", "transcribe: BEFORE whisper.transcribe", {"audio_path": audio_path, "file_size": os.path.getsize(audio_path) if os.path.exists(audio_path) else 0}, "D")
    # #endregion
    
    result = whisper_model.transcribe(audio_path, fp16=False, language="en")
    text = result["text"].strip()
    
    # #region agent log
    _write_debug_log("ai.py:65", "transcribe: AFTER whisper.transcribe", {"text": text, "text_length": len(text)}, "D")
    # #endregion
    
    logger.info("transcribe: finished, text=%r", text)
    return text


def extract_intent(user_input: str) -> str:
    logger.info("extract_intent: starting, user_input=%r", user_input)
    prompt = f"""
You are an intent classifier.
Classify the user request into EXACTLY one of these:
- UI_AUTOMATION
- FINANCE
- TASK_TRACKER
- UNKNOWN
Only output the intent name.

User request: "{user_input}"
"""
    attempts = 0
    backoff = 0.6
    last_err = None
    while attempts < 3:
        try:
            completion = groq_client.chat.completions.create(
                model="moonshotai/kimi-k2-instruct-0905",
                messages=[{"role": "user", "content": prompt}],
                temperature=0,
                max_completion_tokens=256,
                top_p=1,
                stream=False,
            )
            intent = completion.choices[0].message.content.strip()
            logger.info("extract_intent: finished, intent=%s", intent)
            return intent
        except Exception as e:
            last_err = e
            attempts += 1
            logger.warning(f"extract_intent attempt {attempts} failed: {e}")
            if attempts >= 3:
                break
            time.sleep(backoff)
            backoff *= 2
    logger.error(f"extract_intent: giving up after retries, last_err={last_err}")
    raise last_err


def _strip_code_fences(response_text: str) -> str:
    """Utility to strip ```json fences if the LLM returns them."""
    if "```json" in response_text:
        response_text = response_text.split("```json")[1].split("```")[0].strip()
    elif "```" in response_text:
        response_text = response_text.split("```")[1].split("```")[0].strip()
    return response_text


def extract_finance_data(text: str) -> dict:
    logger.info("extract_finance_data: starting, text=%r", text)
    schema_json = FinanceRecord.model_json_schema()
    prompt = f"""
You are a financial data extraction assistant. Extract structured financial information from the user's request.

User request: "{text}"

Extract the following information if mentioned:
- transactionID: transaction ID if mentioned (usually null for new transactions)
- amount: numeric value (as float)
- date: date in YYYY-MM-DD format (use today's date if not specified)
- source: where the money came from or went to
- description: what the transaction is about
- income: true if it's income/revenue, false if it's an expense
- state: transaction state/status (e.g., "pending", "completed", "cancelled")
- category_name: category name
- userID: user ID if mentioned, otherwise null
- created_at: creation timestamp in ISO format
- is_deleted: false

Output ONLY a valid JSON object matching this schema:
{json.dumps(schema_json, indent=2)}
"""
    completion = groq_client.chat.completions.create(
        model="moonshotai/kimi-k2-instruct-0905",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=512,
        top_p=1,
        stream=False,
    )
    response_text = completion.choices[0].message.content.strip()
    logger.debug("extract_finance_data: raw LLM response=%r", response_text)
    response_text = _strip_code_fences(response_text)

    try:
        data = json.loads(response_text)
    except json.JSONDecodeError as e:
        logger.exception("extract_finance_data: JSON decode error, text=%r", response_text)
        raise

    validated = FinanceRecord(**data)
    payload = validated.model_dump(exclude_none=True)
    logger.info("extract_finance_data: finished, payload=%s", payload)
    return payload


def extract_task_data(text: str) -> dict:
    logger.info("extract_task_data: starting, text=%r", text)
    schema_json = TaskRecord.model_json_schema()
    prompt = f"""
You are a task extraction assistant. Extract structured task information from the user's request.

User request: "{text}"

Extract the following information if mentioned:
- taskID: task ID
- userID: user ID
- title: task title
- description: detailed description
- status: task status
- progressPercentage: 0-100
- priority: task priority
- dueDate: due date in ISO
- created_at: creation timestamp
- updated_at: last update timestamp
- is_deleted: false

Output ONLY a valid JSON object matching this schema:
{json.dumps(schema_json, indent=2)}
"""
    completion = groq_client.chat.completions.create(
        model="moonshotai/kimi-k2-instruct-0905",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=512,
        top_p=1,
        stream=False,
    )
    response_text = completion.choices[0].message.content.strip()
    logger.debug("extract_task_data: raw LLM response=%r", response_text)
    response_text = _strip_code_fences(response_text)

    try:
        data = json.loads(response_text)
    except json.JSONDecodeError as e:
        logger.exception("extract_task_data: JSON decode error, text=%r", response_text)
        raise

    validated = TaskRecord(**data)
    payload = validated.model_dump(exclude_none=True)
    logger.info("extract_task_data: finished, payload=%s", payload)
    return payload


async def handle_finance_service(text: str, userID: str, db: Session) -> dict:
    logger.info("handle_finance_service: starting with text=%r, userID=%s", text, userID)
    
    if not userID or not userID.strip():
        logger.error("handle_finance_service: userID is required but was empty")
        return {"success": False, "error": "userID is required"}
    
    try:
        finance_data = extract_finance_data(text)

        # Ensure transactionID is set if missing, but use provided userID
        if not finance_data.get("transactionID"):
            finance_data["transactionID"] = str(uuid.uuid4())
        
        # Use the provided userID from the mobile app
        finance_data["userID"] = userID.strip()

        logger.info("handle_finance_service: final payload=%s", finance_data)

        # Call the same controller that the /transactions router uses
        controller_result = create_transaction_controller(finance_data, db)
        logger.info("handle_finance_service: controller_result=%s", controller_result)

        return {"success": True, "data": controller_result}
    except Exception as e:
        logger.exception("handle_finance_service: error")
        return {"success": False, "error": str(e)}


async def handle_task_tracker_service(text: str, userID: str, db: Session) -> dict:
    logger.info("handle_task_tracker_service: starting with text=%r, userID=%s", text, userID)
    
    if not userID or not userID.strip():
        logger.error("handle_task_tracker_service: userID is required but was empty")
        return {"success": False, "error": "userID is required"}
    
    try:
        task_data = extract_task_data(text)

        # Ensure taskID is set if missing, but use provided userID
        if not task_data.get("taskID"):
            task_data["taskID"] = str(uuid.uuid4())
        
        # Use the provided userID from the mobile app
        task_data["userID"] = userID.strip()

        logger.info("handle_task_tracker_service: final payload=%s", task_data)

        # Call the same controller that the /tasks router uses
        controller_result = create_task_controller(task_data, db)
        logger.info(
            "handle_task_tracker_service: controller_result=%s", controller_result
        )

        return {"success": True, "data": controller_result}
    except Exception as e:
        logger.exception("handle_task_tracker_service: error")
        return {"success": False, "error": str(e)}

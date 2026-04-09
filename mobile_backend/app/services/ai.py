import os
import uuid
import logging
import asyncio
import time
import json
from typing import Optional

import whisper
from dotenv import load_dotenv
from groq import Groq
from llama_index.llms.groq import Groq as LlamaGroq
from droidrun import AgentConfig, DroidAgent, DroidrunConfig, CodeActConfig, AdbTools
from sqlalchemy.orm import Session

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

automation_llm = LlamaGroq(
    model="moonshotai/kimi-k2-instruct-0905",
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0,
)

config = DroidrunConfig(
    agent=AgentConfig(
        after_sleep_action=0.5,
        wait_for_stable_ui=0.3,
        max_steps=20,
        reasoning=False,
        streaming=False,
        codeact=CodeActConfig(
            vision=False,
        ),
    )
)

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
    logger.info("transcribe: starting, audio_path=%s", audio_path)
    if not whisper_model:
        logger.error("transcribe: Whisper model not loaded")
        raise RuntimeError("Whisper model not loaded")

    result = whisper_model.transcribe(audio_path, fp16=False, language="en")
    text = result["text"].strip()

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
        logger.exception(
            "extract_finance_data: JSON decode error, text=%r", response_text
        )
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
    logger.info(
        "handle_finance_service: starting with text=%r, userID=%s", text, userID
    )

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
    logger.info(
        "handle_task_tracker_service: starting with text=%r, userID=%s", text, userID
    )

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

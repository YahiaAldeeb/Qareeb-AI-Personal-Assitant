import os
import uuid
import logging
import time
import json
from datetime import datetime, timedelta, timezone
import whisper
from dotenv import load_dotenv
from groq import Groq
from llama_index.llms.groq import Groq as LlamaGroq
from droidrun import AgentConfig, DroidrunConfig, CodeActConfig
from sqlalchemy.orm import Session

from app.models.task import TaskRecord
from app.models.transaction import FinanceRecord
from app.controllers.task import create_task_controller
from app.controllers.transaction import create_transaction_controller
from app.services.task import update_task_service
from app.services.memory import (
    get_user_memories,
    build_memory_context,
    extract_memory_facts,
    save_memory_facts,
)
from app.services.prompt_logger import save_interaction
from app.services.behavior import run_behavioral_analysis_if_needed

load_dotenv()
logger = logging.getLogger(__name__)

groq_client = Groq(api_key=os.environ.get("GROQ_API_KEY"))

llm = LlamaGroq(
    model="openai/gpt-oss-120b",
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0,
)

automation_llm = LlamaGroq(
    model="openai/gpt-oss-120b",
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0,
)

try:
    whisper_model = whisper.load_model("base")
    logger.info("Whisper model loaded successfully")
except Exception as e:
    logger.exception("Failed to load whisper model: %s", e)
    whisper_model = None


# ─────────────────────────────────────────────
# UTILITIES
# ─────────────────────────────────────────────

def transcribe(audio_path: str) -> str:
    if not whisper_model:
        raise RuntimeError("Whisper model not loaded")
    result = whisper_model.transcribe(audio_path, fp16=False, language="en")
    return result["text"].strip()


def get_date_context() -> str:
    today = datetime.now()
    today_weekday = today.weekday()
    day_names = ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]
    upcoming = {}
    for i, name in enumerate(day_names):
        delta = (i - today_weekday) % 7
        if delta == 0:
            delta = 7
        upcoming[name] = (today + timedelta(days=delta)).strftime("%Y-%m-%d")
    tomorrow = (today + timedelta(days=1)).strftime("%Y-%m-%d")
    return f"""
Today's date is {today.strftime("%Y-%m-%d")} ({day_names[today_weekday]}).
Tomorrow is {tomorrow}.
Upcoming weekdays (use these exact dates, do NOT calculate yourself):
This Monday: {upcoming['Monday']}
This Tuesday: {upcoming['Tuesday']}
This Wednesday: {upcoming['Wednesday']}
This Thursday: {upcoming['Thursday']}
This Friday: {upcoming['Friday']}
This Saturday: {upcoming['Saturday']}
This Sunday: {upcoming['Sunday']}
""".strip()


def _strip_code_fences(text: str) -> str:
    if "```json" in text:
        text = text.split("```json")[1].split("```")[0].strip()
    elif "```" in text:
        text = text.split("```")[1].split("```")[0].strip()
    return text


def get_last_user_message(text: str) -> str:
  # ✅ FIX: if text is a list, convert to string
    if isinstance(text, list):

        if len(text) == 0:
            return ""

        # take first item
        text = text[0]

    # ✅ safety
    text = str(text)

    lines = [
        l.strip()
        for l in text.strip().splitlines()
        if l.strip()
    ]

    if not lines:
        return ""

    return lines[-1]

def get_conversation_history(text: str) -> str:
       # ✅ FIX: handle list input
    if isinstance(text, list):

        # convert list into text conversation
        text = "\n".join([str(x) for x in text])

    # ✅ safety
    text = str(text)

    lines = [
        l.strip()
        for l in text.strip().splitlines()
        if l.strip()
    ]
    conversation = []

    for line in lines:

        if line.lower().startswith("user:"):
            conversation.append({
                "role": "user",
                "content": line[5:].strip()
            })

        elif line.lower().startswith("assistant:"):
            conversation.append({
                "role": "assistant",
                "content": line[10:].strip()
            })

        else:
            conversation.append({
                "role": "user",
                "content": line
            })

    return conversation


def _is_update_request(text: str) -> bool:
    update_keywords = [
        "change", "update", "modify", "reschedule", "move",
        "edit", "shift", "postpone", "push", "mark", "delete", "cancel"
    ]
    return any(keyword in text.lower() for keyword in update_keywords)


# ─────────────────────────────────────────────
# INTENT
# ─────────────────────────────────────────────

def extract_intent(user_input: str) -> str:
    logger.info("extract_intent: starting, user_input=%r", user_input)
    prompt = f"""
You are a STRICT intent classifier.

Rules:
If the user mentions an event, plan, reminder, or schedule → TASK_TRACKER
If the user mentions money → FINANCE
If the user wants to control apps → UI_AUTOMATION
Otherwise → UNKNOWN

User request: "{user_input}"

Output ONLY one:
TASK_TRACKER, FINANCE, UI_AUTOMATION, UNKNOWN
"""
    attempts, backoff, last_err = 0, 0.6, None
    while attempts < 3:
        try:
            completion = groq_client.chat.completions.create(
                model="openai/gpt-oss-120b",
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
            if attempts >= 3:
                break
            time.sleep(backoff)
            backoff *= 2
    raise last_err


# ─────────────────────────────────────────────
# FINANCE
# ─────────────────────────────────────────────

def extract_finance_data(text: str, memory_context: str = "") -> dict:
    schema_json = FinanceRecord.model_json_schema()
    prompt = f"""
You are a financial data extraction assistant.
{memory_context}

User request: "{text}"

Extract:
- transactionID: null for new transactions
- amount: numeric float
- date: YYYY-MM-DD (today if not specified)
- source: where money came from/went
- description: what the transaction is about
- income: true if income, false if expense
- state: "pending", "completed", or "cancelled"
- category_name: category
- is_deleted: false

Output ONLY valid JSON matching this schema:
{json.dumps(schema_json, indent=2)}
"""
    completion = groq_client.chat.completions.create(
        model="openai/gpt-oss-120b",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=512,
        top_p=1,
        stream=False,
    )
    response_text = _strip_code_fences(completion.choices[0].message.content.strip())
    data = json.loads(response_text)
    validated = FinanceRecord(**data)
    return validated.model_dump(exclude_none=True)


async def handle_finance_service(text: str, userID: str, db: Session) -> dict:
    logger.info("handle_finance_service: starting, userID=%s", userID)
    if not userID or not userID.strip():
        return {"success": False, "error": "userID is required"}
    try:
        memories = get_user_memories(db, userID)
        memory_context = build_memory_context(memories)
        logger.info("handle_finance_service: loaded %d memories", len(memories))

        finance_data = extract_finance_data(text, memory_context)
        if not finance_data.get("transactionID"):
            finance_data["transactionID"] = str(uuid.uuid4())
        finance_data["userID"] = userID.strip()

        controller_result = create_transaction_controller(finance_data, db)

        clean_text = get_last_user_message(text)
        ai_response = f"Recorded transaction: {finance_data.get('description', '')}"

        # Save interaction to prompts table
        save_interaction(
            db=db,
            userID=userID,
            user_message=clean_text,
            qareeb_response=ai_response,
            intent="FINANCE",
            module="FINANCE",
        )

        # Extract explicit memories
        new_facts = extract_memory_facts(
            f"User: {clean_text}\nQareeb: {ai_response}",
            memories
        )
        save_memory_facts(db, userID, new_facts)

        # Run behavioral analysis every 5 interactions
        await run_behavioral_analysis_if_needed(db, userID)

        return {"success": True, "data": controller_result}
    except Exception as e:
        logger.exception("handle_finance_service: error")
        return {"success": False, "error": str(e)}


# ─────────────────────────────────────────────
# TASK TRACKER
# ─────────────────────────────────────────────

def extract_task_data(text: str, memory_context: str = "") -> dict:
    schema_json = TaskRecord.model_json_schema()
    date_context = get_date_context()
    prompt = f"""
You are a task extraction assistant.
{date_context}
{memory_context}

IMPORTANT: Use ONLY the exact dates listed above. Do NOT calculate dates yourself.
User request: "{text}"

Extract:
- title: short 2-4 words
- description: one natural sentence
- status: default "pending"
- progressPercentage: default 0
- priority: default "medium"
- dueDate: exact date from above only
- is_deleted: false

If memory says user has a recurring schedule for this task type,
use that day's date automatically even if user didn't specify.

Output ONLY valid JSON matching this schema:
{json.dumps(schema_json, indent=2)}
"""
    completion = groq_client.chat.completions.create(
        model="openai/gpt-oss-120b",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=512,
        top_p=1,
        stream=False,
    )
    response_text = _strip_code_fences(completion.choices[0].message.content.strip())
    data = json.loads(response_text)
    validated = TaskRecord(**data)
    return validated.model_dump(exclude_none=True)


def extract_task_update_data(
    text: str,
    conversation_history: str,
    memory_context: str = ""
) -> dict:
    date_context = get_date_context()
    prompt = f"""
You are a task update assistant.
{date_context}
{memory_context}

Recent conversation:
{conversation_history}

User's update request: "{text}"

1. Identify which task title they mean from conversation
2. Extract ONLY the fields they want to change

Rules:
- Only include explicitly changed fields
- "mark as done" → status="completed", progressPercentage=100
- "delete it" → is_deleted=true
- target_title: infer from conversation

Output ONLY:
{{
  "target_title": "Task Title Here",
  "changes": {{
    "dueDate": "2026-05-21"
  }}
}}
"""
    completion = groq_client.chat.completions.create(
        model="openai/gpt-oss-120b",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=512,
        top_p=1,
        stream=False,
    )
    response_text = _strip_code_fences(completion.choices[0].message.content.strip())
    return json.loads(response_text)

async def handle_suggestion_check(userID: str, db: Session) -> dict | None:
    """
    Called on every interaction to check if Qareeb should
    proactively suggest a task based on user memories.
    Returns suggestion dict if there's something to suggest, None otherwise.
    """
    try:
        from app.services.suggestion import get_upcoming_suggestions
        memories = get_user_memories(db, userID)
        if not memories:
            logger.info("handle_suggestion_check: no memories for userID=%s", userID)
            return None

        suggestion = get_upcoming_suggestions(db, userID, memories)
        if suggestion:
            logger.info(
                "handle_suggestion_check: suggestion found for userID=%s: %r",
                userID, suggestion
            )
            return {
                "has_suggestion": True,
                "suggestion": suggestion,
            }
        logger.info("handle_suggestion_check: no suggestion for userID=%s", userID)
        return None
    except Exception as e:
        logger.exception("handle_suggestion_check: error=%s", e)
        return None
async def handle_task_tracker_service(text: str, userID: str, db: Session) -> dict:
    logger.info("handle_task_tracker_service: starting, userID=%s", userID)
    if not userID or not userID.strip():
        return {"success": False, "error": "userID is required"}

    try:
        clean_text = get_last_user_message(text)
        conversation_history = get_conversation_history(text)
        is_update = _is_update_request(clean_text)

        memories = get_user_memories(db, userID)
        memory_context = build_memory_context(memories)
        logger.info(
            "handle_task_tracker_service: loaded %d memories: %s",
            len(memories), memories
        )

        # ── UPDATE ───────────────────────────────────────────────────────
        if is_update:
            logger.info("handle_task_tracker_service: routing to UPDATE flow")
            update_data = extract_task_update_data(
                clean_text, conversation_history, memory_context
            )
            target_title = update_data.get("target_title")
            changes = update_data.get("changes", {})

            if not changes:
                return {"success": False, "error": "No changes detected"}

            from sqlalchemy import text as sql_text

            task_row = None
            if target_title:
                task_row = db.execute(
                    sql_text(
                        'SELECT * FROM "Task" WHERE "userID" = :userID '
                        'AND LOWER(title) LIKE LOWER(:title) '
                        'AND is_deleted = false '
                        'ORDER BY created_at DESC LIMIT 1'
                    ),
                    {"userID": userID, "title": f"%{target_title}%"}
                ).mappings().first()

            if not task_row:
                task_row = db.execute(
                    sql_text(
                        'SELECT * FROM "Task" WHERE "userID" = :userID '
                        'AND is_deleted = false '
                        'ORDER BY created_at DESC LIMIT 1'
                    ),
                    {"userID": userID}
                ).mappings().first()

            if not task_row:
                return {"success": False, "error": "No task found to update"}

            task_id = str(task_row["taskID"])
            changes["updated_at"] = datetime.now(timezone.utc).isoformat()
            updated_task = update_task_service(db, task_id, changes)

            ai_response = f"Updated task '{task_row['title']}': {changes}"
            save_interaction(db=db, userID=userID, user_message=clean_text,
                           qareeb_response=ai_response, intent="TASK_TRACKER", module="TASK_TRACKER")
            new_facts = extract_memory_facts(f"User: {clean_text}\nQareeb: {ai_response}", memories)
            save_memory_facts(db, userID, new_facts)
            await run_behavioral_analysis_if_needed(db, userID)

            return {"success": True, "data": {"task": updated_task}}

        # ── CREATE ───────────────────────────────────────────────────────
        else:
            logger.info("handle_task_tracker_service: routing to CREATE flow")

            # ✅ Fix: if text is a yes response, don't extract task from it
            yes_words = ["yes", "yeah", "yep", "sure", "ok", "okay",
                        "do it", "add it", "create it", "go ahead", "please",
                        "yes please"]
            if clean_text.lower().strip() in yes_words or clean_text.lower() == "yes please":
                logger.warning(
                    "handle_task_tracker_service: received yes/confirmation text=%r, "
                    "cannot create task without context", clean_text
                )
                return {"success": False, "error": "No task context provided"}

            task_data = extract_task_data(clean_text, memory_context)
            task_data["userID"] = userID.strip()
            task_data["created_at"] = datetime.now(timezone.utc).isoformat()
            task_data["updated_at"] = datetime.now(timezone.utc).isoformat()

            controller_result = create_task_controller(task_data, db)
            task_title = task_data.get("title", "")
            ai_response = f"Created task: {task_title}"

            save_interaction(db=db, userID=userID, user_message=clean_text,
                           qareeb_response=ai_response, intent="TASK_TRACKER", module="TASK_TRACKER")
            new_facts = extract_memory_facts(f"User: {clean_text}\nQareeb: {ai_response}", memories)
            save_memory_facts(db, userID, new_facts)
            await run_behavioral_analysis_if_needed(db, userID)

            return {"success": True, "data": controller_result}

    except Exception as e:
        logger.exception("handle_task_tracker_service: error")
        return {"success": False, "error": str(e)}
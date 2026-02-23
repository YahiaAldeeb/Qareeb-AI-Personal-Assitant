import os
import json
import uuid
import whisper
import aiofiles
from dotenv import load_dotenv
from groq import Groq
from llama_index.llms.groq import Groq as LlamaGroq
from droidrun import AgentConfig, DroidAgent, DroidrunConfig, CodeActConfig
from sqlalchemy.orm import Session

from app.models.task import TaskRecord
from app.models.transaction import FinanceRecord
from app.services.task import create_task_service
from app.services.transaction import create_transaction_service

load_dotenv()

groq_client = Groq(api_key=os.environ.get("GROQ_API_KEY"))

llm = LlamaGroq(
    model="openai/gpt-oss-120b",
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0,
)

config = DroidrunConfig(
    agent=AgentConfig(
        after_sleep_action=0.3,
        wait_for_stable_ui=0.1,
        codeact=CodeActConfig(vision=False),
    )
)

UI_AUTOMATION_PROMPT = """
Mobile UI agent. Continuous.
Minimal actions. No explanations.
Output only actions or FAILED.
""".strip()

try:
    whisper_model = whisper.load_model("base")
except Exception as e:
    print(f"Failed to load whisper model: {e}")
    whisper_model = None

def transcribe(audio_path: str) -> str:
    if not whisper_model:
        raise RuntimeError("Whisper model not loaded")
    result = whisper_model.transcribe(audio_path, fp16=False, language="en")
    return result["text"].strip()

def extract_intent(user_input: str) -> str:
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
    completion = groq_client.chat.completions.create(
        model="openai/gpt-oss-120b",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=256,
        top_p=1,
        reasoning_effort="medium",
        stream=False,
    )
    return completion.choices[0].message.content.strip()

def extract_finance_data(text: str) -> dict:
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
        model="openai/gpt-oss-120b",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=512,
        top_p=1,
        reasoning_effort="medium",
        stream=False,
    )
    response_text = completion.choices[0].message.content.strip()
    if "```json" in response_text:
        response_text = response_text.split("```json")[1].split("```")[0].strip()
    elif "```" in response_text:
        response_text = response_text.split("```")[1].split("```")[0].strip()
    data = json.loads(response_text)
    validated = FinanceRecord(**data)
    return validated.model_dump(exclude_none=True)

def extract_task_data(text: str) -> dict:
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
        model="openai/gpt-oss-120b",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=512,
        top_p=1,
        reasoning_effort="medium",
        stream=False,
    )
    response_text = completion.choices[0].message.content.strip()
    if "```json" in response_text:
        response_text = response_text.split("```json")[1].split("```")[0].strip()
    elif "```" in response_text:
        response_text = response_text.split("```")[1].split("```")[0].strip()
    data = json.loads(response_text)
    validated = TaskRecord(**data)
    return validated.model_dump(exclude_none=True)

async def handle_finance_service(text: str, db: Session) -> dict:
    try:
        finance_data = extract_finance_data(text)
        # Default UUID logic or rely on extract_finance_data if needed
        if "transactionID" not in finance_data:
            finance_data["transactionID"] = str(uuid.uuid4())
            finance_data["userID"] = finance_data.get("userID", str(uuid.uuid4()))
        result = create_transaction_service(db, finance_data)
        return {"success": True, "data": result}
    except Exception as e:
        return {"success": False, "error": str(e)}

async def handle_task_tracker_service(text: str, db: Session) -> dict:
    try:
        task_data = extract_task_data(text)
        if "taskID" not in task_data:
            task_data["taskID"] = str(uuid.uuid4())
            task_data["userID"] = task_data.get("userID", str(uuid.uuid4()))
        result = create_task_service(db, task_data)
        return {"success": True, "data": result}
    except Exception as e:
        return {"success": False, "error": str(e)}

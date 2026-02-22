import os
import json
from datetime import datetime
from typing import Optional

import aiofiles
import httpx
import uvicorn
import whisper
from dotenv import load_dotenv
from fastapi import FastAPI, File, UploadFile
from groq import Groq
from droidrun import AgentConfig, DroidAgent, DroidrunConfig, CodeActConfig
from llama_index.llms.groq import Groq as LlamaGroq

from models import FinanceRecord, TaskRecord


# Load environment variables from a local .env file so GROQ_API_KEY and others are available.
load_dotenv()
app = FastAPI()

# ===============================
# LLM clients (Groq + LlamaIndex)
# ===============================
groq_client = Groq(api_key=os.environ.get("GROQ_API_KEY"))

llm = LlamaGroq(
    model="openai/gpt-oss-120b",
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0,
)


# ===============================
# UI automation agent (Droidrun)
# ===============================
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


# ===============================
# Speech‑to‑text (Whisper)
# ===============================
print("Loading Whisper model...")
whisper_model = whisper.load_model("base")


def transcribe(audio_path: str) -> str:
    """
    Convert an audio file on disk into text using Whisper.
    """
    result = whisper_model.transcribe(audio_path, fp16=False, language="en")
    return result["text"].strip()


# ===============================
# Intent orchestrator
# ===============================
def extract_intent(user_input: str) -> str:
    """
    Ask the Groq LLM to classify the user's command into a single intent.
    """
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


# ===============================
# Backend API configuration
# ===============================
FINANCE_API_URL = os.environ.get("FINANCE_API_URL", "http://localhost:8001/api/finance")
TASK_TRACKER_API_URL = os.environ.get("TASK_TRACKER_API_URL", "http://localhost:8002/api/tasks")


# ===============================
# LLM-based data extraction
# ===============================
def extract_finance_data(text: str) -> dict:
    """
    Use Groq LLM to extract structured finance data from natural language text.
    Returns a dictionary matching the FinanceRecord schema.
    """
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
- category_name: category name like "Food", "Transport", "Salary", "Entertainment", etc.
- user_ID: user ID if mentioned, otherwise null
- created_at: creation timestamp in ISO format (use current timestamp if not specified)
- is_deleted: false for new transactions (set to true only if explicitly mentioned as deleted)

Output ONLY a valid JSON object matching this schema:
{json.dumps(schema_json, indent=2)}

Do not include any explanation, only the JSON object. If a field is not mentioned, set it to null.
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
    print(f"\n🔍 LLM Raw Response (Finance):\n{response_text}\n")
    
    # Extract JSON from response (handle cases where LLM adds markdown formatting)
    original_response = response_text
    if "```json" in response_text:
        response_text = response_text.split("```json")[1].split("```")[0].strip()
    elif "```" in response_text:
        response_text = response_text.split("```")[1].split("```")[0].strip()
    
    print(f"📝 Extracted JSON String (Finance):\n{response_text}\n")
    
    try:
        data = json.loads(response_text)
        print(f"📊 Parsed JSON Data (Finance):\n{json.dumps(data, indent=2)}\n")
        
        # Validate against Pydantic schema
        validated = FinanceRecord(**data)
        final_data = validated.model_dump(exclude_none=True)
        
        print(f"✅ Validated & Final JSON (Finance):\n{json.dumps(final_data, indent=2)}\n")
        return final_data
    except Exception as e:
        print(f"❌ Error parsing finance data: {e}")
        print(f"LLM response: {original_response}")
        raise


def extract_task_data(text: str) -> dict:
    """
    Use Groq LLM to extract structured task data from natural language text.
    Returns a dictionary matching the TaskRecord schema.
    """
    schema_json = TaskRecord.model_json_schema()
    
    prompt = f"""
You are a task extraction assistant. Extract structured task information from the user's request.

User request: "{text}"

Extract the following information if mentioned:
- taskID: task ID if mentioned (usually null for new tasks)
- user_ID: user ID if mentioned, otherwise null
- title: task title/name
- description: detailed task description
- status: task status (e.g., "pending", "in_progress", "completed", "cancelled")
- progressPercentage: progress percentage as integer (0-100)
- priority: task priority (e.g., "low", "medium", "high", "urgent")
- dueDate: due date in YYYY-MM-DD or ISO format (YYYY-MM-DDTHH:MM:SS)
- created_at: creation timestamp in ISO format (use current timestamp if not specified)
- updated_at: last update timestamp in ISO format (use current timestamp if not specified)
- is_deleted: false for new tasks (set to true only if explicitly mentioned as deleted)

Output ONLY a valid JSON object matching this schema:
{json.dumps(schema_json, indent=2)}

Do not include any explanation, only the JSON object. If a field is not mentioned, set it to null.
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
    print(f"\n🔍 LLM Raw Response (Task):\n{response_text}\n")
    
    # Extract JSON from response (handle cases where LLM adds markdown formatting)
    original_response = response_text
    if "```json" in response_text:
        response_text = response_text.split("```json")[1].split("```")[0].strip()
    elif "```" in response_text:
        response_text = response_text.split("```")[1].split("```")[0].strip()
    
    print(f"📝 Extracted JSON String (Task):\n{response_text}\n")
    
    try:
        data = json.loads(response_text)
        print(f"📊 Parsed JSON Data (Task):\n{json.dumps(data, indent=2)}\n")
        
        # Validate against Pydantic schema
        validated = TaskRecord(**data)
        final_data = validated.model_dump(exclude_none=True)
        
        print(f"✅ Validated & Final JSON (Task):\n{json.dumps(final_data, indent=2)}\n")
        return final_data
    except Exception as e:
        print(f"❌ Error parsing task data: {e}")
        print(f"LLM response: {original_response}")
        raise


# ===============================
# Business logic services
# ===============================
async def handle_finance(text: str) -> dict:
    """
    Handle finance-related commands:
    1. Extract structured data from text using LLM
    2. Send POST request to finance backend API
    3. Return the API response
    """
    try:
        print(f"💰 Processing finance request: {text}")
        
        # Extract structured data using LLM
        finance_data = extract_finance_data(text)
        print(f"📊 Extracted finance data: {finance_data}")
        
        # Send to backend API
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                FINANCE_API_URL,
                json=finance_data,
                headers={"Content-Type": "application/json"}
            )
            response.raise_for_status()
            result = response.json()
            print(f"✅ Finance record created: {result}")
            return {"success": True, "data": result}
            
    except httpx.HTTPError as e:
        error_msg = f"Finance API error: {str(e)}"
        print(f"❌ {error_msg}")
        return {"success": False, "error": error_msg}
    except Exception as e:
        error_msg = f"Finance processing error: {str(e)}"
        print(f"❌ {error_msg}")
        return {"success": False, "error": error_msg}


async def handle_task_tracker(text: str) -> dict:
    """
    Handle task-tracking commands:
    1. Extract structured data from text using LLM
    2. Send POST request to task tracker backend API
    3. Return the API response
    """
    try:
        print(f"📋 Processing task request: {text}")
        
        # Extract structured data using LLM
        task_data = extract_task_data(text)
        print(f"📊 Extracted task data: {task_data}")
        
        # Send to backend API
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                TASK_TRACKER_API_URL,
                json=task_data,
                headers={"Content-Type": "application/json"}
            )
            response.raise_for_status()
            result = response.json()
            print(f"✅ Task created: {result}")
            return {"success": True, "data": result}
            
    except httpx.HTTPError as e:
        error_msg = f"Task tracker API error: {str(e)}"
        print(f"❌ {error_msg}")
        return {"success": False, "error": error_msg}
    except Exception as e:
        error_msg = f"Task processing error: {str(e)}"
        print(f"❌ {error_msg}")
        return {"success": False, "error": error_msg}


# ===============================
# Main pipeline endpoint
# ===============================
@app.post("/transcribe")
async def process_command(file: UploadFile = File(...)):
    """
    Full command pipeline:
    1. Receive and save the recorded audio from the client.
    2. Transcribe audio to text with Whisper.
    3. Ask the LLM to classify the intent.
    4. Route to the appropriate handler (UI automation / finance / tasks / unknown).
    """

    temp_audio = "command.wav"
    try:
        # 1. Save uploaded audio to disk
        async with aiofiles.open(temp_audio, "wb") as out:
            content = await file.read()
            await out.write(content)

        # 2. Transcribe audio into text
        text = transcribe(temp_audio)
        print("Transcription:", text)

        # 3. Detect user intent from the transcription
        intent = extract_intent(text)
        print("Intent:", intent)

        # 4. Route intent to the correct handler
        if intent == "UI_AUTOMATION":
            print("Executing UI automation")

            agent = DroidAgent(
                goal=f"{UI_AUTOMATION_PROMPT}\n\nTASK:\n{text}",
                config=config,
                llms={
                    "manager": llm,
                    "executor": llm,
                    "codeact": llm,
                    "text_manipulator": llm,
                    "app_opener": llm
                }
            )

            result = await agent.run()

            # Return result of UI automation back to the client
            return {
                "status": "success",
                "intent": intent,
                "transcription": text,
                "automation_success": result.success
            }

        elif intent == "FINANCE":
            # Extract finance data and send to backend API
            result = await handle_finance(text)
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "transcription": text,
                "result": result
            }

        elif intent == "TASK_TRACKER":
            # Extract task data and send to backend API
            result = await handle_task_tracker(text)
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "transcription": text,
                "result": result
            }

        else:
            # Intent could not be classified confidently.
            return {"status": "unknown_intent", "transcription": text}

    except Exception as e:
        # Catch any unexpected errors in the pipeline so the client gets a JSON error response.
        print("Error in /transcribe pipeline:", e)
        return {"status": "error", "message": str(e)}

    finally:
        # Always clean up the temporary audio file.
        if os.path.exists(temp_audio):
            os.remove(temp_audio)

# ===============================
# Local development server entrypoint
# ===============================
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)

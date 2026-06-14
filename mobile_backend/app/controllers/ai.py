import os
import logging
import tempfile
import subprocess
import uuid
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
VENV_PYTHON = PROJECT_ROOT / "venv" / "Scripts" / "python.exe"

import aiofiles
from fastapi import UploadFile
from sqlalchemy.orm import Session

from app.services.ai import (
    transcribe,
    extract_intent,
    handle_finance_service,
    handle_task_tracker_service,
    handle_suggestion_check,
)
from app.services.memory import get_user_memories
from app.services.suggestion import handle_suggestion_response

logger = logging.getLogger(__name__)

automation_jobs: dict[str, dict] = {}
executor = ThreadPoolExecutor(max_workers=2)

# Store pending suggestions per user in memory
pending_suggestions: dict[str, str] = {}


def run_droidrun_sync(command: str) -> dict:
    logger.info(f"run_droidrun_sync: received command={command}")

    groq_key = os.environ.get("GROQ_API_KEY")
    portal_token = os.environ.get("PORTAL_AUTH_TOKEN")

    env = os.environ.copy()
    if groq_key:
        env["GROQ_API_KEY"] = groq_key
    if portal_token:
        env["PORTAL_AUTH_TOKEN"] = portal_token

    env["PYTHONIOENCODING"] = "utf-8"

    cmd = [
        str(VENV_PYTHON),
        "-m", "droidrun", "run", command,
        "--provider", "Groq",
        "--model", "openai/gpt-oss-120b",
        "--tcp", "--no-stream",
    ]

    logger.info(f"run_droidrun_sync: executing cmd={cmd}")

    try:
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=env,
            encoding="utf-8",
            errors="replace",
            bufsize=1,
        )

        output_lines = []
        for line in process.stdout:
            output_lines.append(line)
            logger.info(f"droidrun: {line.rstrip()}")

        process.wait()
        returncode = process.returncode
        logger.info(f"run_droidrun_sync: subprocess completed, returncode={returncode}")

        return {
            "status": "success" if returncode == 0 else "failed",
            "returncode": returncode,
            "stdout": "".join(output_lines),
            "stderr": "",
            "completed_at": datetime.now().isoformat(),
        }
    except subprocess.TimeoutExpired:
        logger.error("run_droidrun_sync: Timeout after 5 minutes")
        return {"status": "failed", "error": "Timeout after 5 minutes"}
    except Exception as e:
        logger.exception(f"run_droidrun_sync: Exception: {e}")
        return {"status": "failed", "error": str(e)}


async def process_command_controller(file: UploadFile, userID: str, db: Session):
    temp_dir = Path(tempfile.gettempdir()) / "qareeb_audio"
    temp_dir.mkdir(parents=True, exist_ok=True)

    content_type = file.content_type or ""
    if "mp3" in content_type.lower():
        ext = ".mp3"
    elif "wav" in content_type.lower():
        ext = ".wav"
    elif "m4a" in content_type.lower():
        ext = ".m4a"
    else:
        ext = ".wav"

    temp_audio = str(temp_dir / f"command_{os.urandom(4).hex()}{ext}")
    logger.info("process_command_controller: started, temp_audio=%s, userID=%s", temp_audio, userID)

    try:
        async with aiofiles.open(temp_audio, "wb") as out:
            content = await file.read()
            logger.info("process_command_controller: received file, size=%s bytes", len(content))
            await out.write(content)

        text = transcribe(temp_audio)
        logger.info("process_command_controller: transcription done text=%r", text)

        intent = extract_intent(text)
        logger.info("process_command_controller: extracted intent=%s", intent)

        if intent == "UI_AUTOMATION":
            job_id = str(uuid.uuid4())
            future = executor.submit(run_droidrun_sync, text)
            automation_jobs[job_id] = {
                "status": "running",
                "command": text,
                "started_at": datetime.now().isoformat(),
                "future": future,
            }
            return {"status": "accepted", "intent": intent, "transcription": text, "job_id": job_id}

        elif intent == "FINANCE":
            result = await handle_finance_service(text, userID, db)
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent, "transcription": text, "result": result,
            }

        elif intent == "TASK_TRACKER":
            result = await handle_task_tracker_service(text, userID, db)
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent, "transcription": text, "result": result,
            }

        else:
            return {"status": "unknown_intent", "intent": intent, "transcription": text}

    except Exception as e:
        logger.exception("process_command_controller: unexpected error")
        return {"status": "error", "message": str(e), "stage": "process_command_controller"}

    finally:
        if os.path.exists(temp_audio):
            try:
                os.remove(temp_audio)
            except Exception:
                logger.exception("process_command_controller: failed to remove temp_audio=%s", temp_audio)


async def process_text_controller(text: str, userID: str, db: Session):
    logger.info("process_text_controller: started, text=%r, userID=%s", text, userID)

    if not userID or not userID.strip():
        return {"status": "error", "message": "userID is required"}

    try:
        clean_text = text.strip()

        # ── STEP 1: Check if user is responding YES to a pending suggestion ──
        if userID in pending_suggestions and handle_suggestion_response(clean_text):
            logger.info("process_text_controller: user accepted suggestion")
            suggestion_text = pending_suggestions.pop(userID)
            result = await handle_task_tracker_service(suggestion_text, userID, db)
            return {
                "status": "success" if result.get("success") else "error",
                "intent": "TASK_TRACKER",
                "text": clean_text,
                "result": result,
                "message": "Task created from your suggestion!",
            }

        # ── STEP 2: Check memories and get suggestion ─────────────────────
        memories = get_user_memories(db, userID)
        logger.info(
            "process_text_controller: user has %d memories: %s",
            len(memories), memories
        )

        suggestion_result = await handle_suggestion_check(userID, db)
        logger.info("process_text_controller: suggestion_result=%s", suggestion_result)

        if suggestion_result:
            pending_suggestions[userID] = suggestion_result["suggestion"]

        # ── STEP 3: Normal intent flow ────────────────────────────────────
        logger.info("process_text_controller: extracting intent")
        intent = extract_intent(text)
        logger.info("process_text_controller: extracted intent=%s", intent)

        if intent == "UI_AUTOMATION":
            job_id = str(uuid.uuid4())
            future = executor.submit(run_droidrun_sync, text)
            automation_jobs[job_id] = {
                "status": "running",
                "command": text,
                "started_at": datetime.now().isoformat(),
                "future": future,
            }
            response = {
                "status": "accepted",
                "intent": intent,
                "text": text,
                "job_id": job_id,
            }

        elif intent == "FINANCE":
            result = await handle_finance_service(text, userID, db)
            logger.info("process_text_controller: FINANCE service result success=%s", result.get("success"))
            response = {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "text": text,
                "result": result,
            }

        elif intent == "TASK_TRACKER":
            result = await handle_task_tracker_service(text, userID, db)
            logger.info("process_text_controller: TASK_TRACKER service result success=%s", result.get("success"))
            response = {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "text": text,
                "result": result,
            }

        else:
            logger.warning("process_text_controller: unknown intent=%s, text=%r", intent, text)
            response = {"status": "unknown_intent", "intent": intent, "text": text}

        # ── STEP 4: Attach suggestion to response ─────────────────────────
        if suggestion_result:
            response["suggestion"] = suggestion_result["suggestion"]

        return response

    except Exception as e:
        logger.exception("process_text_controller: unexpected error")
        return {
            "status": "error",
            "message": str(e),
            "stage": "process_text_controller",
        }
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
)

logger = logging.getLogger(__name__)

automation_jobs: dict[str, dict] = {}
executor = ThreadPoolExecutor(max_workers=2)


def run_droidrun_sync(command: str) -> dict:
    """Runs droidrun CLI in background thread."""
    logger.info(f"run_droidrun_sync: received command={command}")

    groq_key = os.environ.get("GROQ_API_KEY")
    portal_token = os.environ.get("PORTAL_AUTH_TOKEN")

    env = os.environ.copy()
    if groq_key:
        env["GROQ_API_KEY"] = groq_key
    if portal_token:
        env["PORTAL_AUTH_TOKEN"] = portal_token

    env["PYTHONIOENCODING"] = "utf-8"

    logger.info(f"run_droidrun_sync: GROQ_API_KEY={'set' if groq_key else 'NOT SET'}")
    logger.info(
        f"run_droidrun_sync: PORTAL_AUTH_TOKEN={'set' if portal_token else 'NOT SET'}"
    )

    cmd = [
        str(VENV_PYTHON),
        "-m",
        "droidrun",
        "run",
        command,
        "--provider",
        "Groq",
        "--model",
        "moonshotai/kimi-k2-instruct-0905",
        "--tcp",
        "--no-stream",
    ]

    logger.info(f"run_droidrun_sync: executing cmd={cmd}")

    try:
        logger.info("run_droidrun_sync: starting subprocess.Popen...")

        # Use Popen to read output in real-time
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

        # Read and log output line by line as it arrives
        output_lines = []
        for line in process.stdout:
            output_lines.append(line)
            logger.info(f"droidrun: {line.rstrip()}")

        # Wait for process to complete
        process.wait()
        returncode = process.returncode

        logger.info(f"run_droidrun_sync: subprocess completed, returncode={returncode}")

        if returncode != 0:
            logger.error(f"run_droidrun_sync: FAILED")

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
    """
    Full AI workflow:
    - save uploaded audio
    - speech-to-text
    - intent classification
    - route to UI automation / finance / task tracker
    """
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

    logger.info(
        "process_command_controller: started, temp_audio=%s, userID=%s",
        temp_audio,
        userID,
    )

    try:
        async with aiofiles.open(temp_audio, "wb") as out:
            content = await file.read()
            logger.info(
                "process_command_controller: received file, size=%s bytes",
                len(content),
            )
            await out.write(content)

        logger.info("process_command_controller: starting transcription")
        text = transcribe(temp_audio)

        logger.info("process_command_controller: transcription done text=%r", text)

        logger.info("process_command_controller: extracting intent")
        intent = extract_intent(text)

        logger.info("process_command_controller: extracted intent=%s", intent)

        if intent == "UI_AUTOMATION":
            logger.info("process_command_controller: routing to UI_AUTOMATION via CLI")
            job_id = str(uuid.uuid4())

            future = executor.submit(run_droidrun_sync, text)

            automation_jobs[job_id] = {
                "status": "running",
                "command": text,
                "started_at": datetime.now().isoformat(),
                "future": future,
            }

            return {
                "status": "accepted",
                "intent": intent,
                "transcription": text,
                "job_id": job_id,
            }

        elif intent == "FINANCE":
            logger.info(
                "process_command_controller: routing to FINANCE service with text=%r, userID=%s",
                text,
                userID,
            )
            result = await handle_finance_service(text, userID, db)

            logger.info(
                "process_command_controller: FINANCE service result success=%s",
                result.get("success"),
            )
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "transcription": text,
                "result": result,
            }

        elif intent == "TASK_TRACKER":
            logger.info(
                "process_command_controller: routing to TASK_TRACKER service with text=%r, userID=%s",
                text,
                userID,
            )
            result = await handle_task_tracker_service(text, userID, db)

            logger.info(
                "process_command_controller: TASK_TRACKER service result success=%s",
                result.get("success"),
            )
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "transcription": text,
                "result": result,
            }

        else:
            logger.warning(
                "process_command_controller: unknown intent=%s, text=%r",
                intent,
                text,
            )
            return {"status": "unknown_intent", "intent": intent, "transcription": text}

    except Exception as e:
        logger.exception("process_command_controller: unexpected error")
        return {
            "status": "error",
            "message": str(e),
            "stage": "process_command_controller",
        }

    finally:
        if os.path.exists(temp_audio):
            try:
                os.remove(temp_audio)
                logger.info(
                    "process_command_controller: cleaned up temp_audio=%s", temp_audio
                )
            except Exception:
                logger.exception(
                    "process_command_controller: failed to remove temp_audio=%s",
                    temp_audio,
                )


async def process_text_controller(text: str, userID: str, db: Session):
    """
    Text-based AI workflow (no transcription needed):
    - intent classification
    - route to UI automation / finance / task tracker
    """
    logger.info("process_text_controller: started, text=%r, userID=%s", text, userID)

    if not userID or not userID.strip():
        return {"status": "error", "message": "userID is required"}

    try:
        logger.info("process_text_controller: extracting intent")
        intent = extract_intent(text)

        logger.info("process_text_controller: extracted intent=%s", intent)

        if intent == "UI_AUTOMATION":
            logger.info("process_text_controller: routing to UI_AUTOMATION via CLI")
            job_id = str(uuid.uuid4())

            future = executor.submit(run_droidrun_sync, text)

            automation_jobs[job_id] = {
                "status": "running",
                "command": text,
                "started_at": datetime.now().isoformat(),
                "future": future,
            }

            return {
                "status": "accepted",
                "intent": intent,
                "text": text,
                "job_id": job_id,
            }

        elif intent == "FINANCE":
            logger.info(
                "process_text_controller: routing to FINANCE service with text=%r, userID=%s",
                text,
                userID,
            )
            result = await handle_finance_service(text, userID, db)

            logger.info(
                "process_text_controller: FINANCE service result success=%s",
                result.get("success"),
            )
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "text": text,
                "result": result,
            }

        elif intent == "TASK_TRACKER":
            logger.info(
                "process_text_controller: routing to TASK_TRACKER service with text=%r, userID=%s",
                text,
                userID,
            )
            result = await handle_task_tracker_service(text, userID, db)

            logger.info(
                "process_text_controller: TASK_TRACKER service result success=%s",
                result.get("success"),
            )
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "text": text,
                "result": result,
            }

        else:
            logger.warning(
                "process_text_controller: unknown intent=%s, text=%r",
                intent,
                text,
            )
            return {"status": "unknown_intent", "intent": intent, "text": text}

    except Exception as e:
        logger.exception("process_text_controller: unexpected error")
        return {
            "status": "error",
            "message": str(e),
            "stage": "process_text_controller",
        }

import os
import logging
import json
from pathlib import Path

import aiofiles
from fastapi import UploadFile
from sqlalchemy.orm import Session

from app.services.ai import (
    transcribe,
    extract_intent,
    handle_finance_service,
    handle_task_tracker_service,
    UI_AUTOMATION_PROMPT,
    automation_llm,
    config,
    DroidAgent,
    AdbToolsManager,
)

logger = logging.getLogger(__name__)

# #region agent log
DEBUG_LOG_PATH = Path(r"e:\Graduation project\Qareeb-AI-Personal-Assitant\.cursor\debug.log")
# #endregion


def _write_debug_log(location: str, message: str, data: dict, hypothesis_id: str = None):
    """Write debug log to NDJSON file"""
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


async def process_command_controller(file: UploadFile, userID: str, db: Session):
    """
    Full AI workflow:
    - save uploaded audio
    - speech-to-text
    - intent classification
    - route to UI automation / finance / task tracker

    Extensive logging is added so you can trace each step in the console.
    """
    # #region agent log
    _write_debug_log("ai.py:22", "process_command_controller: ENTRY", {"userID": userID, "filename": file.filename, "content_type": file.content_type}, "A")
    # #endregion
    
    # Use absolute path for temp audio file in a known location
    temp_dir = Path(r"e:\Graduation project\Qareeb-AI-Personal-Assitant\mobile_backend\temp_audio")
    temp_dir.mkdir(exist_ok=True)
    
    # Determine file extension from content type or default to .wav
    content_type = file.content_type or ""
    if "mp3" in content_type.lower():
        ext = ".mp3"
    elif "wav" in content_type.lower():
        ext = ".wav"
    elif "m4a" in content_type.lower():
        ext = ".m4a"
    else:
        ext = ".wav"  # default
    
    temp_audio = str(temp_dir / f"command_{os.urandom(4).hex()}{ext}")
    
    # #region agent log
    _write_debug_log("ai.py:32", "process_command_controller: temp_audio path created", {"temp_audio": temp_audio, "temp_dir": str(temp_dir)}, "C")
    # #endregion
    
    logger.info("process_command_controller: started, temp_audio=%s, userID=%s", temp_audio, userID)

    try:
        # #region agent log
        _write_debug_log("ai.py:36", "process_command_controller: BEFORE file read", {"filename": file.filename, "content_type": file.content_type}, "B")
        # #endregion
        
        async with aiofiles.open(temp_audio, "wb") as out:
            content = await file.read()
            
            # #region agent log
            _write_debug_log("ai.py:40", "process_command_controller: AFTER file read", {"content_size": len(content), "temp_audio_exists": os.path.exists(temp_audio)}, "B")
            # #endregion
            
            logger.info(
                "process_command_controller: received file, size=%s bytes",
                len(content),
            )
            await out.write(content)
            
            # #region agent log
            _write_debug_log("ai.py:48", "process_command_controller: AFTER file write", {"file_size": len(content), "temp_audio_exists": os.path.exists(temp_audio), "temp_audio_size": os.path.getsize(temp_audio) if os.path.exists(temp_audio) else 0}, "B")
            # #endregion

        # #region agent log
        _write_debug_log("ai.py:52", "process_command_controller: BEFORE transcription", {"temp_audio": temp_audio, "file_exists": os.path.exists(temp_audio)}, "D")
        # #endregion
        
        logger.info("process_command_controller: starting transcription")
        text = transcribe(temp_audio)
        
        # #region agent log
        _write_debug_log("ai.py:58", "process_command_controller: AFTER transcription", {"transcribed_text": text, "text_length": len(text) if text else 0}, "D")
        # #endregion
        
        logger.info("process_command_controller: transcription done text=%r", text)

        # #region agent log
        _write_debug_log("ai.py:64", "process_command_controller: BEFORE intent extraction", {"text": text}, "E")
        # #endregion
        
        logger.info("process_command_controller: extracting intent")
        intent = extract_intent(text)
        
        # #region agent log
        _write_debug_log("ai.py:70", "process_command_controller: AFTER intent extraction", {"intent": intent}, "E")
        # #endregion
        
        logger.info("process_command_controller: extracted intent=%s", intent)

        if intent == "UI_AUTOMATION":
            logger.info("process_command_controller: routing to UI_AUTOMATION agent")
            try:
                tools = AdbToolsManager.get_tools()
                await tools.connect()
                agent = DroidAgent(
                    goal=f"{UI_AUTOMATION_PROMPT}\n\nTASK:\n{text}",
                    config=config,
                    llms=automation_llm,
                    tools=tools,
                )
                result = await agent.run()
                return {
                    "status": "success",
                    "intent": intent,
                    "transcription": text,
                    "automation_success": result.success,
                    "message": getattr(result, "output", "Task completed"),
                }
            except Exception as e:
                logger.exception(f"DroidAgent execution failed: {e}")
                return {
                    "status": "error",
                    "intent": intent,
                    "transcription": text,
                    "error": str(e),
                    "message": "UI automation failed",
                }

        elif intent == "FINANCE":
            # #region agent log
            _write_debug_log("ai.py:77", "process_command_controller: routing to FINANCE", {"text": text, "userID": userID}, "E")
            # #endregion
            
            logger.info(
                "process_command_controller: routing to FINANCE service with text=%r, userID=%s",
                text,
                userID,
            )
            result = await handle_finance_service(text, userID, db)
            
            # #region agent log
            _write_debug_log("ai.py:87", "process_command_controller: FINANCE service returned", {"success": result.get("success"), "result_keys": list(result.keys()) if isinstance(result, dict) else None}, "E")
            # #endregion
            
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
            # #region agent log
            _write_debug_log("ai.py:95", "process_command_controller: routing to TASK_TRACKER", {"text": text, "userID": userID}, "E")
            # #endregion
            
            logger.info(
                "process_command_controller: routing to TASK_TRACKER service with text=%r, userID=%s",
                text,
                userID,
            )
            result = await handle_task_tracker_service(text, userID, db)
            
            # #region agent log
            _write_debug_log("ai.py:105", "process_command_controller: TASK_TRACKER service returned", {"success": result.get("success"), "result_keys": list(result.keys()) if isinstance(result, dict) else None}, "E")
            # #endregion
            
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
        # #region agent log
        _write_debug_log("ai.py:121", "process_command_controller: EXCEPTION caught", {"error_type": type(e).__name__, "error_message": str(e), "error_repr": repr(e)}, "A")
        # #endregion
        
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

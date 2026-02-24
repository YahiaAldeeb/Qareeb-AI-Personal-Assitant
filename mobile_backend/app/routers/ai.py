import json
import logging
from pathlib import Path
from fastapi import APIRouter, File, UploadFile, Depends, Form, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.controllers.ai import process_command_controller

router = APIRouter(prefix="/ai", tags=["ai"])
logger = logging.getLogger(__name__)

DEBUG_LOG_PATH = Path(r"e:\Graduation project\Qareeb-AI-Personal-Assitant\.cursor\debug.log")

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

@router.post("/transcribe")
async def process_command(
    file: UploadFile = File(...),
    userID: str = Form(...),
    db: Session = Depends(get_db)
):
    # #region agent log
    _write_debug_log("ai.py:9", "router: /transcribe ENTRY", {"userID": userID, "filename": file.filename, "content_type": file.content_type, "file_size": None}, "A")
    # #endregion
    
    logger.info(f"POST /api/ai/transcribe - filename={file.filename}, content_type={file.content_type}, userID={userID}")
    
    if not userID or not userID.strip():
        # #region agent log
        _write_debug_log("ai.py:14", "router: userID validation FAILED", {"userID": userID}, "A")
        # #endregion
        raise HTTPException(status_code=400, detail="userID is required")
    
    # #region agent log
    _write_debug_log("ai.py:16", "router: calling process_command_controller", {"userID": userID.strip()}, "A")
    # #endregion
    
    result = await process_command_controller(file, userID.strip(), db)
    
    # #region agent log
    _write_debug_log("ai.py:20", "router: process_command_controller returned", {"result_status": result.get("status") if isinstance(result, dict) else None, "result_keys": list(result.keys()) if isinstance(result, dict) else None}, "A")
    # #endregion
    
    return result

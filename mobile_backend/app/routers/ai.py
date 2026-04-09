import logging
from fastapi import APIRouter, File, UploadFile, Depends, Form, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.controllers.ai import process_command_controller, automation_jobs

router = APIRouter(prefix="/ai", tags=["ai"])
logger = logging.getLogger(__name__)


@router.post("/transcribe")
async def process_command(
    file: UploadFile = File(...), userID: str = Form(...), db: Session = Depends(get_db)
):
    logger.info(
        f"POST /api/ai/transcribe - filename={file.filename}, content_type={file.content_type}, userID={userID}"
    )

    if not userID or not userID.strip():
        raise HTTPException(status_code=400, detail="userID is required")

    result = await process_command_controller(file, userID.strip(), db)

    return result


@router.get("/status/{job_id}")
async def get_automation_status(job_id: str):
    """Poll for automation job status. Auto-deletes after result fetched."""
    job = automation_jobs.get(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    future = job.get("future")
    if future and future.done():
        result = future.result()
        job["status"] = result["status"]
        job["result"] = result

    response = {
        "job_id": job_id,
        "status": job["status"],
        "command": job.get("command"),
        "started_at": job.get("started_at"),
        "result": job.get("result"),
    }

    if job.get("result"):
        del automation_jobs[job_id]

    return response

from fastapi import APIRouter, File, UploadFile, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.controllers.ai import process_command_controller

router = APIRouter(prefix="/ai", tags=["ai"])

@router.post("/transcribe")
async def process_command(file: UploadFile = File(...), db: Session = Depends(get_db)):
    return await process_command_controller(file, db)

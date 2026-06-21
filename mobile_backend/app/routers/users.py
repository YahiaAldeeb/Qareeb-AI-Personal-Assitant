from fastapi import APIRouter, Body, Depends, File, UploadFile, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import text
from typing import List
import tempfile
import os
import shutil

from app.database import get_db
from app.models.user import LoginRequest, RegisterRequest
from app.services.voice_auth import extract_voice_embedding, average_embeddings, encrypt_embedding
from app.controllers.user import (
    list_users_controller,
    create_user_controller,
    login_controller,
    register_controller,
    get_user_controller,
    update_user_controller,
    delete_user_controller,
    get_user_tasks_controller,
    get_user_transactions_controller,
)

router = APIRouter(prefix="/users", tags=["users"])

@router.get("")
def list_users(db: Session = Depends(get_db)):
    return list_users_controller(db)

@router.post("")
def create_user_route(payload: dict = Body(...), db: Session = Depends(get_db)):
    return create_user_controller(payload, db)

@router.post("/login")
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    return login_controller(payload, db)

@router.post("/register")
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    return register_controller(payload, db)

@router.get("/{userID}")
def get_user(userID: int, db: Session = Depends(get_db)):
    return get_user_controller(userID, db)

@router.put("/{userID}")
def update_user_route(userID: int, payload: dict = Body(...), db: Session = Depends(get_db)):
    return update_user_controller(userID, payload, db)

@router.delete("/{userID}")
def delete_user_route(userID: int, db: Session = Depends(get_db)):
    return delete_user_controller(userID, db)

@router.get("/{userID}/tasks")
def get_user_tasks(userID: int, db: Session = Depends(get_db)):
    return get_user_tasks_controller(userID, db)

@router.get("/{userID}/transactions")
def get_user_transactions(userID: int, db: Session = Depends(get_db)):
    return get_user_transactions_controller(userID, db)

@router.post("/voice/{userID}")
async def register_voice(
    userID: str,
    wav_files: List[UploadFile] = File(...),
    db: Session = Depends(get_db)
):
    # Ensure user exists
    user = db.execute(
        text('SELECT * FROM "User" WHERE "userID" = :userID'),
        {"userID": userID}
    ).mappings().first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    temp_files = []
    embeddings = []
    try:
        temp_dir = tempfile.gettempdir()
        for idx, upload_file in enumerate(wav_files):
            temp_path = os.path.join(temp_dir, f"enroll_{userID}_{idx}_{os.urandom(4).hex()}.wav")
            with open(temp_path, "wb") as buffer:
                shutil.copyfileobj(upload_file.file, buffer)
            temp_files.append(temp_path)
            
            # Extract embedding
            emb = extract_voice_embedding(temp_path)
            embeddings.append(emb)
        
        # Average and unit-normalize
        avg_emb = average_embeddings(embeddings)
        # Encrypt embedding
        encrypted_bytes = encrypt_embedding(avg_emb)
        
        # Save in database
        db.execute(
            text('UPDATE "User" SET voice_embedding = :emb WHERE "userID" = :uid'),
            {"emb": encrypted_bytes, "uid": userID}
        )
        db.commit()
        
        return {"voice_embedding": "success"}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        # Clean up temp files
        for temp_path in temp_files:
            if os.path.exists(temp_path):
                try:
                    os.remove(temp_path)
                except Exception:
                    pass
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime, timedelta
import uuid
from app.database import get_db

router = APIRouter()


class TaskSync(BaseModel):
    taskID: str
    userID: str
    title: str
    description: Optional[str] = None
    updated_at: str
    is_deleted: bool = False
    dueDate: Optional[str] = None


class PushPayload(BaseModel):
    records: List[TaskSync]


class PullResponse(BaseModel):
    records: List[dict]
    server_time: str


def serialize_row(row):
    """Convert database row to JSON-serializable dict"""
    result = {}
    for k, v in row.items():
        if isinstance(v, datetime):
            result[k] = v.isoformat()
        elif isinstance(v, uuid.UUID):
            result[k] = str(v)
        else:
            result[k] = v
    return result


@router.get("/sync/pull", response_model=PullResponse)
def pull_changes(user_id: str, last_sync: str, db: Session = Depends(get_db)):
    """
    Pull all tasks for a user that have been updated since last_sync
    """
    print(f"[SYNC PULL] user_id: {user_id}")
    print(f"[SYNC PULL] last_sync: {last_sync}")
    
    # Parse the last_sync parameter
    try:
        cutoff_time = datetime.fromisoformat(last_sync.replace("Z", "+00:00"))
        # Remove timezone info to match DB format (if your DB stores naive datetimes)
        if cutoff_time.tzinfo:
            cutoff_time = cutoff_time.replace(tzinfo=None)
    except Exception as e:
        print(f"[SYNC PULL] Error parsing last_sync: {e}")
        # Fallback to very old date if parsing fails
        cutoff_time = datetime(2000, 1, 1)
    
    print(f"[SYNC PULL] cutoff_time: {cutoff_time}")
    
    # Query tasks for this user updated after cutoff_time
    rows = db.execute(
        text('''
            SELECT * FROM "Task" 
            WHERE "userID" = :user_id 
            AND updated_at >= :cutoff 
            ORDER BY updated_at DESC
        '''),
        {"user_id": user_id, "cutoff": cutoff_time}
    ).mappings().all()
    
    print(f"[SYNC PULL] Found {len(rows)} tasks")
    if rows:
        print(f"[SYNC PULL] Sample task: {dict(rows[0])}")
    
    # Serialize rows (convert UUID and datetime to strings)
    serialized = [serialize_row(r) for r in rows]
    
    if serialized:
        print(f"[SYNC PULL] Sample serialized: {serialized[0]}")
    
    return {
        "records": serialized,
        "server_time": datetime.utcnow().isoformat()
    }


@router.post("/sync/push")
def push_changes(payload: PushPayload, db: Session = Depends(get_db)):
    """
    Push task changes from client to server
    Uses last-write-wins conflict resolution
    """
    print(f"[SYNC PUSH] Received {len(payload.records)} records")
    
    for item in payload.records:
        # Check if task already exists
        existing = db.execute(
            text('SELECT "taskID", updated_at FROM "Task" WHERE "taskID" = :taskID'),
            {"taskID": item.taskID}
        ).mappings().first()

        if existing is None:
            # New task - insert it
            print(f"[SYNC PUSH] Inserting new task: {item.title}")
            
            # Parse dueDate if provided
            due_date = None
            if item.dueDate:
                try:
                    due_date = datetime.fromisoformat(item.dueDate.replace("Z", "+00:00"))
                    if due_date.tzinfo:
                        due_date = due_date.replace(tzinfo=None)
                except Exception as e:
                    print(f"[SYNC PUSH] Error parsing dueDate: {e}")
            
            db.execute(
                text('''
                    INSERT INTO "Task" 
                    ("taskID", "userID", title, description, is_deleted, "dueDate") 
                    VALUES (:taskID, :userID, :title, :description, :is_deleted, :dueDate)
                '''),
                {
                    "taskID": item.taskID,
                    "userID": item.userID,
                    "title": item.title,
                    "description": item.description,
                    "is_deleted": item.is_deleted,
                    "dueDate": due_date
                }
            )
        else:
            # Task exists - check if client version is newer
            server_time = existing["updated_at"]
            
            # Parse client timestamp
            try:
                client_time = datetime.fromisoformat(item.updated_at.replace("Z", "+00:00"))
                if client_time.tzinfo and server_time and server_time.tzinfo is None:
                    client_time = client_time.replace(tzinfo=None)
            except Exception as e:
                print(f"[SYNC PUSH] Error parsing client time: {e}")
                client_time = None
            
            # Update if server_time is None OR client is newer
            should_update = (server_time is None) or (client_time and client_time > server_time)
            
            if should_update:
                print(f"[SYNC PUSH] Updating task: {item.title}")
                
                # Parse dueDate if provided
                due_date = None
                if item.dueDate:
                    try:
                        due_date = datetime.fromisoformat(item.dueDate.replace("Z", "+00:00"))
                        if due_date.tzinfo:
                            due_date = due_date.replace(tzinfo=None)
                    except Exception as e:
                        print(f"[SYNC PUSH] Error parsing dueDate: {e}")
                
                db.execute(
                    text('''
                        UPDATE "Task" 
                        SET title = :title, 
                            description = :description, 
                            is_deleted = :is_deleted, 
                            "dueDate" = :dueDate 
                        WHERE "taskID" = :taskID
                    '''),
                    {
                        "taskID": item.taskID,
                        "title": item.title,
                        "description": item.description,
                        "is_deleted": item.is_deleted,
                        "dueDate": due_date
                    }
                )
            else:
                print(f"[SYNC PUSH] Skipped (server newer): {item.title}")

    db.commit()
    print(f"[SYNC PUSH] Commit successful!")
    return {"status": "ok"}
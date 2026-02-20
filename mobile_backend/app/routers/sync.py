from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime, timedelta
from app.database import get_db

router = APIRouter()


class TaskSync(BaseModel):
    taskID: str
    userID: str
    title: str
    description: Optional[str] = None
    updated_at: str
    is_deleted: bool = False


class PushPayload(BaseModel):
    records: List[TaskSync]


@router.get("/sync/pull")
def pull_changes(user_id: str, last_sync: str, db: Session = Depends(get_db)):
    five_days_ago = datetime.utcnow() - timedelta(days=5)
    rows = db.execute(
        text('SELECT * FROM "Task" WHERE "userID" = :user_id AND updated_at >= :cutoff'),
        {"user_id": user_id, "cutoff": five_days_ago}
    ).mappings().all()
    return {
        "records": [dict(r) for r in rows],
        "server_time": datetime.utcnow().isoformat()
    }


@router.post("/sync/push")
def push_changes(payload: PushPayload, db: Session = Depends(get_db)):
    for item in payload.records:
        existing = db.execute(
            text('SELECT "taskID", updated_at FROM "Task" WHERE "taskID" = :taskID'),
            {"taskID": item.taskID}
        ).mappings().first()

        if existing is None:
            db.execute(
                text('INSERT INTO "Task" ("taskID", "userID", title, description, is_deleted) VALUES (:taskID, :userID, :title, :description, :is_deleted)'),
                {
                    "taskID": item.taskID,
                    "userID": item.userID,
                    "title": item.title,
                    "description": item.description,
                    "is_deleted": item.is_deleted
                }
            )
        else:
            server_time = existing["updated_at"]
            if server_time is None:
                db.execute(
                    text('UPDATE "Task" SET title = :title, description = :description, is_deleted = :is_deleted WHERE "taskID" = :taskID'),
                    {
                        "taskID": item.taskID,
                        "title": item.title,
                        "description": item.description,
                        "is_deleted": item.is_deleted
                    }
                )
            else:
                client_time = datetime.fromisoformat(item.updated_at.replace("Z", "+00:00"))
                if server_time.tzinfo is None:
                    server_time = server_time.replace(tzinfo=client_time.tzinfo)
                if client_time > server_time:
                    db.execute(
                        text('UPDATE "Task" SET title = :title, description = :description, is_deleted = :is_deleted WHERE "taskID" = :taskID'),
                        {
                            "taskID": item.taskID,
                            "title": item.title,
                            "description": item.description,
                            "is_deleted": item.is_deleted
                        }
                    )

    db.commit()
    return {"status": "ok"}
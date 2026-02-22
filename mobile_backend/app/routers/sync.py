from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
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


class TransactionSync(BaseModel):
    transactionID: str
    userID: str
    title: str
    amount: float
    date: str
    state: Optional[str] = None
    is_deleted: bool = False
    updated_at: str


class PushPayload(BaseModel):
    records: List[TaskSync]


class TransactionPushPayload(BaseModel):
    records: List[TransactionSync]


class PullResponse(BaseModel):
    records: List[dict]
    server_time: str


def serialize_row(row):
    result = {}
    for k, v in row.items():
        if isinstance(v, datetime):
            result[k] = v.isoformat()
        elif isinstance(v, uuid.UUID):
            result[k] = str(v)
        else:
            result[k] = v
    return result


# ── Task Pull ──
@router.get("/sync/pull", response_model=PullResponse)
def pull_changes(user_id: str, last_sync: str, db: Session = Depends(get_db)):
    print(f"[SYNC PULL] user_id: {user_id}")
    print(f"[SYNC PULL] last_sync: {last_sync}")

    try:
        cutoff_time = datetime.fromisoformat(last_sync.replace("Z", "+00:00"))
        if cutoff_time.tzinfo:
            cutoff_time = cutoff_time.replace(tzinfo=None)
    except Exception as e:
        print(f"[SYNC PULL] Error parsing last_sync: {e}")
        cutoff_time = datetime(2000, 1, 1)

    print(f"[SYNC PULL] cutoff_time: {cutoff_time}")

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
    serialized = [serialize_row(r) for r in rows]

    return {
        "records": serialized,
        "server_time": datetime.utcnow().isoformat()
    }


# ── Task Push ──
@router.post("/sync/push")
def push_changes(payload: PushPayload, db: Session = Depends(get_db)):
    print(f"[SYNC PUSH] Received {len(payload.records)} records")

    for item in payload.records:
        existing = db.execute(
            text('SELECT "taskID", updated_at FROM "Task" WHERE "taskID" = :taskID'),
            {"taskID": item.taskID}
        ).mappings().first()

        due_date = None
        if item.dueDate:
            try:
                due_date = datetime.fromisoformat(item.dueDate.replace("Z", "+00:00"))
                if due_date.tzinfo:
                    due_date = due_date.replace(tzinfo=None)
            except Exception as e:
                print(f"[SYNC PUSH] Error parsing dueDate: {e}")

        if existing is None:
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
            server_time = existing["updated_at"]
            try:
                client_time = datetime.fromisoformat(item.updated_at.replace("Z", "+00:00"))
                if client_time.tzinfo and server_time and server_time.tzinfo is None:
                    client_time = client_time.replace(tzinfo=None)
            except:
                client_time = None

            if (server_time is None) or (client_time and client_time > server_time):
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

    db.commit()
    return {"status": "ok"}


# ── Transaction Pull ──
@router.get("/sync/pull/transactions", response_model=PullResponse)
def pull_transactions(user_id: str, last_sync: str, db: Session = Depends(get_db)):
    print(f"[TRANSACTION PULL] user_id: {user_id}")
    print(f"[TRANSACTION PULL] last_sync: {last_sync}")

    try:
        cutoff_time = datetime.fromisoformat(last_sync.replace("Z", "+00:00"))
        if cutoff_time.tzinfo:
            cutoff_time = cutoff_time.replace(tzinfo=None)
    except Exception as e:
        print(f"[TRANSACTION PULL] Error parsing last_sync: {e}")
        cutoff_time = datetime(2000, 1, 1)

    rows = db.execute(
        text('''
            SELECT * FROM "Transaction"
            WHERE "userID" = :user_id
            AND updated_at >= :cutoff
            ORDER BY updated_at DESC
        '''),
        {"user_id": user_id, "cutoff": cutoff_time}
    ).mappings().all()

    print(f"[TRANSACTION PULL] Found {len(rows)} transactions")
    serialized = [serialize_row(r) for r in rows]

    return {
        "records": serialized,
        "server_time": datetime.utcnow().isoformat()
    }


# ── Transaction Push ──
@router.post("/sync/push/transactions")
def push_transactions(payload: TransactionPushPayload, db: Session = Depends(get_db)):
    print(f"[TRANSACTION PUSH] Received {len(payload.records)} records")

    for item in payload.records:
        existing = db.execute(
            text('SELECT "transactionID", updated_at FROM "Transaction" WHERE "transactionID" = :transactionID'),
            {"transactionID": item.transactionID}
        ).mappings().first()

        date_val = None
        if item.date:
            try:
                date_val = datetime.fromisoformat(item.date.replace("Z", "+00:00"))
                if date_val.tzinfo:
                    date_val = date_val.replace(tzinfo=None)
            except Exception as e:
                print(f"[TRANSACTION PUSH] Error parsing date: {e}")

        if existing is None:
            db.execute(
                text('''
                    INSERT INTO "Transaction"
                    ("transactionID", "userID", title, amount, date, state, is_deleted)
                    VALUES (:transactionID, :userID, :title, :amount, :date, :state, :is_deleted)
                '''),
                {
                    "transactionID": item.transactionID,
                    "userID": item.userID,
                    "title": item.title,
                    "amount": item.amount,
                    "date": date_val,
                    "state": item.state,
                    "is_deleted": item.is_deleted
                }
            )
        else:
            server_time = existing["updated_at"]
            try:
                client_time = datetime.fromisoformat(item.updated_at.replace("Z", "+00:00"))
                if client_time.tzinfo and server_time and server_time.tzinfo is None:
                    client_time = client_time.replace(tzinfo=None)
            except:
                client_time = None

            if (server_time is None) or (client_time and client_time > server_time):
                db.execute(
                    text('''
                        UPDATE "Transaction"
                        SET title = :title,
                            amount = :amount,
                            date = :date,
                            state = :state,
                            is_deleted = :is_deleted
                        WHERE "transactionID" = :transactionID
                    '''),
                    {
                        "transactionID": item.transactionID,
                        "title": item.title,
                        "amount": item.amount,
                        "date": date_val,
                        "state": item.state,
                        "is_deleted": item.is_deleted
                    }
                )

    db.commit()
    return {"status": "ok"}
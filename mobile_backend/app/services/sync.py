from datetime import datetime
import uuid
from sqlalchemy import text
from sqlalchemy.orm import Session
from app.models.sync import PushPayload, TransactionPushPayload

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

def pull_tasks_service(userID: str, last_sync: str, db: Session):
    try:
        cutoff_time = datetime.fromisoformat(last_sync.replace("Z", "+00:00"))
        if cutoff_time.tzinfo:
            cutoff_time = cutoff_time.replace(tzinfo=None)
    except Exception as e:
        cutoff_time = datetime(2000, 1, 1)

    rows = db.execute(
        text('''
            SELECT * FROM "Task"
            WHERE "userID" = :userID OR "userID" = :userID
            AND updated_at >= :cutoff
            ORDER BY updated_at DESC
        '''),
        {"userID": userID, "cutoff": cutoff_time}
    ).mappings().all()
    
    return [serialize_row(r) for r in rows]

def push_tasks_service(payload: PushPayload, db: Session):
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
                pass

        if existing is None:
            db.execute(
                text('''
                    INSERT INTO "Task"
                    ("taskID", "userID", "userID", title, description, is_deleted, "dueDate")
                    VALUES (:taskID, :userID, :userID, :title, :description, :is_deleted, :dueDate)
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

def pull_transactions_service(userID: str, last_sync: str, db: Session):
    try:
        cutoff_time = datetime.fromisoformat(last_sync.replace("Z", "+00:00"))
        if cutoff_time.tzinfo:
            cutoff_time = cutoff_time.replace(tzinfo=None)
    except Exception as e:
        cutoff_time = datetime(2000, 1, 1)

    rows = db.execute(
        text('''
            SELECT * FROM "Transaction"
            WHERE "userID" = :userID OR "userID" = :userID
            AND updated_at >= :cutoff
            ORDER BY updated_at DESC
        '''),
        {"userID": userID, "cutoff": cutoff_time}
    ).mappings().all()

    return [serialize_row(r) for r in rows]

def push_transactions_service(payload: TransactionPushPayload, db: Session):
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
                pass

        if existing is None:
            db.execute(
                text('''
                    INSERT INTO "Transaction"
                    ("transactionID", "userID", "userID", title, amount, date, state, is_deleted)
                    VALUES (:transactionID, :userID, :userID, :title, :amount, :date, :state, :is_deleted)
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

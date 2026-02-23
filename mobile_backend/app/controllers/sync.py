from datetime import datetime
from sqlalchemy.orm import Session
from app.models.sync import PushPayload, TransactionPushPayload
from app.services.sync import (
    pull_tasks_service,
    push_tasks_service,
    pull_transactions_service,
    push_transactions_service,
)

def pull_changes_controller(userID: str, last_sync: str, db: Session):
    records = pull_tasks_service(userID, last_sync, db)
    return {
        "records": records,
        "server_time": datetime.utcnow().isoformat()
    }

def push_changes_controller(payload: PushPayload, db: Session):
    push_tasks_service(payload, db)
    return {"status": "ok"}

def pull_transactions_controller(userID: str, last_sync: str, db: Session):
    records = pull_transactions_service(userID, last_sync, db)
    return {
        "records": records,
        "server_time": datetime.utcnow().isoformat()
    }

def push_transactions_controller(payload: TransactionPushPayload, db: Session):
    push_transactions_service(payload, db)
    return {"status": "ok"}

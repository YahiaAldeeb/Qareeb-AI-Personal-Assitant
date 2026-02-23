from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.sync import PullResponse, PushPayload, TransactionPushPayload
from app.controllers.sync import (
    pull_changes_controller,
    push_changes_controller,
    pull_transactions_controller,
    push_transactions_controller,
)

router = APIRouter(prefix="/sync", tags=["sync"])

@router.get("/pull", response_model=PullResponse)
def pull_changes(userID: str, last_sync: str, db: Session = Depends(get_db)):
    return pull_changes_controller(userID, last_sync, db)

@router.post("/push")
def push_changes(payload: PushPayload, db: Session = Depends(get_db)):
    return push_changes_controller(payload, db)

@router.get("/pull/transactions", response_model=PullResponse)
def pull_transactions(userID: str, last_sync: str, db: Session = Depends(get_db)):
    return pull_transactions_controller(userID, last_sync, db)

@router.post("/push/transactions")
def push_transactions(payload: TransactionPushPayload, db: Session = Depends(get_db)):
    return push_transactions_controller(payload, db)
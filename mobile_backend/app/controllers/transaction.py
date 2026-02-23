from fastapi import HTTPException
from sqlalchemy.orm import Session
from app.services.transaction import (
    get_transactions_service,
    create_transaction_service,
    update_transaction_service,
    delete_transaction_service,
)

def list_transactions_controller(db: Session):
    return {"transactions": get_transactions_service(db)}

def create_transaction_controller(payload: dict, db: Session):
    try:
        return {"transaction": create_transaction_service(db, payload)}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))

def update_transaction_controller(transaction_id: int, payload: dict, db: Session):
    try:
        tx = update_transaction_service(db, transaction_id, payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    if tx is None:
        raise HTTPException(status_code=404, detail="Transaction not found")
    return {"transaction": tx}

def delete_transaction_controller(transaction_id: int, db: Session):
    tx = delete_transaction_service(db, transaction_id)
    if tx is None:
        raise HTTPException(status_code=404, detail="Transaction not found")
    return {"deleted": tx}

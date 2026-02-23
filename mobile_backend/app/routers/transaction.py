from fastapi import APIRouter, Body, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.controllers.transaction import (
    list_transactions_controller,
    create_transaction_controller,
    update_transaction_controller,
    delete_transaction_controller,
)

router = APIRouter(prefix="/transactions", tags=["transactions"])

@router.get("")
def list_transactions(db: Session = Depends(get_db)):
    return list_transactions_controller(db)

@router.post("")
def create_transaction_route(payload: dict = Body(...), db: Session = Depends(get_db)):
    return create_transaction_controller(payload, db)

@router.put("/{transaction_id}")
def update_transaction_route(
    transaction_id: int, payload: dict = Body(...), db: Session = Depends(get_db)
):
    return update_transaction_controller(transaction_id, payload, db)

@router.delete("/{transaction_id}")
def delete_transaction_route(transaction_id: int, db: Session = Depends(get_db)):
    return delete_transaction_controller(transaction_id, db)
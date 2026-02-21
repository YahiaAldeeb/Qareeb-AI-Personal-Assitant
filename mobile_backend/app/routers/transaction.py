from fastapi import APIRouter, Body, Depends, HTTPException
from sqlalchemy.orm import Session

from ..crud import create_transaction, delete_transaction, update_transaction
from ..database import get_db

router = APIRouter(prefix="/transactions", tags=["transactions"])


@router.get("")
def list_transactions(db: Session = Depends(get_db)):
    rows = db.execute('SELECT * FROM "Transaction" ORDER BY id').mappings().all()
    return {"transactions": [dict(row) for row in rows]}


@router.post("")
def create_transaction_route(payload: dict = Body(...), db: Session = Depends(get_db)):
    try:
        return {"transaction": create_transaction(db, payload)}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))


@router.put("/{transaction_id}")
def update_transaction_route(
    transaction_id: int, payload: dict = Body(...), db: Session = Depends(get_db)
):
    try:
        tx = update_transaction(db, transaction_id, payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    if tx is None:
        raise HTTPException(status_code=404, detail="Transaction not found")
    return {"transaction": tx}


@router.delete("/{transaction_id}")
def delete_transaction_route(transaction_id: int, db: Session = Depends(get_db)):
    tx = delete_transaction(db, transaction_id)
    if tx is None:
        raise HTTPException(status_code=404, detail="Transaction not found")
    return {"deleted": tx}

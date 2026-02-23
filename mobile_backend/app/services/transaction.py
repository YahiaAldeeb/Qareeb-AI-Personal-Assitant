from typing import Any
from sqlalchemy import text
from sqlalchemy.orm import Session
from .base import insert_row, update_row, delete_row

TRANSACTION_COLUMNS = "*"

def get_transactions_by_userID_service(db: Session, userID: int | str) -> list[dict[str, Any]]:
    rows = (
        db.execute(
            text(
                f'SELECT {TRANSACTION_COLUMNS} FROM "Transaction" WHERE "userID" = :userID ORDER BY id'
            ),
            {"userID": userID},
        )
        .mappings()
        .all()
    )
    return [dict(row) for row in rows]

def get_transactions_service(db: Session) -> list[dict[str, Any]]:
    rows = db.execute(text(f'SELECT {TRANSACTION_COLUMNS} FROM "Transaction" ORDER BY id')).mappings().all()
    return [dict(row) for row in rows]

def create_transaction_service(db: Session, payload: dict[str, Any]) -> dict[str, Any]:
    return insert_row(db, "Transaction", payload)

def update_transaction_service(db: Session, transaction_id: int, payload: dict[str, Any]) -> dict[str, Any] | None:
    return update_row(db, "Transaction", transaction_id, payload)

def delete_transaction_service(db: Session, transaction_id: int) -> dict[str, Any] | None:
    return delete_row(db, "Transaction", transaction_id)

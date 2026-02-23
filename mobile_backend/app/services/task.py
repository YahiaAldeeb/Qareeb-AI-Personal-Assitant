from typing import Any
from sqlalchemy import text
from sqlalchemy.orm import Session
from .base import insert_row, update_row, delete_row

TASK_COLUMNS = "*"

def get_tasks_by_userID_service(db: Session, userID: int) -> list[dict[str, Any]]:
    rows = (
        db.execute(
            text(f'SELECT {TASK_COLUMNS} FROM "Task" WHERE "userID" = :userID ORDER BY id'),
            {"userID": userID},
        )
        .mappings()
        .all()
    )
    return [dict(row) for row in rows]

def get_tasks_service(db: Session) -> list[dict[str, Any]]:
    rows = db.execute(text(f'SELECT {TASK_COLUMNS} FROM "Task" ORDER BY id')).mappings().all()
    return [dict(row) for row in rows]

def get_task_by_id_service(db: Session, task_id: int) -> dict[str, Any] | None:
    row = (
        db.execute(
            text(f'SELECT {TASK_COLUMNS} FROM "Task" WHERE id = :task_id'),
            {"task_id": task_id},
        )
        .mappings()
        .first()
    )
    return dict(row) if row else None

def create_task_service(db: Session, payload: dict[str, Any]) -> dict[str, Any]:
    return insert_row(db, "Task", payload)

def update_task_service(db: Session, task_id: int, payload: dict[str, Any]) -> dict[str, Any] | None:
    return update_row(db, "Task", task_id, payload)

def delete_task_service(db: Session, task_id: int) -> dict[str, Any] | None:
    return delete_row(db, "Task", task_id)

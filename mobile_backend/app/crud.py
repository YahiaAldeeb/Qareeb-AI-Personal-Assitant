from typing import Any

from sqlalchemy import text
from sqlalchemy.orm import Session


USER_COLUMNS = "id, created_at, name, email, last_login"
TASK_COLUMNS = "*"
TRANSACTION_COLUMNS = "*"


def _get_table_columns(db: Session, table_name: str) -> set[str]:
    rows = (
        db.execute(
            text(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = :table_name
                """
            ),
            {"table_name": table_name},
        )
        .mappings()
        .all()
    )
    return {row["column_name"] for row in rows}


def _filter_payload(payload: dict[str, Any], allowed_columns: set[str]) -> dict[str, Any]:
    return {k: v for k, v in payload.items() if k in allowed_columns}


def _insert_row(db: Session, table_name: str, payload: dict[str, Any]) -> dict[str, Any]:
    columns = _get_table_columns(db, table_name)
    columns.discard("id")
    data = _filter_payload(payload, columns)
    if not data:
        raise ValueError("No valid fields to insert")

    col_list = ", ".join(f'"{c}"' for c in data.keys())
    val_list = ", ".join(f":{c}" for c in data.keys())
    row = (
        db.execute(
            text(f'INSERT INTO "{table_name}" ({col_list}) VALUES ({val_list}) RETURNING *'),
            data,
        )
        .mappings()
        .first()
    )
    db.commit()
    return dict(row) if row else {}


def _update_row(db: Session, table_name: str, row_id: int, payload: dict[str, Any]) -> dict[str, Any] | None:
    columns = _get_table_columns(db, table_name)
    columns.discard("id")
    data = _filter_payload(payload, columns)
    if not data:
        raise ValueError("No valid fields to update")

    set_clause = ", ".join(f'"{c}" = :{c}' for c in data.keys())
    data["id"] = row_id
    row = (
        db.execute(
            text(f'UPDATE "{table_name}" SET {set_clause} WHERE id = :id RETURNING *'),
            data,
        )
        .mappings()
        .first()
    )
    db.commit()
    return dict(row) if row else None


def _delete_row(db: Session, table_name: str, row_id: int) -> dict[str, Any] | None:
    row = (
        db.execute(
            text(f'DELETE FROM "{table_name}" WHERE id = :id RETURNING *'),
            {"id": row_id},
        )
        .mappings()
        .first()
    )
    db.commit()
    return dict(row) if row else None


def get_users(db: Session) -> list[dict[str, Any]]:
    rows = db.execute(text(f'SELECT {USER_COLUMNS} FROM "User" ORDER BY id')).mappings().all()
    return [dict(row) for row in rows]


def get_user_by_id(db: Session, user_id: int) -> dict[str, Any] | None:
    row = (
        db.execute(
            text(f'SELECT {USER_COLUMNS} FROM "User" WHERE id = :user_id'),
            {"user_id": user_id},
        )
        .mappings()
        .first()
    )
    return dict(row) if row else None


def get_tasks_by_user_id(db: Session, user_id: int) -> list[dict[str, Any]]:
    rows = (
        db.execute(
            text(f'SELECT {TASK_COLUMNS} FROM "Task" WHERE "user_ID" = :user_id ORDER BY id'),
            {"user_id": user_id},
        )
        .mappings()
        .all()
    )
    return [dict(row) for row in rows]


def get_tasks(db: Session) -> list[dict[str, Any]]:
    rows = db.execute(text(f'SELECT {TASK_COLUMNS} FROM "Task" ORDER BY id')).mappings().all()
    return [dict(row) for row in rows]


def get_task_by_id(db: Session, task_id: int) -> dict[str, Any] | None:
    row = (
        db.execute(
            text(f'SELECT {TASK_COLUMNS} FROM "Task" WHERE id = :task_id'),
            {"task_id": task_id},
        )
        .mappings()
        .first()
    )
    return dict(row) if row else None


def get_transactions_by_user_id(db: Session, user_id: int) -> list[dict[str, Any]]:
    rows = (
        db.execute(
            text(
                f'SELECT {TRANSACTION_COLUMNS} FROM "Transaction" WHERE "user_ID" = :user_id ORDER BY id'
            ),
            {"user_id": user_id},
        )
        .mappings()
        .all()
    )
    return [dict(row) for row in rows]


def create_user(db: Session, payload: dict[str, Any]) -> dict[str, Any]:
    return _insert_row(db, "User", payload)


def update_user(db: Session, user_id: int, payload: dict[str, Any]) -> dict[str, Any] | None:
    return _update_row(db, "User", user_id, payload)


def delete_user(db: Session, user_id: int) -> dict[str, Any] | None:
    return _delete_row(db, "User", user_id)


def create_task(db: Session, payload: dict[str, Any]) -> dict[str, Any]:
    return _insert_row(db, "Task", payload)


def update_task(db: Session, task_id: int, payload: dict[str, Any]) -> dict[str, Any] | None:
    return _update_row(db, "Task", task_id, payload)


def delete_task(db: Session, task_id: int) -> dict[str, Any] | None:
    return _delete_row(db, "Task", task_id)


def create_transaction(db: Session, payload: dict[str, Any]) -> dict[str, Any]:
    return _insert_row(db, "Transaction", payload)


def update_transaction(db: Session, transaction_id: int, payload: dict[str, Any]) -> dict[str, Any] | None:
    return _update_row(db, "Transaction", transaction_id, payload)


def delete_transaction(db: Session, transaction_id: int) -> dict[str, Any] | None:
    return _delete_row(db, "Transaction", transaction_id)

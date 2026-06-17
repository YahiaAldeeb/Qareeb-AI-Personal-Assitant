from typing import Any
from sqlalchemy import text
from sqlalchemy.orm import Session


def get_table_columns(db: Session, table_name: str) -> set[str]:
    # ✅ uses :table_name (colon style), NOT %(table_name)s (percent style)
    rows = (
        db.execute(
            text(
                "SELECT column_name "
                "FROM information_schema.columns "
                "WHERE table_schema = 'public' AND table_name = :table_name"
            ),
            {"table_name": table_name},
        )
        .mappings()
        .all()
    )
    return {row["column_name"] for row in rows}


def filter_payload(payload: dict[str, Any], allowed_columns: set[str]) -> dict[str, Any]:
    return {k: v for k, v in payload.items() if k in allowed_columns}


def insert_row(db: Session, table_name: str, payload: dict[str, Any]) -> dict[str, Any]:
    try:
        columns = get_table_columns(db, table_name)
        columns.discard("id")
        data = filter_payload(payload, columns)
        if not data:
            raise ValueError(f"No valid fields to insert into {table_name}. payload={payload}")

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
    except Exception:
        db.rollback()
        raise


def update_row(db: Session, table_name: str, task_id: str, payload: dict[str, Any]) -> dict[str, Any] | None:
    try:
        columns = get_table_columns(db, table_name)
        columns.discard("taskID")
        data = filter_payload(payload, columns)
        if not data:
            raise ValueError(f"No valid fields to update in {table_name}. payload={payload}")

        set_clause = ", ".join(f'"{c}" = :{c}' for c in data.keys())
        data["taskID"] = task_id
        row = (
            db.execute(
                text(f'UPDATE "{table_name}" SET {set_clause} WHERE "taskID" = :taskID RETURNING *'),
                data,
            )
            .mappings()
            .first()
        )
        db.commit()
        return dict(row) if row else None
    except Exception:
        db.rollback()
        raise


def delete_row(db: Session, table_name: str, row_id: int) -> dict[str, Any] | None:
    try:
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
    except Exception:
        db.rollback()
        raise
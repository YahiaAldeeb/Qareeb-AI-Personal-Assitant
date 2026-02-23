from typing import Any
from sqlalchemy import text
from sqlalchemy.orm import Session

def get_table_columns(db: Session, table_name: str) -> set[str]:
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

def filter_payload(payload: dict[str, Any], allowed_columns: set[str]) -> dict[str, Any]:
    return {k: v for k, v in payload.items() if k in allowed_columns}

def insert_row(db: Session, table_name: str, payload: dict[str, Any]) -> dict[str, Any]:
    columns = get_table_columns(db, table_name)
    columns.discard("id")
    data = filter_payload(payload, columns)
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

def update_row(db: Session, table_name: str, row_id: int, payload: dict[str, Any]) -> dict[str, Any] | None:
    columns = get_table_columns(db, table_name)
    columns.discard("id")
    data = filter_payload(payload, columns)
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

def delete_row(db: Session, table_name: str, row_id: int) -> dict[str, Any] | None:
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

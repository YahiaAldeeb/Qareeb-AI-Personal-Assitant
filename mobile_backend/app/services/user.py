import uuid
from typing import Any
from sqlalchemy import text
from sqlalchemy.orm import Session
from .base import insert_row, update_row, delete_row

USER_COLUMNS = "id, created_at, name, email, last_login"

def get_users_service(db: Session) -> list[dict[str, Any]]:
    rows = db.execute(text(f'SELECT {USER_COLUMNS} FROM "User" ORDER BY id')).mappings().all()
    return [dict(row) for row in rows]

def get_user_by_id_service(db: Session, userID: int) -> dict[str, Any] | None:
    row = (
        db.execute(
            text(f'SELECT {USER_COLUMNS} FROM "User" WHERE id = :userID'),
            {"userID": userID},
        )
        .mappings()
        .first()
    )
    return dict(row) if row else None

def create_user_service(db: Session, payload: dict[str, Any]) -> dict[str, Any]:
    return insert_row(db, "User", payload)

def update_user_service(db: Session, userID: int, payload: dict[str, Any]) -> dict[str, Any] | None:
    return update_row(db, "User", userID, payload)

def delete_user_service(db: Session, userID: int) -> dict[str, Any] | None:
    return delete_row(db, "User", userID)

def login_service(db: Session, email: str, password: str):
    user = db.execute(
        text('SELECT * FROM "User" WHERE email = :email AND password = :password'),
        {"email": email, "password": password}
    ).mappings().first()
    return user

def register_service(db: Session, name: str, email: str, password: str):
    existing = db.execute(
        text('SELECT "userID" FROM "User" WHERE email = :email'),
        {"email": email}
    ).mappings().first()

    if existing is not None:
        raise ValueError("Email already exists")

    new_userID = str(uuid.uuid4())
    try:
        db.execute(
            text('''
                INSERT INTO "User" ("userID", name, email, password)
                VALUES (:userID, :name, :email, :password)
            '''),
            {
                "userID": new_userID,
                "name": name,
                "email": email,
                "password": password
            }
        )
        db.commit()
    except Exception as e:
        db.rollback()
        raise e

    return {
        "userID": new_userID,
        "name": name,
        "email": email
    }

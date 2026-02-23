from fastapi import HTTPException
from sqlalchemy.orm import Session
from app.models.user import LoginRequest, RegisterRequest
from app.services.user import (
    get_users_service,
    create_user_service,
    get_user_by_id_service,
    update_user_service,
    delete_user_service,
    login_service,
    register_service,
)
from app.services.task import get_tasks_by_userID_service
from app.services.transaction import get_transactions_by_userID_service

def list_users_controller(db: Session):
    return {"users": get_users_service(db)}

def create_user_controller(payload: dict, db: Session):
    try:
        return {"user": create_user_service(db, payload)}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))

def login_controller(payload: LoginRequest, db: Session):
    user = login_service(db, payload.email, payload.password)
    if user is None:
        raise HTTPException(status_code=401, detail="Invalid email or password")
    return {
        "userID": str(user["userID"]),
        "name": user["name"],
        "email": user["email"]
    }

def register_controller(payload: RegisterRequest, db: Session):
    try:
        return register_service(db, payload.name, payload.email, payload.password)
    except ValueError as e:
        raise HTTPException(status_code=409, detail=str(e))
    except Exception as e:
        print(f"[REGISTER] Error: {e}")
        raise HTTPException(status_code=500, detail="Registration failed")

def get_user_controller(userID: int, db: Session):
    user = get_user_by_id_service(db, userID)
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return {"user": user}

def update_user_controller(userID: int, payload: dict, db: Session):
    try:
        user = update_user_service(db, userID, payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return {"user": user}

def delete_user_controller(userID: int, db: Session):
    user = delete_user_service(db, userID)
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return {"deleted": user}

def get_user_tasks_controller(userID: int, db: Session):
    return {"tasks": get_tasks_by_userID_service(db, userID)}

def get_user_transactions_controller(userID: int, db: Session):
    return {"transactions": get_transactions_by_userID_service(db, userID)}

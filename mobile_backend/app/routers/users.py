from fastapi import APIRouter, Body, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import LoginRequest, RegisterRequest
from app.controllers.user import (
    list_users_controller,
    create_user_controller,
    login_controller,
    register_controller,
    get_user_controller,
    update_user_controller,
    delete_user_controller,
    get_user_tasks_controller,
    get_user_transactions_controller,
)

router = APIRouter(prefix="/users", tags=["users"])

@router.get("")
def list_users(db: Session = Depends(get_db)):
    return list_users_controller(db)

@router.post("")
def create_user_route(payload: dict = Body(...), db: Session = Depends(get_db)):
    return create_user_controller(payload, db)

@router.post("/login")
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    return login_controller(payload, db)

@router.post("/register")
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    return register_controller(payload, db)

@router.get("/{userID}")
def get_user(userID: int, db: Session = Depends(get_db)):
    return get_user_controller(userID, db)

@router.put("/{userID}")
def update_user_route(userID: int, payload: dict = Body(...), db: Session = Depends(get_db)):
    return update_user_controller(userID, payload, db)

@router.delete("/{userID}")
def delete_user_route(userID: int, db: Session = Depends(get_db)):
    return delete_user_controller(userID, db)

@router.get("/{userID}/tasks")
def get_user_tasks(userID: int, db: Session = Depends(get_db)):
    return get_user_tasks_controller(userID, db)

@router.get("/{userID}/transactions")
def get_user_transactions(userID: int, db: Session = Depends(get_db)):
    return get_user_transactions_controller(userID, db)
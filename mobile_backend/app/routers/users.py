from fastapi import APIRouter, Body, Depends, HTTPException
from sqlalchemy.orm import Session

from ..crud import (
    create_user,
    delete_user,
    get_tasks_by_user_id,
    get_transactions_by_user_id,
    get_user_by_id,
    get_users,
    update_user,
)
from ..database import get_db

router = APIRouter(prefix="/users", tags=["users"])


@router.get("")
def list_users(db: Session = Depends(get_db)):
    return {"users": get_users(db)}


@router.post("")
def create_user_route(payload: dict = Body(...), db: Session = Depends(get_db)):
    try:
        return {"user": create_user(db, payload)}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))


@router.get("/{user_id}")
def get_user(user_id: int, db: Session = Depends(get_db)):
    user = get_user_by_id(db, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return {"user": user}


@router.put("/{user_id}")
def update_user_route(user_id: int, payload: dict = Body(...), db: Session = Depends(get_db)):
    try:
        user = update_user(db, user_id, payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return {"user": user}


@router.delete("/{user_id}")
def delete_user_route(user_id: int, db: Session = Depends(get_db)):
    user = delete_user(db, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return {"deleted": user}


@router.get("/{user_id}/tasks")
def get_user_tasks(user_id: int, db: Session = Depends(get_db)):
    return {"tasks": get_tasks_by_user_id(db, user_id)}


@router.get("/{user_id}/transactions")
def get_user_transactions(user_id: int, db: Session = Depends(get_db)):
    return {"transactions": get_transactions_by_user_id(db, user_id)}

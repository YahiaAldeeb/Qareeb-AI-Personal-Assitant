from fastapi import APIRouter, Body, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.controllers.task import (
    list_tasks_controller,
    create_task_controller,
    get_task_controller,
    update_task_controller,
    delete_task_controller,
)

router = APIRouter(prefix="/tasks", tags=["tasks"])

@router.get("")
def list_tasks(db: Session = Depends(get_db)):
    return list_tasks_controller(db)

@router.post("")
def create_task_route(payload: dict = Body(...), db: Session = Depends(get_db)):
    return create_task_controller(payload, db)

@router.get("/{task_id}")
def get_task(task_id: int, db: Session = Depends(get_db)):
    return get_task_controller(task_id, db)

@router.put("/{task_id}")
def update_task_route(task_id: int, payload: dict = Body(...), db: Session = Depends(get_db)):
    return update_task_controller(task_id, payload, db)

@router.delete("/{task_id}")
def delete_task_route(task_id: int, db: Session = Depends(get_db)):
    return delete_task_controller(task_id, db)

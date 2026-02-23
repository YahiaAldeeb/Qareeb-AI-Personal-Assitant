from fastapi import HTTPException
from sqlalchemy.orm import Session
from app.services.task import (
    get_tasks_service,
    create_task_service,
    get_task_by_id_service,
    update_task_service,
    delete_task_service,
)

def list_tasks_controller(db: Session):
    return {"tasks": get_tasks_service(db)}

def create_task_controller(payload: dict, db: Session):
    try:
        return {"task": create_task_service(db, payload)}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))

def get_task_controller(task_id: int, db: Session):
    task = get_task_by_id_service(db, task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"task": task}

def update_task_controller(task_id: int, payload: dict, db: Session):
    try:
        task = update_task_service(db, task_id, payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    if task is None:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"task": task}

def delete_task_controller(task_id: int, db: Session):
    task = delete_task_service(db, task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"deleted": task}

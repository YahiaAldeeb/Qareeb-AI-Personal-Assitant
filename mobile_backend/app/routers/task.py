from fastapi import APIRouter, Body, Depends, HTTPException
from sqlalchemy.orm import Session

from ..crud import create_task, delete_task, get_task_by_id, get_tasks, update_task
from ..database import get_db

router = APIRouter(prefix="/tasks", tags=["tasks"])


@router.get("")
def list_tasks(db: Session = Depends(get_db)):
    return {"tasks": get_tasks(db)}


@router.post("")
def create_task_route(payload: dict = Body(...), db: Session = Depends(get_db)):
    try:
        return {"task": create_task(db, payload)}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))


@router.get("/{task_id}")
def get_task(task_id: int, db: Session = Depends(get_db)):
    task = get_task_by_id(db, task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"task": task}


@router.put("/{task_id}")
def update_task_route(task_id: int, payload: dict = Body(...), db: Session = Depends(get_db)):
    try:
        task = update_task(db, task_id, payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    if task is None:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"task": task}


@router.delete("/{task_id}")
def delete_task_route(task_id: int, db: Session = Depends(get_db)):
    task = delete_task(db, task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"deleted": task}

from fastapi import FastAPI

from app.routers import task, transaction, users

app = FastAPI()

app.include_router(users.router)
app.include_router(task.router)
app.include_router(transaction.router)

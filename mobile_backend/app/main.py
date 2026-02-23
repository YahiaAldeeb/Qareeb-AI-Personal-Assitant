from fastapi import FastAPI
from app.routers import task, transaction, users, sync, ai

app = FastAPI()

app.include_router(users.router, prefix="/api")
app.include_router(task.router, prefix="/api")
app.include_router(transaction.router, prefix="/api")
app.include_router(sync.router, prefix="/api")
app.include_router(ai.router, prefix="/api")
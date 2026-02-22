from fastapi import FastAPI
from app.routers import task, transaction, users, sync

app = FastAPI()

app.include_router(users.router,prefix="/api")
app.include_router(task.router)
app.include_router(transaction.router)
app.include_router(sync.router, prefix="/api", tags=["sync"])
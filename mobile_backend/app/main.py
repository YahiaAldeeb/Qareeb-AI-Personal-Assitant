from fastapi import FastAPI
from app.routers import ai, sync, task, notifications,users,memory_sync  # add notifications
from app.scheduler import start_scheduler, scheduler
from app.database import get_db
#from app.routers.memory_sync import router as memory_sync_router  # ✅ add this
app = FastAPI()

import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

app.include_router(users.router, prefix="/api")
app.include_router(ai.router, prefix="/api")
app.include_router(sync.router, prefix="/api")
app.include_router(task.router, prefix="/api")
app.include_router(notifications.router, prefix="/api")  # add this
app.include_router(memory_sync.router, prefix="/api")  # ✅ add this


@app.on_event("startup")
async def startup_event():
    start_scheduler(get_db)  # start background scheduler


@app.on_event("shutdown")
async def shutdown_event():
    scheduler.shutdown()
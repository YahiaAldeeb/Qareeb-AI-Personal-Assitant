import logging
import sys
from fastapi import FastAPI
from app.routers import task, transaction, users, sync, ai

# Configure logging to show INFO level and above
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

app = FastAPI()

app.include_router(users.router, prefix="/api")
app.include_router(task.router, prefix="/api")
app.include_router(transaction.router, prefix="/api")
app.include_router(sync.router, prefix="/api")
app.include_router(ai.router, prefix="/api")
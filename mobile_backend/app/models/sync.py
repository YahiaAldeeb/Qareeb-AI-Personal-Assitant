from pydantic import BaseModel
from typing import List, Optional

class TaskSync(BaseModel):
    taskID: str
    userID: str
    title: str
    description: Optional[str] = None
    updated_at: str
    is_deleted: bool = False
    dueDate: Optional[str] = None

class TransactionSync(BaseModel):
    transactionID: str
    userID: str
    title: str
    amount: float
    date: Optional[str] = None
    state: Optional[str] = None
    is_deleted: bool = False
    updated_at: str

class PushPayload(BaseModel):
    records: List[TaskSync]

class TransactionPushPayload(BaseModel):
    records: List[TransactionSync]

class PullResponse(BaseModel):
    records: List[dict]
    server_time: str

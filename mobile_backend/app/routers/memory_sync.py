from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text as sql_text
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime, timezone
from app.database import get_db

router = APIRouter(prefix="/sync", tags=["memory-sync"])


class PromptSyncItem(BaseModel):
    prompt_id: Optional[int] = None
    userId: str                        # ✅ lowercase d — matches Supabase
    userMessage: str
    qareebResponse: str
    promptType: str
    module: str
    intentDetected: Optional[str] = None
    createdAt: int
    metadata: Optional[str] = None


class MemorySyncItem(BaseModel):
    memoryId: Optional[int] = None
    userId: str                        # ✅ lowercase d — matches Supabase
    fact: str
    createdAt: int


class PromptPushPayload(BaseModel):
    records: List[PromptSyncItem]


class MemoryPushPayload(BaseModel):
    records: List[MemorySyncItem]


@router.post("/push/prompts")
def push_prompts(payload: PromptPushPayload, db: Session = Depends(get_db)):
    for item in payload.records:
        existing = db.execute(
            sql_text('SELECT prompt_id FROM "Prompts" WHERE prompt_id = :pid'),
            {"pid": item.prompt_id}
        ).mappings().first() if item.prompt_id else None

        if not existing:
            db.execute(
                sql_text('''
                    INSERT INTO "Prompts"
                    ("userId", user_message, qareeb_response, prompt_type, module,
                     intent_detected, created_at, metadata)
                    VALUES (:userId, :user_message, :qareeb_response, :prompt_type,
                            :module, :intent_detected, :created_at, :metadata)
                '''),
                {
                    "userId": item.userId,
                    "user_message": item.userMessage,
                    "qareeb_response": item.qareebResponse,
                    "prompt_type": item.promptType,
                    "module": item.module,
                    "intent_detected": item.intentDetected,
                    "created_at": item.createdAt,
                    "metadata": item.metadata,
                }
            )
    db.commit()
    return {"status": "ok", "count": len(payload.records)}


@router.get("/pull/prompts")
def pull_prompts(userID: str, last_sync: str, db: Session = Depends(get_db)):
    try:
        since = int(datetime.fromisoformat(last_sync).timestamp() * 1000)
    except Exception:
        since = 0

    rows = db.execute(
        sql_text('''
            SELECT * FROM "Prompts"
            WHERE "userId" = :userID AND created_at > :since
            ORDER BY created_at DESC LIMIT 20
        '''),
        {"userID": userID, "since": since}
    ).mappings().all()

    return {
        "records": [
            {
                "promptId": r["prompt_id"],
                "userId": r["userId"],
                "userMessage": r["user_message"],
                "qareebResponse": r["qareeb_response"],
                "promptType": r["prompt_type"],
                "module": r["module"],
                "intentDetected": r["intent_detected"],
                "createdAt": r["created_at"],
                "metadata": r.get("metadata"),
            }
            for r in rows
        ],
        "server_time": datetime.now(timezone.utc).isoformat()
    }


@router.post("/push/memory")
def push_memory(payload: MemoryPushPayload, db: Session = Depends(get_db)):
    for item in payload.records:
        existing = db.execute(
            sql_text('SELECT memory_id FROM "Memory" WHERE "userId" = :uid AND fact = :fact'),
            {"uid": item.userId, "fact": item.fact}
        ).mappings().first()

        if existing:
            db.execute(
                sql_text('UPDATE "Memory" SET fact = :fact WHERE memory_id = :mid'),
                {"fact": item.fact, "mid": existing["memory_id"]}
            )
        else:
            db.execute(
                sql_text(
                    'INSERT INTO "Memory" ("userId", fact, created_at) '
                    'VALUES (:userId, :fact, :created_at)'
                ),
                {"userId": item.userId, "fact": item.fact, "created_at": item.createdAt}
            )
    db.commit()
    return {"status": "ok"}


@router.get("/pull/memory")
def pull_memory(userID: str, db: Session = Depends(get_db)):
    rows = db.execute(
        sql_text('SELECT * FROM "Memory" WHERE "userId" = :userID ORDER BY "createdAt" ASC'),
        {"userID": userID}
    ).mappings().all()

    return {
        "records": [
            {
                "memoryId": r["memory_id"],
                "userId": r["userId"],
                "fact": r["fact"],
                "createdAt": r["createdAt"],  # ✅ also fix here
            }
            for r in rows
        ],
        "server_time": datetime.now(timezone.utc).isoformat()
    }
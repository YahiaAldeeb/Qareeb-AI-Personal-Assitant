import logging
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session
from sqlalchemy import text as sql_text
from app.database import get_db

router = APIRouter(prefix="/notifications", tags=["notifications"])
logger = logging.getLogger(__name__)


class FCMTokenRequest(BaseModel):
    userID: str
    fcm_token: str


@router.post("/register-token")
def register_fcm_token(request: FCMTokenRequest, db: Session = Depends(get_db)):
    """
    Called by Android app on startup to register/update FCM token.
    """
    try:
        db.execute(
            sql_text('UPDATE "User" SET fcm_token = :token WHERE "userID" = :userID'),
            {"token": request.fcm_token, "userID": request.userID}
        )
        db.commit()
        logger.info("register_fcm_token: saved token for userID=%s", request.userID)
        return {"status": "ok"}
    except Exception as e:
        logger.exception("register_fcm_token: failed")
        db.rollback()
        return {"status": "error", "error": str(e)}
    

    # Add to app/routers/notifications.py

@router.post("/test-notify/{userID}")
def test_notification(userID: str, db: Session = Depends(get_db)):
    """Test endpoint — manually trigger notification check for one user."""
    from app.services.notification import (
        get_user_memories_for_notification,
        send_push_notification,
    )
    from app.services.suggestion import get_upcoming_suggestions

    memories = get_user_memories_for_notification(db, userID)
    suggestion = get_upcoming_suggestions(db, userID, memories)

    if not suggestion:
        return {"status": "no_suggestion", "memories": memories}

    # Get user's FCM token
    row = db.execute(
        sql_text('SELECT fcm_token FROM "User" WHERE "userID" = :uid'),
        {"uid": userID}
    ).mappings().first()

    if not row or not row["fcm_token"]:
        return {"status": "no_token", "suggestion": suggestion}

    sent = send_push_notification(
        fcm_token=row["fcm_token"],
        title="Qareeb Reminder 🔔",
        body=suggestion,
    )

    return {
        "status": "sent" if sent else "failed",
        "suggestion": suggestion,
    }
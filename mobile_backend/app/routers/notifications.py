import logging
from datetime import datetime, timedelta

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session
from sqlalchemy import text as sql_text

from app.database import get_db

router = APIRouter(
    prefix="/notifications",
    tags=["notifications"]
)

logger = logging.getLogger(__name__)


# =========================
# REQUEST MODEL
# =========================

class FCMTokenRequest(BaseModel):
    userID: str
    fcm_token: str


# =========================
# REGISTER TOKEN
# =========================

@router.post("/register-token")
def register_fcm_token(
    request: FCMTokenRequest,
    db: Session = Depends(get_db)
):
    """
    Register/update FCM token from Android app.
    """

    try:

        db.execute(
            sql_text(
                '''
                UPDATE "User"
                SET fcm_token = :token
                WHERE "userID" = :userID
                '''
            ),
            {
                "token": request.fcm_token,
                "userID": request.userID
            }
        )

        db.commit()

        logger.info(
            "register_fcm_token: saved token for userID=%s",
            request.userID
        )

        return {
            "status": "ok"
        }

    except Exception as e:

        logger.exception(
            "register_fcm_token: failed"
        )

        db.rollback()

        return {
            "status": "error",
            "error": str(e)
        }


# =========================
# TEST NOTIFICATION
# =========================

@router.post("/test-notify/{userID}")
def test_notification(
    userID: str,
    db: Session = Depends(get_db)
):

    from app.services.notification import (
        get_user_memories_for_notification,
        send_push_notification,
    )

    from app.services.suggestion import (
        get_upcoming_suggestions
    )

    today = datetime.now()
    tomorrow = today + timedelta(days=1)

    day_names = [
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
    ]

    # =========================
    # GET MEMORIES
    # =========================

    memories = get_user_memories_for_notification(
        db,
        userID
    )

    logger.info(
        "test_notification: today=%s (%s), tomorrow=%s (%s), memories=%s",
        today.strftime("%Y-%m-%d"),
        day_names[today.weekday()],
        tomorrow.strftime("%Y-%m-%d"),
        day_names[tomorrow.weekday()],
        memories
    )

    # =========================
    # GET SUGGESTIONS
    # =========================

    suggestions = get_upcoming_suggestions(
        db,
        userID,
        memories
    )

    logger.info(
        "test_notification: suggestions=%r",
        suggestions
    )

    if not suggestions:

        return {
            "status": "no_suggestion",
            "memories": memories
        }

    # =========================
    # GET FCM TOKEN
    # =========================

    row = db.execute(
        sql_text(
            '''
            SELECT fcm_token
            FROM "User"
            WHERE "userID" = :uid
            '''
        ),
        {"uid": userID}
    ).mappings().first()

    if not row or not row["fcm_token"]:

        return {
            "status": "no_token",
            "suggestions": suggestions
        }

    fcm_token = row["fcm_token"]

    logger.info(
        "test_notification: sending to fcm_token=%s...",
        fcm_token[:20]
    )

    # =========================
    # SEND EACH NOTIFICATION
    # =========================

    results = []

    for suggestion in suggestions:

        logger.info(
            "test_notification: sending suggestion=%r",
            suggestion
        )

        sent = send_push_notification(

            fcm_token=fcm_token,

            title="Qareeb Reminder 🔔",

            body=str(suggestion),

            data={
                "suggestion": str(suggestion),
                "action": "CREATE_TASK",
                "userID": str(userID),
            }
        )

        logger.info(
            "test_notification: sent=%s",
            sent
        )

        results.append({
            "suggestion": suggestion,
            "sent": sent
        })

    # =========================
    # RESPONSE
    # =========================

    return {
        "status": "done",
        "notifications_sent": len(results),
        "results": results,
        "fcm_token_prefix": fcm_token[:20] + "..."
    }
import logging
import os
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from sqlalchemy import text as sql_text
import firebase_admin
from firebase_admin import credentials, messaging

logger = logging.getLogger(__name__)

# Initialize Firebase once
_firebase_initialized = False

def init_firebase():
    global _firebase_initialized
    if not _firebase_initialized:
        try:
            cred_path = os.path.join(
                os.path.dirname(__file__), "..", "..", "firebase_credentials.json"
            )
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)
            _firebase_initialized = True
            logger.info("init_firebase: Firebase initialized successfully")
        except Exception as e:
            logger.exception("init_firebase: failed, error=%s", e)


def send_push_notification(fcm_token: str, title: str, body: str) -> bool:
    """Send a single push notification to a device."""
    try:
        init_firebase()
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            token=fcm_token,
            android=messaging.AndroidConfig(
                priority="high",
                notification=messaging.AndroidNotification(
                    sound="default",
                    priority="high",
                ),
            ),
        )
        response = messaging.send(message)
        logger.info("send_push_notification: sent successfully, response=%s", response)
        return True
    except Exception as e:
        logger.exception("send_push_notification: failed, error=%s", e)
        return False


def get_all_users_with_tokens(db: Session) -> list[dict]:
    """Fetch all users who have FCM tokens registered."""
    try:
        rows = db.execute(
            sql_text('''
                SELECT "userID", fcm_token
                FROM "User"
                WHERE fcm_token IS NOT NULL
                AND fcm_token != ''
            ''')
        ).mappings().all()
        return [dict(r) for r in rows]
    except Exception as e:
        logger.exception("get_all_users_with_tokens: failed, error=%s", e)
        db.rollback()
        return []


def get_user_memories_for_notification(db: Session, userID: str) -> list[str]:
    """Fetch memories for a specific user."""
    try:
        rows = db.execute(
            sql_text('''
                SELECT fact FROM "Memory"
                WHERE "userId" = :userID
                ORDER BY "createdAt" ASC
            '''),
            {"userID": userID}
        ).mappings().all()
        return [r["fact"] for r in rows]
    except Exception as e:
        logger.exception("get_user_memories_for_notification: failed")
        db.rollback()
        return []


def check_and_notify_all_users(db: Session) -> None:
    """
    Main scheduler job.
    Runs daily — checks every user's memories and sends
    push notifications for upcoming events tomorrow.
    """
    from app.services.suggestion import get_upcoming_suggestions

    logger.info("check_and_notify_all_users: starting daily notification check")

    users = get_all_users_with_tokens(db)
    logger.info("check_and_notify_all_users: found %d users with tokens", len(users))

    notified = 0
    for user in users:
        userID = user["userID"]
        fcm_token = user["fcm_token"]

        try:
            memories = get_user_memories_for_notification(db, userID)
            if not memories:
                continue

            suggestion = get_upcoming_suggestions(db, userID, memories)
            if suggestion:
                logger.info(
                    "check_and_notify_all_users: sending notification to userID=%s: %r",
                    userID, suggestion
                )
                sent = send_push_notification(
                    fcm_token=fcm_token,
                    title="Qareeb Reminder 🔔",
                    body=suggestion,
                )
                if sent:
                    notified += 1
        except Exception as e:
            logger.exception(
                "check_and_notify_all_users: error for userID=%s, error=%s",
                userID, e
            )

    logger.info(
        "check_and_notify_all_users: done, notified %d/%d users",
        notified, len(users)
    )
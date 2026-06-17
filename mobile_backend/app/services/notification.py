import logging
import os
from sqlalchemy.orm import Session
from sqlalchemy import text as sql_text
import firebase_admin
from firebase_admin import credentials, messaging

logger = logging.getLogger(__name__)

# =========================
# FIREBASE INIT FLAG
# =========================

_firebase_initialized = False


# =========================
# SEND PUSH NOTIFICATION
# =========================

def send_push_notification(
    fcm_token: str,
    title: str,
    body: str,
    data: dict = None
) -> bool:

    try:
        init_firebase()

        logger.info(
            "send_push_notification: sending to token=%s...",
            fcm_token[:20]
        )

        # ✅ Firebase requires ALL data values to be strings
        safe_data = {
            str(k): str(v)
            for k, v in (data or {}).items()
        }

        # ✅ Ensure title/body are strings
        safe_title = str(title)
        safe_body = str(body)

        message = messaging.Message(

            notification=messaging.Notification(
                title=safe_title,
                body=safe_body,
            ),

            data=safe_data,

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

        logger.info(
            "send_push_notification: sent successfully, response=%s",
            response
        )

        return True

    except Exception as e:

        logger.exception(
            "send_push_notification: failed, error=%s",
            e
        )

        return False


# =========================
# INIT FIREBASE
# =========================

def init_firebase():

    global _firebase_initialized

    if not _firebase_initialized:

        try:

            cred_path = os.path.join(
                os.path.dirname(__file__),
                "..",
                "firebase_credentials.json"
            )

            cred_path = os.path.abspath(cred_path)

            logger.info(
                "init_firebase: loading from path=%s",
                cred_path
            )

            logger.info(
                "init_firebase: file exists=%s",
                os.path.exists(cred_path)
            )

            if not os.path.exists(cred_path):

                logger.error(
                    "init_firebase: credentials file NOT FOUND at %s",
                    cred_path
                )

                return

            cred = credentials.Certificate(cred_path)

            firebase_admin.initialize_app(cred)

            _firebase_initialized = True

            logger.info(
                "init_firebase: Firebase initialized successfully"
            )

        except Exception as e:

            logger.exception(
                "init_firebase: failed, error=%s",
                e
            )


# =========================
# GET USERS WITH TOKENS
# =========================

def get_all_users_with_tokens(db: Session) -> list[dict]:

    try:

        rows = db.execute(
            sql_text(
                '''
                SELECT "userID", fcm_token
                FROM "User"
                WHERE fcm_token IS NOT NULL
                AND fcm_token != ''
                '''
            )
        ).mappings().all()

        return [dict(r) for r in rows]

    except Exception as e:

        logger.exception(
            "get_all_users_with_tokens: failed, error=%s",
            e
        )

        db.rollback()

        return []


# =========================
# GET USER MEMORIES
# =========================

def get_user_memories_for_notification(
    db: Session,
    userID: str
) -> list[str]:

    try:

        rows = db.execute(
            sql_text(
                '''
                SELECT fact
                FROM "Memory"
                WHERE "userId" = :userID
                ORDER BY "createdAt" ASC
                '''
            ),
            {"userID": userID}
        ).mappings().all()

        return [r["fact"] for r in rows]

    except Exception as e:

        logger.exception(
            "get_user_memories_for_notification: failed, error=%s",
            e
        )

        db.rollback()

        return []


# =========================
# MAIN DAILY CHECK
# =========================

def check_and_notify_all_users(db: Session) -> None:
    """
    Runs daily.

    Checks all user memories and sends notifications
    for tomorrow's recurring events.
    """

    from app.services.suggestion import get_upcoming_suggestions

    logger.info(
        "check_and_notify_all_users: starting daily notification check"
    )

    users = get_all_users_with_tokens(db)

    logger.info(
        "check_and_notify_all_users: found %d users with tokens",
        len(users)
    )

    notified = 0

    for user in users:

        userID = user["userID"]
        fcm_token = user["fcm_token"]

        try:

            memories = get_user_memories_for_notification(
                db,
                userID
            )

            if not memories:

                logger.info(
                    "check_and_notify_all_users: no memories for userID=%s",
                    userID
                )

                continue

            # ✅ NOW RETURNS LIST[str]
            suggestions = get_upcoming_suggestions(
                db,
                userID,
                memories
            )

            logger.info(
                "check_and_notify_all_users: got %d suggestions for userID=%s",
                len(suggestions),
                userID
            )

            if not suggestions:
                continue

            # ✅ SEND EACH SUGGESTION SEPARATELY
            for suggestion in suggestions:

                logger.info(
                    "check_and_notify_all_users: sending suggestion=%r",
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
                    "check_and_notify_all_users: sent=%s",
                    sent
                )

                if sent:
                    notified += 1

        except Exception as e:

            logger.exception(
                "check_and_notify_all_users: error for userID=%s, error=%s",
                userID,
                e
            )

    logger.info(
        "check_and_notify_all_users: done, notified %d notifications",
        notified
    )
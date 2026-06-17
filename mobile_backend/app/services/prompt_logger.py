import logging
from datetime import datetime, timezone
from sqlalchemy.orm import Session
from sqlalchemy import text as sql_text

logger = logging.getLogger(__name__)


def save_interaction(
    db: Session,
    userID: str,
    user_message: str,
    qareeb_response: str,
    intent: str = "UNKNOWN",
    module: str = "CHATBOT",
    prompt_type: str = "TEXT",
) -> None:
    """
    Save every AI interaction to Prompts table automatically.
    This builds the history that behavioral analysis reads from.
    """
    try:
        now_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
        db.execute(
            sql_text('''
                INSERT INTO "Prompts"
                ("userId", user_message, qareeb_response,
                 prompt_type, module, intent_detected, created_at)
                VALUES
                (:userId, :user_message, :qareeb_response,
                 :prompt_type, :module, :intent_detected, :created_at)
            '''),
            {
                "userId": userID,
                "user_message": user_message,
                "qareeb_response": qareeb_response,
                "prompt_type": prompt_type,
                "module": module,
                "intent_detected": intent,
                "created_at": now_ms,
            }
        )
        db.commit()
        logger.info(
            "save_interaction: saved for userID=%s, intent=%s",
            userID, intent
        )
    except Exception as e:
        logger.exception("save_interaction: failed, error=%s", e)
        db.rollback()
import json
import logging
from datetime import datetime, timezone
from sqlalchemy.orm import Session
from sqlalchemy import text as sql_text
from groq import Groq
import os

logger = logging.getLogger(__name__)
groq_client = Groq(api_key=os.environ.get("GROQ_API_KEY"))


def extract_memory_facts(conversation: str, existing_memories: list[str]) -> list[str]:
    """
    Extract explicit facts from a single conversation snippet.
    These are facts the user directly told Qareeb.
    """
    existing_str = (
        "\n".join(f"- {m}" for m in existing_memories)
        if existing_memories else "None yet."
    )

    prompt = f"""
You are a memory extraction assistant for a personal AI called Qareeb.

Your job is to extract long-term personal facts about the user from the conversation below.
These are facts the user EXPLICITLY mentioned.

Good facts to extract:
- Recurring schedules ("has tennis every Wednesday")
- Personal preferences ("prefers to study at 8am")
- Habits ("goes to the gym on weekdays")
- Goals ("is preparing for an OS exam")

Do NOT extract:
- One-time tasks with no recurring pattern
- Facts already in existing memories
- Vague or useless information

Existing memories (do not duplicate):
{existing_str}

Conversation:
{conversation}

Output ONLY a JSON array of strings. Each string is one fact.
If nothing new is worth remembering, output: []
"""
    try:
        completion = groq_client.chat.completions.create(
            model="openai/gpt-oss-120b",
            messages=[{"role": "user", "content": prompt}],
            temperature=0,
            max_completion_tokens=512,
            top_p=1,
            stream=False,
        )
        response_text = completion.choices[0].message.content.strip()

        if "```json" in response_text:
            response_text = response_text.split("```json")[1].split("```")[0].strip()
        elif "```" in response_text:
            response_text = response_text.split("```")[1].split("```")[0].strip()

        facts = json.loads(response_text)
        return [str(f) for f in facts if f] if isinstance(facts, list) else []
    except Exception as e:
        logger.exception("extract_memory_facts: failed, error=%s", e)
        return []


def get_user_memories(db: Session, userID: str) -> list[str]:
    """Fetch all memory facts for a user."""
    try:
        rows = db.execute(
            sql_text(
                'SELECT fact FROM "Memory" WHERE "userId" = :userID '
                'ORDER BY "createdAt" ASC'
            ),
            {"userID": userID}
        ).mappings().all()
        return [row["fact"] for row in rows]
    except Exception as e:
        logger.exception("get_user_memories: failed, error=%s", e)
        db.rollback()
        return []


def save_memory_facts(db: Session, userID: str, facts: list[str]) -> None:
    """Save new memory facts, avoiding exact duplicates."""
    if not facts:
        return
    try:
        now_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
        saved = 0
        for fact in facts:
            existing = db.execute(
                sql_text(
                    'SELECT memory_id FROM "Memory" '
                    'WHERE "userId" = :userID AND fact = :fact'
                ),
                {"userID": userID, "fact": fact}
            ).mappings().first()

            if not existing:
                db.execute(
                    sql_text(
                        'INSERT INTO "Memory" ("userId", fact, "createdAt") '
                        'VALUES (:userID, :fact, :createdAt)'
                    ),
                    {"userID": userID, "fact": fact, "createdAt": now_ms}
                )
                saved += 1

        db.commit()
        logger.info(
            "save_memory_facts: saved %d new facts for userID=%s",
            saved, userID
        )
    except Exception as e:
        logger.exception("save_memory_facts: failed, error=%s", e)
        db.rollback()


def build_memory_context(memories: list[str]) -> str:
    """Format memories into a context block for LLM prompts."""
    if not memories:
        return ""
    facts_str = "\n".join(f"- {m}" for m in memories)
    return f"""
What you know about this user (long-term memory):
{facts_str}
Use this context to personalize your responses, suggest correct dates,
priorities, and anticipate the user's needs.
""".strip()
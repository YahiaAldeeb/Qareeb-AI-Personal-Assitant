import json
import logging
from datetime import datetime, timezone
from sqlalchemy.orm import Session
from sqlalchemy import text as sql_text
from groq import Groq
import os

logger = logging.getLogger(__name__)
groq_client = Groq(api_key=os.environ.get("GROQ_API_KEY"))


def get_recent_interactions(db: Session, userID: str, limit: int = 50) -> list[dict]:
    """
    Fetch recent prompt history for a user to analyze behavior.
    """
    try:
        rows = db.execute(
            sql_text('''
                SELECT user_message, qareeb_response, prompt_type,
                       module, intent_detected, created_at
                FROM "Prompts"
                WHERE "userId" = :userID
                ORDER BY created_at DESC
                LIMIT :limit
            '''),
            {"userID": userID, "limit": limit}
        ).mappings().all()
        return [dict(r) for r in rows]
    except Exception as e:
        logger.exception("get_recent_interactions: failed, error=%s", e)
        db.rollback()
        return []


def has_enough_data(interactions: list[dict], tasks: list[dict], transactions: list[dict]) -> bool:
    """
    Only run analysis if there's enough data to find real patterns.
    Need at least 10 interactions OR 5 tasks to have meaningful patterns.
    """
    return len(interactions) >= 10 or len(tasks) >= 5

def get_interaction_count(db: Session, userID: str) -> int:
    """Get total number of interactions for a user."""
    try:
        row = db.execute(
            sql_text('SELECT COUNT(*) as cnt FROM "Prompts" WHERE "userId" = :userID'),
            {"userID": userID}
        ).mappings().first()
        return row["cnt"] if row else 0
    except Exception as e:
        logger.exception("get_interaction_count: failed, error=%s", e)
        db.rollback()
        return 0


def get_recent_tasks(db: Session, userID: str, limit: int = 30) -> list[dict]:
    """Fetch recent tasks created by the user to find scheduling patterns."""
    try:
        rows = db.execute(
            sql_text('''
                SELECT title, description, priority, status,
                       "dueDate", created_at, updated_at
                FROM "Task"
                WHERE "userID" = :userID
                AND is_deleted = false
                ORDER BY created_at DESC
                LIMIT :limit
            '''),
            {"userID": userID, "limit": limit}
        ).mappings().all()
        return [dict(r) for r in rows]
    except Exception as e:
        logger.exception("get_recent_tasks: failed, error=%s", e)
        db.rollback()
        return []


def get_recent_transactions(db: Session, userID: str, limit: int = 30) -> list[dict]:
    """Fetch recent transactions to find financial behavior patterns."""
    try:
        rows = db.execute(
            sql_text('''
                SELECT title, amount, source, description,
                       income, state, date, created_at
                FROM "Transaction"
                WHERE "userID" = :userID
                AND is_deleted = false
                ORDER BY created_at DESC
                LIMIT :limit
            '''),
            {"userID": userID, "limit": limit}
        ).mappings().all()
        return [dict(r) for r in rows]
    except Exception as e:
        logger.exception("get_recent_transactions: failed, error=%s", e)
        db.rollback()
        return []


def get_existing_behavioral_memories(db: Session, userID: str) -> list[str]:
    """Get memories that were auto-learned (tagged as behavioral)."""
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
        logger.exception("get_existing_behavioral_memories: failed, error=%s", e)
        db.rollback()
        return []


def _serialize_for_prompt(data: list[dict]) -> str:
    """Convert list of dicts to readable string for LLM prompt."""
    if not data:
        return "None"
    lines = []
    for item in data:
        parts = []
        for k, v in item.items():
            if v is not None:
                parts.append(f"{k}={v}")
        lines.append(", ".join(parts))
    return "\n".join(lines)


def analyze_behavior_patterns(
    db: Session,
    userID: str,
) -> list[str]:
    """
    Core function: analyze all user data and extract behavioral patterns.
    Returns list of new facts to save to memory.
    """
    logger.info("analyze_behavior_patterns: starting for userID=%s", userID)

    # Gather all data
    interactions = get_recent_interactions(db, userID, limit=50)
    tasks = get_recent_tasks(db, userID, limit=30)
    transactions = get_recent_transactions(db, userID, limit=30)
    existing_memories = get_existing_behavioral_memories(db, userID)

    if not interactions and not tasks and not transactions:
        logger.info("analyze_behavior_patterns: no data to analyze")
        return []

    existing_str = "\n".join(f"- {m}" for m in existing_memories) if existing_memories else "None yet."
    interactions_str = _serialize_for_prompt(interactions[:20])
    tasks_str = _serialize_for_prompt(tasks)
    transactions_str = _serialize_for_prompt(transactions)

    prompt = f"""
You are a strict behavioral pattern analyzer for a personal AI assistant called Qareeb.

Your job is to find ONLY genuine recurring patterns from the user's history.
You must be very conservative — it is better to miss a pattern than to invent one.

--- EXISTING MEMORIES (do not duplicate) ---
{existing_str}

--- RECENT CONVERSATIONS ---
{interactions_str}

--- RECENT TASKS ---
{tasks_str}

--- RECENT TRANSACTIONS ---
{transactions_str}

--- STRICT RULES ---

1. MINIMUM OCCURRENCES: A pattern is ONLY valid if you see it at least 3 times clearly.
   - Seen once → NOT a pattern, ignore completely
   - Seen twice → NOT a pattern, ignore completely  
   - Seen 3+ times consistently → valid pattern

2. WHAT COUNTS AS A PATTERN:
   - Same task type on same day repeatedly (3+ times)
   - Same priority level for same category (3+ times)
   - Same type of expense recurring (3+ times)
   - Consistent time-of-day behavior (3+ times)

3. WHAT IS NOT A PATTERN:
   - Anything mentioned only once or twice
   - Coincidences (two similar tasks don't make a pattern)
   - General statements without repetition evidence

4. Do NOT duplicate existing memories
5. Write facts as: "User consistently...", "User always...", "User regularly..."
   NOT: "User tends to...", "User may...", "User seems to..."
   (Only use strong language if truly confident from 3+ occurrences)

Output ONLY a JSON array of strings.
If you cannot find patterns with 3+ clear occurrences, output: []

Example of BAD output (only seen once): ["User tracks food expenses"]
Example of GOOD output (seen 3+ times): ["User consistently has lectures on Mondays"]
"""

    try:
        completion = groq_client.chat.completions.create(
            model="openai/gpt-oss-120b",
            messages=[{"role": "user", "content": prompt}],
            temperature=0,
            max_completion_tokens=1024,
            top_p=1,
            stream=False,
        )
        response_text = completion.choices[0].message.content.strip()

        if "```json" in response_text:
            response_text = response_text.split("```json")[1].split("```")[0].strip()
        elif "```" in response_text:
            response_text = response_text.split("```")[1].split("```")[0].strip()

        facts = json.loads(response_text)
        if isinstance(facts, list):
            new_facts = [str(f) for f in facts if f]
            logger.info(
                "analyze_behavior_patterns: found %d new patterns: %s",
                len(new_facts), new_facts
            )
            return new_facts
        return []
    except Exception as e:
        logger.exception("analyze_behavior_patterns: LLM call failed, error=%s", e)
        return []


def save_behavioral_facts(db: Session, userID: str, facts: list[str]) -> None:
    """Save behavioral facts to Memory table, avoiding duplicates."""
    if not facts:
        return
    try:
        now_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
        saved = 0
        for fact in facts:
            existing = db.execute(
                sql_text('SELECT memory_id FROM "Memory" WHERE "userId" = :userID AND fact = :fact'),
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
            "save_behavioral_facts: saved %d/%d facts for userID=%s",
            saved, len(facts), userID
        )
    except Exception as e:
        logger.exception("save_behavioral_facts: failed, error=%s", e)
        db.rollback()


def should_run_analysis(db: Session, userID: str) -> bool:
    count = get_interaction_count(db, userID)
    should = count > 0 and count % 10 == 0  # ✅ changed from 5 to 10
    logger.info(
        "should_run_analysis: userID=%s, count=%d, will_run=%s",
        userID, count, should
    )
    return should


async def run_behavioral_analysis_if_needed(db: Session, userID: str) -> None:
    try:
        if should_run_analysis(db, userID):
            # Pre-check: enough data?
            interactions = get_recent_interactions(db, userID, limit=50)
            tasks = get_recent_tasks(db, userID, limit=30)
            transactions = get_recent_transactions(db, userID, limit=30)

            if not has_enough_data(interactions, tasks, transactions):
                logger.info(
                    "run_behavioral_analysis_if_needed: not enough data yet "
                    "(interactions=%d, tasks=%d), skipping",
                    len(interactions), len(tasks)
                )
                return

            logger.info("run_behavioral_analysis_if_needed: running analysis for userID=%s", userID)
            facts = analyze_behavior_patterns(db, userID)
            if facts:
                save_behavioral_facts(db, userID, facts)
    except Exception as e:
        logger.exception("run_behavioral_analysis_if_needed: error=%s", e)
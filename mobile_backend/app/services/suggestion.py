import logging
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from groq import Groq
import os

logger = logging.getLogger(__name__)
groq_client = Groq(api_key=os.environ.get("GROQ_API_KEY"))


def get_upcoming_suggestions(db: Session, userID: str, memories: list[str]) -> str | None:
    """
    Look at user memories and today's date.
    If a recurring event is coming up tomorrow, return a suggestion message.
    Returns None if no suggestion needed.
    """
    if not memories:
        return None

    today = datetime.now()
    tomorrow = today + timedelta(days=1)
    day_names = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    tomorrow_name = day_names[tomorrow.weekday()]
    today_name = day_names[today.weekday()]
    tomorrow_date = tomorrow.strftime("%Y-%m-%d")

    memories_str = "\n".join(f"- {m}" for m in memories)

    prompt = f"""
You are Qareeb, a proactive personal AI assistant.

Today is {today_name}, {today.strftime("%Y-%m-%d")}.
Tomorrow is {tomorrow_name}, {tomorrow_date}.

User's known recurring schedule and preferences:
{memories_str}

Your job: Check if any recurring event from the user's memory is happening TOMORROW.

If YES → write a short friendly suggestion message asking if they want you to create it as a task.
If NO → output exactly: null

Rules:
- Only suggest if the recurring event clearly matches tomorrow's day
- Be conversational and friendly
- Keep it to one sentence
- End with a question mark

Example output when match found:
"You have tennis training tomorrow (Monday) — want me to add it to your tasks?"

Example output when no match:
null

Output ONLY the suggestion string or null. Nothing else.
"""
    try:
        completion = groq_client.chat.completions.create(
            model="openai/gpt-oss-120b",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_completion_tokens=100,
            top_p=1,
            stream=False,
        )
        response_text = completion.choices[0].message.content.strip()

        if response_text.lower() == "null" or response_text == "":
            return None

        response_text = response_text.strip('"').strip("'")
        logger.info("get_upcoming_suggestions: suggestion=%r", response_text)
        return response_text

    except Exception as e:
        logger.exception("get_upcoming_suggestions: failed, error=%s", e)
        return None


def handle_suggestion_response(user_text: str) -> bool:
    """Check if the user is responding yes to a suggestion."""
    yes_keywords = [
        "yes", "yeah", "yep", "sure", "ok", "okay",
        "do it", "add it", "create it", "go ahead", "please"
    ]
    return any(word in user_text.lower() for word in yes_keywords)
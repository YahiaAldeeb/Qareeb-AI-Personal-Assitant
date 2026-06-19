import logging
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from groq import Groq
import os

logger = logging.getLogger(__name__)
groq_client = Groq(api_key=os.environ.get("GROQ_API_KEY"))


def get_upcoming_suggestions(
    db: Session,
    userID: str,
    memories: list[str]
) -> list[str]:
    """
    Look at user memories and today's date.
    If recurring events are coming up tomorrow,
    return a list of suggestion messages.

    Example return:
    [
        "You have gym tomorrow (Monday) — want me to add it to your tasks?",
        "You have swimming tomorrow (Monday) — want me to add it to your tasks?"
    ]

    Returns [] if no suggestions needed.
    """

    if not memories:
        logger.info(
            "get_upcoming_suggestions: no memories for userID=%s",
            userID
        )
        return []

    today = datetime.now()
    tomorrow = today + timedelta(days=1)

    day_names = [
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday",
    ]

    today_name = day_names[today.weekday()]
    tomorrow_name = day_names[tomorrow.weekday()]
    tomorrow_date = tomorrow.strftime("%Y-%m-%d")

    logger.info(
        "get_upcoming_suggestions: today=%s (%s), tomorrow=%s (%s)",
        today.strftime("%Y-%m-%d"),
        today_name,
        tomorrow_date,
        tomorrow_name,
    )

    memories_str = "\n".join(f"- {m}" for m in memories)

    prompt = f"""
Today is {today_name}, {today.strftime("%Y-%m-%d")}.
Tomorrow is {tomorrow_name}, {tomorrow_date}.

User memories:
{memories_str}

Find ALL recurring events happening tomorrow ({tomorrow_name}).

Rules:
- Return one line per event
- Each line must be a short friendly reminder
- Do NOT number the lines
- If no events exist, return exactly: null

Example:
You have gym tomorrow (Monday) — want me to add it to your tasks?
You have swimming tomorrow (Monday) — want me to add it to your tasks?

Your answer:
"""

    try:
        completion = groq_client.chat.completions.create(
            model="openai/gpt-oss-120b",
            messages=[
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            temperature=0.3,
            max_completion_tokens=500,
            top_p=1,
            stream=False,
        )

        response_text = completion.choices[0].message.content.strip()

        logger.info(
            "get_upcoming_suggestions: raw LLM response=%r",
            response_text
        )

        # No suggestions
        if (
            not response_text
            or response_text.lower().strip() == "null"
        ):
            logger.info(
                "get_upcoming_suggestions: no upcoming suggestions found"
            )
            return []

        # Split into multiple suggestions
        suggestions = []

        for line in response_text.splitlines():

            clean_line = (
                line.strip()
                .strip('"')
                .strip("'")
                .strip("-")
                .strip()
            )

            if clean_line:
                suggestions.append(clean_line)

        logger.info(
            "get_upcoming_suggestions: parsed %d suggestions",
            len(suggestions)
        )

        for s in suggestions:
            logger.info(
                "get_upcoming_suggestions: suggestion=%r",
                s
            )

        return suggestions

    except Exception as e:
        logger.exception(
            "get_upcoming_suggestions: failed, error=%s",
            e
        )
        return []


def handle_suggestion_response(user_text: str) -> bool:
    """Check if the user is responding yes to a suggestion."""
    yes_keywords = [
        "yes", "yeah", "yep", "sure", "ok", "okay",
        "do it", "add it", "create it", "go ahead", "please"
    ]
    return any(word in user_text.lower() for word in yes_keywords)
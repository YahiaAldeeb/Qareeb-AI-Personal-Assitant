import os
import aiofiles
from fastapi import UploadFile
from sqlalchemy.orm import Session
from app.services.ai import (
    transcribe,
    extract_intent,
    handle_finance_service,
    handle_task_tracker_service,
    UI_AUTOMATION_PROMPT,
    llm,
    config,
    DroidAgent
)

async def process_command_controller(file: UploadFile, db: Session):
    temp_audio = f"command_{os.urandom(4).hex()}.wav"
    try:
        async with aiofiles.open(temp_audio, "wb") as out:
            content = await file.read()
            await out.write(content)

        text = transcribe(temp_audio)
        intent = extract_intent(text)

        if intent == "UI_AUTOMATION":
            agent = DroidAgent(
                goal=f"{UI_AUTOMATION_PROMPT}\n\nTASK:\n{text}",
                config=config,
                llms={
                    "manager": llm,
                    "executor": llm,
                    "codeact": llm,
                    "text_manipulator": llm,
                    "app_opener": llm
                }
            )
            result = await agent.run()
            return {
                "status": "success",
                "intent": intent,
                "transcription": text,
                "automation_success": result.success
            }
        elif intent == "FINANCE":
            result = await handle_finance_service(text, db)
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "transcription": text,
                "result": result
            }
        elif intent == "TASK_TRACKER":
            result = await handle_task_tracker_service(text, db)
            return {
                "status": "success" if result.get("success") else "error",
                "intent": intent,
                "transcription": text,
                "result": result
            }
        else:
            return {"status": "unknown_intent", "transcription": text}

    except Exception as e:
        return {"status": "error", "message": str(e)}

    finally:
        if os.path.exists(temp_audio):
            os.remove(temp_audio)

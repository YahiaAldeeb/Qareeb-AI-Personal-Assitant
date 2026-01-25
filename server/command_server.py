import os
import aiofiles
import whisper
import uvicorn
from fastapi import FastAPI, UploadFile, File
from groq import Groq
from dotenv import load_dotenv

# ===============================
# ENV SETUP
# ===============================
# Load environment variables from .env file
load_dotenv()

os.environ['HF_HUB_DISABLE_SYMLINKS_WARNING'] = '1'
os.environ['HF_HUB_DISABLE_SYMLINKS'] = '1'

# ===============================
# FASTAPI APP
# ===============================
app = FastAPI()

# ===============================
# GROQ CLIENT (INTENT + DROIDRUN)
# ===============================
groq_client = Groq(
    api_key=os.environ.get("GROQ_API_KEY")
)

# ===============================
# WHISPER (STT)
# ===============================
print("Loading Whisper model...")
whisper_model = whisper.load_model("base")

def transcribe(audio_path: str) -> str:
    result = whisper_model.transcribe(audio_path, fp16=False,language="en")
    return result["text"].strip()

# ===============================
# SPEAKER VERIFICATION
# ===============================
from speechbrain.pretrained import SpeakerRecognition

spkrec = SpeakerRecognition.from_hparams(
    source="speechbrain/spkrec-ecapa-voxceleb"
)

def verify_speaker(enroll_path: str, test_path: str, threshold=0.25) -> bool:
    score, prediction = spkrec.verify_files(enroll_path, test_path)
    print(f"Speaker score: {score}")
    return score > threshold

# ===============================
# INTENT ORCHESTRATOR
# ===============================
def extract_intent(user_input: str) -> str:
    prompt = f"""
You are an intent classifier.
Classify the user request into EXACTLY one of these:
- UI_AUTOMATION
- FINANCE
- TASK_TRACKER
- UNKNOWN
Only output the intent name.

User request: "{user_input}"
"""

    completion = groq_client.chat.completions.create(
        model="openai/gpt-oss-120b",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_completion_tokens=256,
        top_p=1,
        reasoning_effort="medium",
        stream=False
    )

    return completion.choices[0].message.content.strip()

# ===============================
# DROIDRUN SETUP (UI AUTOMATION)
# ===============================
from droidrun import DroidAgent
from droidrun import AgentConfig, DroidrunConfig, CodeActConfig
from llama_index.llms.groq import Groq as LlamaGroq

llm = LlamaGroq(
    model="openai/gpt-oss-120b",
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0
)

config = DroidrunConfig(
    agent=AgentConfig(
        after_sleep_action=0.3,
        wait_for_stable_ui=0.1,
        codeact=CodeActConfig(vision=False)
    )
)

UI_AUTOMATION_PROMPT = """
Mobile UI agent. Continuous.
Minimal actions. No explanations.
Output only actions or FAILED.
""".strip()

# ===============================
# PLACEHOLDER SERVICES
# ===============================
def handle_finance(text: str):
    print("💰 FINANCE API PLACEHOLDER:", text)

def handle_task_tracker(text: str):
    print("📋 TASK TRACKER API PLACEHOLDER:", text)

# ===============================
# MAIN PIPELINE ENDPOINT
# ===============================
@app.post("/transcribe")
async def process_command(file: UploadFile = File(...)):
    """
    Called AFTER:
    - Wake word detected on device
    - Overlay shown on Android
    - Command audio recorded on client
    """

    temp_audio = "command.wav"
    verify_audio = "verify.wav"

    try:
        # 1. Save uploaded audio
        async with aiofiles.open(temp_audio, "wb") as out:
            content = await file.read()
            await out.write(content)

        # 2. Speaker verification (first 2 seconds)
        import soundfile as sf
        audio, sr = sf.read(temp_audio)
        verify_segment = audio[: int(2 * sr)]
        sf.write(verify_audio, verify_segment, sr)

        print("🔐 Verifying speaker...")
        if not verify_speaker("enrolled.wav", verify_audio):
            return {"status": "rejected", "reason": "Speaker not recognized"}

        print("✅ Speaker verified")

        # 3. Transcribe command
        text = transcribe(temp_audio)
        print("🧠 Transcription:", text)

        # 4. Intent orchestration
        intent = extract_intent(text)
        print("🧭 Intent:", intent)

        # 5. Route intent
        if intent == "UI_AUTOMATION":
            print("📱 Executing UI automation")

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
            handle_finance(text)
            return {"status": "success", "intent": intent}

        elif intent == "TASK_TRACKER":
            handle_task_tracker(text)
            return {"status": "success", "intent": intent}

        else:
            return {"status": "unknown_intent", "transcription": text}

    except Exception as e:
        print("❌ Error:", e)
        return {"status": "error", "message": str(e)}

    finally:
        if os.path.exists(temp_audio):
            os.remove(temp_audio)
        if os.path.exists(verify_audio):
            os.remove(verify_audio)

# ===============================
# SERVER START
# ===============================
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)

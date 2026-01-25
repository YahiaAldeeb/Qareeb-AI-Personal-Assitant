import os
from fastapi import FastAPI
from pydantic import BaseModel
from droidrun import DroidAgent, DroidrunConfig
# IMPORT THE OFFICIAL GROQ WRAPPER
from llama_index.llms.groq import Groq 
import uvicorn
import os
import aiofiles
import whisper
from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel
from droidrun import DroidAgent, DroidrunConfig
from droidrun import AgentConfig, DroidrunConfig, CodeActConfig
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

app = FastAPI()


llm = Groq(
    model="openai/gpt-oss-120b", 
    api_key=os.environ.get("GROQ_API_KEY"),
    temperature=0
)

print("Loading Whisper model...")
stt_model = whisper.load_model("base")
config = DroidrunConfig(
    agent=AgentConfig(
        # 1. Reduce wait time after actions (Default is often 1.0s or 2.0s)
        # 0.5s is usually enough for fast apps like YouTube
        after_sleep_action=0.3, 
        
        # 2. Reduce UI stability check (Default ~0.3s)
        wait_for_stable_ui=0.1,

        # 3. Disable Vision (Screenshots) if you haven't already.
        # Sending images adds huge latency (2-4s) per step.
        codeact=CodeActConfig(vision=False) 
    )
)

UI_AUTOMATION_PROMPT = """
Mobile UI agent. Continuous.
Minimal actions. No explanations.
Output only actions or FAILED.
""".strip()

@app.post("/transcribe")
async def transcribe_audio(file: UploadFile = File(...)):
    print(f"Received audio file: {file.filename}")
    
    # Save the uploaded file temporarily
    temp_filename = f"temp_{file.filename}"
    
    try:
        # A. Write audio to disk
        async with aiofiles.open(temp_filename, 'wb') as out_file:
            content = await file.read()
            await out_file.write(content)
            
        # B. Transcribe with Whisper
        print("Transcribing...")
        # Whisper handles WAV files natively
        result = stt_model.transcribe(temp_filename,language="en")
        transcript = result["text"].strip()
        print(f"User said: '{transcript}'")
        
        # C. Execute with Droidrun
        if transcript:
            print(f"Executing DroidAgent with goal: {transcript}")
            # Initialize agent with the transcribed text
            agent = DroidAgent(
            goal=f"{UI_AUTOMATION_PROMPT}\n\nTASK:\n{transcript}",
            config=config,
            llms={
                "manager": llm,
                "executor": llm,
                "codeact": llm,       # The main agent that writes code
                "text_manipulator": llm,
                "app_opener": llm
            }
        )
            
            # Run the agent on the connected device
            agent_result = await agent.run()
            print(f"Execution Success: {agent_result.success}")
        
        # D. Return JSON response to Android
        # Your QareebResponse.kt expects a field named "transcription"
        return {"transcription": transcript}

    except Exception as e:
        print(f"Error: {e}")
        return {"transcription": "Error processing audio"}
        
    finally:
        # Cleanup temp file
        if os.path.exists(temp_filename):
            os.remove(temp_filename)

if __name__ == "__main__":
    # Host 0.0.0.0 is required to allow connections from the phone/emulator
    uvicorn.run(app, host="0.0.0.0", port=8000)
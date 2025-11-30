import whisper
from fastapi import FastAPI, UploadFile, File
import uvicorn
import shutil
import os
import time

# Load whisper model once
# Use "small" or "medium" if your PC has a good GPU for better accuracy
print("Loading Whisper Model...")
whisper_model = whisper.load_model("base")
print("Model Loaded.")

# Create a folder to store the saved audio files
SAVE_DIR = "recorded_commands"
os.makedirs(SAVE_DIR, exist_ok=True)

app = FastAPI()

@app.post("/transcribe")
async def transcribe_audio(file: UploadFile = File(...)):
    print(f"--> Receiving Audio: {file.filename}")
    
    # 1. Generate a unique filename with a timestamp
    timestamp = int(time.time())
    # We force the extension to .wav since that is what Android sends
    saved_filename = f"{SAVE_DIR}/cmd_{timestamp}.wav"
    
    # 2. Save the file PERMANENTLY to the disk
    with open(saved_filename, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    
    print(f"    Saved to: {saved_filename}")

    try:
        # 3. Transcribe the SAVED file
        # fp16=False is needed if you are running on CPU to avoid warnings
        result = whisper_model.transcribe(saved_filename, fp16=False, language="en")
        text = result["text"]

        print(f"--> Recognized: {text}")
        return {"transcription": text}

    except Exception as e:
        print(f"--> Error: {e}")
        return {"transcription": "Error processing audio"}

if __name__ == "__main__":
    # host="0.0.0.0" allows external devices (like your phone) to connect
    uvicorn.run(app, host="0.0.0.0", port=8000)
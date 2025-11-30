import whisper
from fastapi import FastAPI, UploadFile, File
import uvicorn
import tempfile

# Load whisper model once
whisper_model = whisper.load_model("base")

app = FastAPI()

@app.post("/transcribe")
async def transcribe_audio(file: UploadFile = File(...)):
    # Save the uploaded file temporarily
    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    # Transcribe
    result = whisper_model.transcribe(tmp_path, fp16=False, language="en")
    text = result["text"]

    return {"transcription": text}

if __name__ == "__main__":
    uvicorn.run("api:app", host="0.0.0.0", port=8000)

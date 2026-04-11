from speechbrain.inference.speaker import SpeakerRecognition
import numpy as np

# Load model once at startup
spkrec = SpeakerRecognition.from_hparams(
    source="speechbrain/spkrec-ecapa-voxceleb"
)

def get_embedding(wav_path: str) -> bytes:
    """
    Takes a path to a .wav file.
    Returns the voice embedding as raw bytes (BLOB) ready to store in PostgreSQL.
    """
    embedding = spkrec.encode_file(wav_path)
    embedding_np = embedding.squeeze().detach().cpu().numpy()
    return embedding_np.tobytes()
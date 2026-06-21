import os
import logging
import base64
import numpy as np
import torch
import whisper
from dotenv import load_dotenv
from cryptography.fernet import Fernet

load_dotenv()

logger = logging.getLogger(__name__)

# Global variables for lazy loading
_feature_extractor = None
_wavlm_model = None

# Hardcoded fallback key for local development. DO NOT use in production!
DEV_FALLBACK_KEY = b"ojDJGTK1-9N_RwbRktUXapR4oIdUl_t-7haFnzCeur0="

def get_fernet_key() -> bytes:
    """
    Get the Fernet encryption key from the environment.
    If not configured, log a loud warning and fall back to the developer key.
    """
    secret = os.environ.get("VOICE_EMBEDDING_SECRET")
    if secret:
        try:
            # Validate key is a valid Fernet key
            key_bytes = secret.encode("utf-8")
            Fernet(key_bytes)
            return key_bytes
        except Exception:
            logger.exception("VOICE_EMBEDDING_SECRET in .env is not a valid 32-byte base64 Fernet key!")
    
    logger.warning(
        "\n============================================================\n"
        "WARNING: VOICE_EMBEDDING_SECRET is not configured or invalid in .env!\n"
        "Falling back to an insecure developer-only key.\n"
        "Please generate a secure key in production using:\n"
        "  from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())\n"
        "============================================================"
    )
    return DEV_FALLBACK_KEY

def get_wavlm_model():
    """
    Lazy load the WavLM model and feature extractor to speed up server startup.
    """
    global _feature_extractor, _wavlm_model
    if _wavlm_model is None:
        try:
            from transformers import AutoFeatureExtractor, WavLMForXVector
            model_id = "microsoft/wavlm-base-plus-sv"
            logger.info("Loading WavLM model for speaker verification...")
            _feature_extractor = AutoFeatureExtractor.from_pretrained(model_id)
            _wavlm_model = WavLMForXVector.from_pretrained(model_id)
            _wavlm_model.eval()
            logger.info("WavLM model loaded successfully.")
        except Exception as e:
            logger.exception("Failed to load WavLM model: %s", e)
            raise RuntimeError(f"WavLM model failed to load: {e}")
    return _feature_extractor, _wavlm_model

def extract_voice_embedding(audio_path: str) -> np.ndarray:
    """
    Extract a normalized 512-dimensional speaker embedding from the audio file.
    Resamples automatically to 16kHz using whisper.load_audio.
    """
    # Load audio (resampled to 16kHz mono float32 array)
    audio = whisper.load_audio(audio_path)
    
    # Get lazy-loaded model & feature extractor
    feature_extractor, model = get_wavlm_model()
    
    # Extract features
    inputs = feature_extractor(audio, return_tensors="pt", sampling_rate=16000)
    
    # Extract X-Vector embedding
    with torch.no_grad():
        outputs = model(**inputs)
        embeddings = outputs.embeddings.cpu().numpy()[0] # Shape: (512,)
        
        # Unit-normalize embedding vector
        norm = np.linalg.norm(embeddings)
        if norm > 0:
            embeddings = embeddings / norm
        return embeddings

def average_embeddings(embeddings_list: list[np.ndarray]) -> np.ndarray:
    """
    Averages a list of embedding vectors and unit-normalizes the result.
    """
    if not embeddings_list:
        raise ValueError("Cannot average an empty list of embeddings.")
    
    averaged = np.mean(embeddings_list, axis=0)
    norm = np.linalg.norm(averaged)
    if norm > 0:
        averaged = averaged / norm
    return averaged

def encrypt_embedding(embedding: np.ndarray) -> bytes:
    """
    Encrypt a numpy array embedding to bytes using Fernet.
    """
    # Serialize float32 array to raw bytes
    raw_bytes = embedding.astype(np.float32).tobytes()
    # Encrypt raw bytes
    key = get_fernet_key()
    f = Fernet(key)
    return f.encrypt(raw_bytes)

def decrypt_embedding(encrypted_bytes: bytes) -> np.ndarray:
    """
    Decrypt bytes back to a float32 numpy array embedding.
    """
    key = get_fernet_key()
    f = Fernet(key)
    raw_bytes = f.decrypt(encrypted_bytes)
    # Deserialize back to float32 numpy array
    return np.frombuffer(raw_bytes, dtype=np.float32)

def verify_voice(audio_path: str, stored_encrypted_embedding: bytes, threshold: float = 0.75) -> tuple[bool, float]:
    """
    Extract embedding from incoming audio file, decrypt stored embedding,
    and compare their cosine similarity.
    Returns (is_verified, score).
    """
    try:
        incoming_emb = extract_voice_embedding(audio_path)
        stored_emb = decrypt_embedding(stored_encrypted_embedding)
        
        # Cosine similarity is the dot product of two unit-normalized vectors
        score = float(np.dot(incoming_emb, stored_emb))
        is_verified = score >= threshold
        
        logger.info("Voice verification: score=%.4f (threshold=%.4f) verified=%s", score, threshold, is_verified)
        return is_verified, score
    except Exception as e:
        logger.exception("Error in verify_voice: %s", e)
        return False, 0.0

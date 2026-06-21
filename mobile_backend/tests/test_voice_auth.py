import sys
import os
import unittest
import numpy as np

# Set python path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.services.voice_auth import (
    average_embeddings,
    encrypt_embedding,
    decrypt_embedding,
    DEV_FALLBACK_KEY,
    get_fernet_key
)

class TestVoiceAuth(unittest.TestCase):

    def setUp(self):
        # Create dummy 512-dim normalized embedding vectors
        v1 = np.random.randn(512)
        self.embedding1 = v1 / np.linalg.norm(v1)

        v2 = np.random.randn(512)
        self.embedding2 = v2 / np.linalg.norm(v2)

    def test_fernet_key_retrieval(self):
        # Validate key retrieval returns a valid Fernet key
        key = get_fernet_key()
        self.assertIsInstance(key, bytes)
        self.assertEqual(len(key), 44) # 32 bytes URL-safe base64 is 44 characters

    def test_encryption_decryption(self):
        # Encrypt the embedding vector
        encrypted = encrypt_embedding(self.embedding1)
        self.assertIsInstance(encrypted, bytes)
        self.assertNotEqual(encrypted, self.embedding1.tobytes())

        # Decrypt the embedding vector
        decrypted = decrypt_embedding(encrypted)
        self.assertIsInstance(decrypted, np.ndarray)
        self.assertEqual(decrypted.shape, (512,))
        
        # Check values are preserved within a tiny margin of error (float32 precision)
        np.testing.assert_allclose(decrypted, self.embedding1, rtol=1e-5, atol=1e-5)

    def test_averaging_embeddings(self):
        # Average multiple embeddings
        embeddings_list = [self.embedding1, self.embedding2]
        averaged = average_embeddings(embeddings_list)

        self.assertEqual(averaged.shape, (512,))
        
        # Check that averaged embedding is unit-normalized (norm = 1.0)
        norm = np.linalg.norm(averaged)
        self.assertAlmostEqual(norm, 1.0, places=6)

        # Check that averaging mathematical direction is correct
        expected_raw_avg = (self.embedding1 + self.embedding2) / 2
        expected_normalized = expected_raw_avg / np.linalg.norm(expected_raw_avg)
        np.testing.assert_allclose(averaged, expected_normalized, rtol=1e-6)

    def test_cosine_similarity(self):
        # The dot product of two unit-normalized vectors is exactly their cosine similarity
        similarity = float(np.dot(self.embedding1, self.embedding1))
        # Self-similarity should be 1.0
        self.assertAlmostEqual(similarity, 1.0, places=5)

        # Cross-similarity of random vectors should be less than 1.0
        similarity_cross = float(np.dot(self.embedding1, self.embedding2))
        self.assertLess(similarity_cross, 0.99)

if __name__ == "__main__":
    unittest.main()

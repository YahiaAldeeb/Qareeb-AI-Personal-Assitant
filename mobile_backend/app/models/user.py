from sqlite3 import Blob

from pydantic import BaseModel

class LoginRequest(BaseModel):
    email: str
    password: str

class RegisterRequest(BaseModel):
    name: str
    email: str
    password: str
    phoneNumber: str
    



class RegisterVoiceRequest(BaseModel):
        userID: str
        wav_path: str
       

from sqlalchemy import Column, String, Text, BigInteger, Float
from app.database import Base

class PromptRecord(Base):
    __tablename__ = "Prompts"   # ✅ REQUIRED
    prompt_id     = Column(BigInteger, primary_key=True, autoincrement=True)
    userId       = Column(String, nullable=False)
    user_message  = Column(Text, nullable=False)
    qareeb_response = Column(Text, nullable=False)
    prompt_type   = Column(String, default="TEXT")
    module        = Column(String, default="CHATBOT")
    intent_detected = Column(String, nullable=True)
    created_at    = Column(BigInteger, nullable=False)
    metadata_ = Column("metadata", String, nullable=True)
class MemoryRecord(Base):
    __tablename__ = "Memory"   # ✅ REQUIRED
    memory_id  = Column(BigInteger, primary_key=True, autoincrement=True)
    userId     = Column(String, nullable=False)
    fact       = Column(Text, nullable=False)
    created_at = Column(BigInteger, nullable=False)
from sqlmodel import SQLModel, Field, Relationship
from datetime import datetime
from typing import Optional, List
from enum import Enum


class UserBase(SQLModel):
    """Base user model"""

    username: str = Field(index=True, unique=True)
    email: str = Field(index=True, unique=True)
    phone_number: Optional[str] = None
    is_active: bool = True


class User(UserBase, table=True):
    """User database model"""

    __tablename__ = "users"

    id: Optional[int] = Field(default=None, primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    # Relationships
    preferences: List["UserPreference"] = Relationship(back_populates="user")
    conversation_logs: List["ConversationLog"] = Relationship(back_populates="user")
    call_logs: List["CallLog"] = Relationship(back_populates="user")
    sms_logs: List["SMSLog"] = Relationship(back_populates="user")


class UserPreferenceBase(SQLModel):
    """Base user preference model"""

    user_id: int = Field(foreign_key="users.id")
    language: str = "en"
    tts_voice: str = "nova"
    auto_reply_enabled: bool = False
    conversation_timeout: int = 300  # seconds
    notification_email: Optional[str] = None


class UserPreference(UserPreferenceBase, table=True):
    """User preferences database model"""

    __tablename__ = "user_preferences"

    id: Optional[int] = Field(default=None, primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    # Relationships
    user: Optional[User] = Relationship(back_populates="preferences")


class ConversationLogBase(SQLModel):
    """Base conversation log model"""

    user_id: int = Field(foreign_key="users.id")
    input_text: str
    gpt_response: str
    input_tokens: int = 0
    output_tokens: int = 0
    processing_time_ms: float = 0.0
    model_used: str = "gpt-3.5-turbo"


class ConversationLog(ConversationLogBase, table=True):
    """Conversation log database model"""

    __tablename__ = "conversation_logs"

    id: Optional[int] = Field(default=None, primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)

    # Relationships
    user: Optional[User] = Relationship(back_populates="conversation_logs")


class CallLogBase(SQLModel):
    """Base call log model"""

    user_id: int = Field(foreign_key="users.id")
    call_duration_seconds: float
    success: bool
    error_message: Optional[str] = None


class CallLog(CallLogBase, table=True):
    """Call log database model"""

    __tablename__ = "call_logs"

    id: Optional[int] = Field(default=None, primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)

    # Relationships
    user: Optional[User] = Relationship(back_populates="call_logs")


class SMSDecisionEnum(str, Enum):
    """Enum for SMS decision"""

    YES = "yes"
    NO = "no"


class SMSLogBase(SQLModel):
    """Base SMS log model"""

    user_id: int = Field(foreign_key="users.id")
    incoming_text: str
    decision: SMSDecisionEnum
    reply_text: str


class SMSLog(SMSLogBase, table=True):
    """SMS log database model"""

    __tablename__ = "sms_logs"

    id: Optional[int] = Field(default=None, primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)

    # Relationships
    user: Optional[User] = Relationship(back_populates="sms_logs")

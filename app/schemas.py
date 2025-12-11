from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional
from enum import Enum


# User Schemas
class UserCreate(BaseModel):
    """Schema for creating a user"""

    username: str = Field(..., min_length=1, max_length=100)
    email: str = Field(..., min_length=1)
    phone_number: Optional[str] = None


class UserUpdate(BaseModel):
    """Schema for updating a user"""

    email: Optional[str] = None
    phone_number: Optional[str] = None
    is_active: Optional[bool] = None


class UserResponse(BaseModel):
    """Schema for user response"""

    id: int
    username: str
    email: str
    phone_number: Optional[str] = None
    is_active: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


# User Preference Schemas
class UserPreferenceCreate(BaseModel):
    """Schema for creating user preferences"""

    language: str = "en"
    tts_voice: str = "nova"
    auto_reply_enabled: bool = False
    conversation_timeout: int = 300
    notification_email: Optional[str] = None


class UserPreferenceUpdate(BaseModel):
    """Schema for updating user preferences"""

    language: Optional[str] = None
    tts_voice: Optional[str] = None
    auto_reply_enabled: Optional[bool] = None
    conversation_timeout: Optional[int] = None
    notification_email: Optional[str] = None


class UserPreferenceResponse(BaseModel):
    """Schema for user preference response"""

    id: int
    user_id: int
    language: str
    tts_voice: str
    auto_reply_enabled: bool
    conversation_timeout: int
    notification_email: Optional[str]
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


# Conversation Schemas
class ConversationRequest(BaseModel):
    """Schema for conversation endpoint"""

    user_id: int
    text: str = Field(..., min_length=1, max_length=5000)


class ConversationResponse(BaseModel):
    """Schema for conversation response"""

    id: int
    user_id: int
    input_text: str
    gpt_response: str
    input_tokens: int
    output_tokens: int
    processing_time_ms: float
    model_used: str
    created_at: datetime

    class Config:
        from_attributes = True


# SMS Decision Schemas
class SMSDecisionEnum(str, Enum):
    """Enum for SMS decision"""

    YES = "yes"
    NO = "no"


class SMSDecisionRequest(BaseModel):
    """Schema for SMS decision endpoint"""

    user_id: int
    text: str = Field(..., min_length=1, max_length=1000)


class SMSDecisionResponse(BaseModel):
    """Schema for SMS decision response"""

    id: int
    user_id: int
    incoming_text: str
    decision: SMSDecisionEnum
    reply_text: str
    created_at: datetime

    class Config:
        from_attributes = True


# Call Log Schemas
class CallLogResponse(BaseModel):
    """Schema for call log response"""

    id: int
    user_id: int
    call_duration_seconds: float
    success: bool
    error_message: Optional[str]
    created_at: datetime

    class Config:
        from_attributes = True


# Conversation Log Schemas
class ConversationLogResponse(BaseModel):
    """Schema for conversation log response"""

    id: int
    user_id: int
    input_text: str
    gpt_response: str
    input_tokens: int
    output_tokens: int
    processing_time_ms: float
    model_used: str
    created_at: datetime

    class Config:
        from_attributes = True


# History Response Schemas
class HistoryResponse(BaseModel):
    """Schema for history response"""

    conversation_logs: list[ConversationLogResponse]
    call_logs: list[CallLogResponse]
    sms_logs: list[SMSDecisionResponse]


# Error Response Schema
class ErrorResponse(BaseModel):
    """Schema for error response"""

    detail: str
    status_code: int


# Health Check Schema
class HealthCheckResponse(BaseModel):
    """Schema for health check response"""

    status: str
    timestamp: datetime

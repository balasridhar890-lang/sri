import pytest
from datetime import datetime

from app.schemas import (
    UserCreate,
    UserResponse,
    UserPreferenceCreate,
    ConversationRequest,
    SMSDecisionRequest,
)


class TestUserSchemas:
    """Test user schemas"""

    def test_user_create_valid(self):
        """Test valid user creation schema"""
        user = UserCreate(
            username="testuser",
            email="test@example.com",
            phone_number="+1234567890",
        )
        assert user.username == "testuser"
        assert user.email == "test@example.com"
        assert user.phone_number == "+1234567890"

    def test_user_create_minimal(self):
        """Test minimal user creation"""
        user = UserCreate(
            username="testuser",
            email="test@example.com",
        )
        assert user.username == "testuser"
        assert user.email == "test@example.com"
        assert user.phone_number is None

    def test_user_create_empty_username_fails(self):
        """Test that empty username fails validation"""
        with pytest.raises(ValueError):
            UserCreate(
                username="",
                email="test@example.com",
            )


class TestPreferenceSchemas:
    """Test preference schemas"""

    def test_preference_create_defaults(self):
        """Test preference creation with defaults"""
        pref = UserPreferenceCreate()
        assert pref.language == "en"
        assert pref.tts_voice == "nova"
        assert pref.auto_reply_enabled is False
        assert pref.conversation_timeout == 300

    def test_preference_create_custom(self):
        """Test preference creation with custom values"""
        pref = UserPreferenceCreate(
            language="es",
            tts_voice="alloy",
            auto_reply_enabled=True,
            conversation_timeout=600,
        )
        assert pref.language == "es"
        assert pref.tts_voice == "alloy"
        assert pref.auto_reply_enabled is True
        assert pref.conversation_timeout == 600


class TestConversationSchemas:
    """Test conversation schemas"""

    def test_conversation_request_valid(self):
        """Test valid conversation request"""
        req = ConversationRequest(
            user_id=1,
            text="Hello, how are you?",
        )
        assert req.user_id == 1
        assert req.text == "Hello, how are you?"

    def test_conversation_request_empty_text_fails(self):
        """Test that empty text fails validation"""
        with pytest.raises(ValueError):
            ConversationRequest(
                user_id=1,
                text="",
            )

    def test_conversation_request_long_text(self):
        """Test conversation request with long text"""
        long_text = "a" * 5000
        req = ConversationRequest(
            user_id=1,
            text=long_text,
        )
        assert len(req.text) == 5000

    def test_conversation_request_too_long_text_fails(self):
        """Test that text exceeding max length fails"""
        long_text = "a" * 5001
        with pytest.raises(ValueError):
            ConversationRequest(
                user_id=1,
                text=long_text,
            )


class TestSMSSchemas:
    """Test SMS schemas"""

    def test_sms_request_valid(self):
        """Test valid SMS request"""
        req = SMSDecisionRequest(
            user_id=1,
            text="Are you available tomorrow?",
        )
        assert req.user_id == 1
        assert req.text == "Are you available tomorrow?"

    def test_sms_request_empty_text_fails(self):
        """Test that empty text fails validation"""
        with pytest.raises(ValueError):
            SMSDecisionRequest(
                user_id=1,
                text="",
            )

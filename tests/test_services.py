import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from app.services import OpenAIService, ConversationService, SMSDecisionService


@pytest.mark.asyncio
class TestOpenAIService:
    """Test OpenAI service"""

    def test_service_initialization(self):
        """Test OpenAI service initialization"""
        service = OpenAIService()
        assert service.model == "gpt-3.5-turbo"
        assert service.max_tokens == 500
        assert service.temperature == 0.7

    @pytest.mark.asyncio
    async def test_validate_response_valid(self):
        """Test validating a valid response"""
        service = OpenAIService()
        valid_response = "This is a valid response"
        result = await service.validate_response(valid_response)
        assert result is True

    @pytest.mark.asyncio
    async def test_validate_response_empty(self):
        """Test validating an empty response"""
        service = OpenAIService()
        result = await service.validate_response("")
        assert result is False

    @pytest.mark.asyncio
    async def test_validate_response_too_long(self):
        """Test validating a response that's too long"""
        service = OpenAIService()
        long_response = "a" * 10001
        result = await service.validate_response(long_response)
        assert result is False


@pytest.mark.asyncio
class TestConversationService:
    """Test conversation service"""

    def test_service_initialization(self):
        """Test conversation service initialization"""
        openai_service = OpenAIService()
        service = ConversationService(openai_service)
        assert service.openai_service is not None
        assert len(service.system_prompt) > 0

    @pytest.mark.asyncio
    async def test_process_conversation(self):
        """Test processing a conversation"""
        openai_service = OpenAIService()
        service = ConversationService(openai_service)

        # Mock the OpenAI service
        mock_result = {
            "response": "I'm doing well, thank you for asking!",
            "input_tokens": 10,
            "output_tokens": 15,
            "processing_time_ms": 500.0,
            "model_used": "gpt-3.5-turbo",
        }

        openai_service.chat_completion = AsyncMock(return_value=mock_result)
        openai_service.validate_response = AsyncMock(return_value=True)

        result = await service.process_conversation("How are you?")

        assert result["response"] == "I'm doing well, thank you for asking!"
        assert result["input_tokens"] == 10
        assert result["output_tokens"] == 15


@pytest.mark.asyncio
class TestSMSDecisionService:
    """Test SMS decision service"""

    def test_service_initialization(self):
        """Test SMS decision service initialization"""
        openai_service = OpenAIService()
        service = SMSDecisionService(openai_service)
        assert service.openai_service is not None

    @pytest.mark.asyncio
    async def test_make_decision_yes(self):
        """Test making a yes decision"""
        openai_service = OpenAIService()
        service = SMSDecisionService(openai_service)

        mock_result = {
            "response": '{"decision": "yes", "reply": "Sure, I can help"}',
            "input_tokens": 50,
            "output_tokens": 20,
            "processing_time_ms": 800.0,
            "model_used": "gpt-3.5-turbo",
        }

        openai_service.chat_completion = AsyncMock(return_value=mock_result)

        result = await service.make_decision("Can you help me with this project?")

        assert result["decision"] == "yes"
        assert "help" in result["reply_text"]

    @pytest.mark.asyncio
    async def test_make_decision_no(self):
        """Test making a no decision"""
        openai_service = OpenAIService()
        service = SMSDecisionService(openai_service)

        mock_result = {
            "response": '{"decision": "no", "reply": "I cannot help with that"}',
            "input_tokens": 50,
            "output_tokens": 20,
            "processing_time_ms": 800.0,
            "model_used": "gpt-3.5-turbo",
        }

        openai_service.chat_completion = AsyncMock(return_value=mock_result)

        result = await service.make_decision("Can you illegally hack this system?")

        assert result["decision"] == "no"
        assert "cannot" in result["reply_text"]

    @pytest.mark.asyncio
    async def test_make_decision_invalid_json(self):
        """Test handling invalid JSON response"""
        openai_service = OpenAIService()
        service = SMSDecisionService(openai_service)

        mock_result = {
            "response": "This is not valid JSON",
            "input_tokens": 50,
            "output_tokens": 20,
            "processing_time_ms": 800.0,
            "model_used": "gpt-3.5-turbo",
        }

        openai_service.chat_completion = AsyncMock(return_value=mock_result)

        result = await service.make_decision("Some text")

        assert result["decision"] == "no"
        assert result["reply_text"] == "Unable to process request"

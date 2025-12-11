import logging
import asyncio
import time
from typing import Optional
from openai import AsyncOpenAI, RateLimitError, APIError
from tenacity import (
    retry,
    stop_after_attempt,
    wait_exponential,
    retry_if_exception_type,
)
from app.config import settings

logger = logging.getLogger(__name__)


class OpenAIService:
    """Service for OpenAI API interactions"""

    def __init__(self):
        self.client = AsyncOpenAI(api_key=settings.openai_api_key)
        self.model = settings.openai_model
        self.max_tokens = settings.openai_max_tokens
        self.temperature = settings.openai_temperature
        self.timeout = settings.openai_timeout
        self.max_retries = settings.max_retries
        self.retry_delay = settings.retry_delay

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((RateLimitError, APIError)),
        reraise=True,
    )
    async def chat_completion(
        self,
        text: str,
        system_prompt: Optional[str] = None,
    ) -> dict:
        """
        Get chat completion from OpenAI with error handling and retries

        Args:
            text: Input text for the AI
            system_prompt: Optional system prompt to guide the AI

        Returns:
            Dictionary with response, tokens used, and processing time

        Raises:
            APIError: If OpenAI API call fails after retries
        """
        start_time = time.time()

        try:
            messages = []

            if system_prompt:
                messages.append({"role": "system", "content": system_prompt})

            messages.append({"role": "user", "content": text})

            response = await asyncio.wait_for(
                self.client.chat.completions.create(
                    model=self.model,
                    messages=messages,
                    max_tokens=self.max_tokens,
                    temperature=self.temperature,
                ),
                timeout=self.timeout,
            )

            processing_time = (time.time() - start_time) * 1000

            logger.info(
                f"OpenAI request successful: {response.usage.prompt_tokens} input tokens, "
                f"{response.usage.completion_tokens} output tokens"
            )

            return {
                "response": response.choices[0].message.content,
                "input_tokens": response.usage.prompt_tokens,
                "output_tokens": response.usage.completion_tokens,
                "processing_time_ms": processing_time,
                "model_used": self.model,
            }

        except asyncio.TimeoutError:
            logger.error(f"OpenAI request timeout after {self.timeout}s")
            raise APIError("OpenAI request timeout")
        except RateLimitError as e:
            logger.warning(f"OpenAI rate limit: {str(e)}")
            raise
        except APIError as e:
            logger.error(f"OpenAI API error: {str(e)}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error in OpenAI request: {str(e)}")
            raise APIError(f"Unexpected error: {str(e)}")

    async def validate_response(self, response: str) -> bool:
        """
        Validate that a response from OpenAI is reasonable

        Args:
            response: The response text to validate

        Returns:
            True if response is valid, False otherwise
        """
        return bool(response and len(response) > 0 and len(response) < 10000)

    async def close(self) -> None:
        """Close the OpenAI client"""
        await self.client.close()


class ConversationService:
    """Service for handling conversation logic"""

    def __init__(self, openai_service: OpenAIService):
        self.openai_service = openai_service
        self.system_prompt = (
            "You are a helpful, concise, and professional assistant. "
            "Provide clear and direct responses. Keep responses under 500 tokens."
        )

    async def process_conversation(self, text: str) -> dict:
        """
        Process a conversation and get AI response

        Args:
            text: Input text from user

        Returns:
            Dictionary with AI response and metadata
        """
        logger.info(f"Processing conversation: {text[:50]}...")

        result = await self.openai_service.chat_completion(
            text=text,
            system_prompt=self.system_prompt,
        )

        if not await self.openai_service.validate_response(result["response"]):
            raise ValueError("Invalid response from OpenAI")

        logger.info(f"Conversation processed successfully in {result['processing_time_ms']:.2f}ms")

        return result


class SMSDecisionService:
    """Service for SMS decision logic"""

    def __init__(self, openai_service: OpenAIService):
        self.openai_service = openai_service

    async def make_decision(self, text: str) -> dict:
        """
        Make a yes/no decision based on SMS text

        Args:
            text: SMS text to analyze

        Returns:
            Dictionary with decision (yes/no) and reply text
        """
        logger.info(f"Processing SMS decision for: {text[:50]}...")

        prompt = f"""You must respond with a JSON object containing:
1. "decision": "yes" or "no"
2. "reply": a brief reply message (max 50 words)

SMS text: {text}

For this SMS, should the user respond positively? Respond with valid JSON only."""

        result = await self.openai_service.chat_completion(
            text=prompt,
            system_prompt="You are an SMS assistant that makes quick yes/no decisions and suggests replies.",
        )

        try:
            import json

            response_json = json.loads(result["response"])
            decision = response_json.get("decision", "no").lower()
            reply = response_json.get("reply", "")

            if decision not in ["yes", "no"]:
                decision = "no"

            logger.info(f"SMS decision made: {decision}")

            return {
                "decision": decision,
                "reply_text": reply,
                "processing_time_ms": result["processing_time_ms"],
            }

        except (json.JSONDecodeError, KeyError) as e:
            logger.error(f"Error parsing SMS decision response: {str(e)}")
            return {
                "decision": "no",
                "reply_text": "Unable to process request",
                "processing_time_ms": result["processing_time_ms"],
            }

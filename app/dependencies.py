from typing import AsyncGenerator
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import async_session_maker
from app.services import OpenAIService, ConversationService, SMSDecisionService
from app.repositories import (
    UserRepository,
    UserPreferenceRepository,
    ConversationLogRepository,
    CallLogRepository,
    SMSLogRepository,
)

# Cache for service instances
_openai_service: OpenAIService | None = None


def get_openai_service() -> OpenAIService:
    """Get or create OpenAI service instance (singleton)"""
    global _openai_service
    if _openai_service is None:
        _openai_service = OpenAIService()
    return _openai_service


async def get_session() -> AsyncGenerator[AsyncSession, None]:
    """Get database session for dependency injection"""
    async with async_session_maker() as session:
        yield session


async def get_user_repository(
    session: AsyncSession = None,
) -> UserRepository:
    """Get user repository"""
    if session is None:
        async with async_session_maker() as session:
            return UserRepository(session)
    return UserRepository(session)


async def get_user_preference_repository(
    session: AsyncSession = None,
) -> UserPreferenceRepository:
    """Get user preference repository"""
    if session is None:
        async with async_session_maker() as session:
            return UserPreferenceRepository(session)
    return UserPreferenceRepository(session)


async def get_conversation_log_repository(
    session: AsyncSession = None,
) -> ConversationLogRepository:
    """Get conversation log repository"""
    if session is None:
        async with async_session_maker() as session:
            return ConversationLogRepository(session)
    return ConversationLogRepository(session)


async def get_call_log_repository(
    session: AsyncSession = None,
) -> CallLogRepository:
    """Get call log repository"""
    if session is None:
        async with async_session_maker() as session:
            return CallLogRepository(session)
    return CallLogRepository(session)


async def get_sms_log_repository(
    session: AsyncSession = None,
) -> SMSLogRepository:
    """Get SMS log repository"""
    if session is None:
        async with async_session_maker() as session:
            return SMSLogRepository(session)
    return SMSLogRepository(session)


def get_conversation_service() -> ConversationService:
    """Get conversation service"""
    return ConversationService(get_openai_service())


def get_sms_decision_service() -> SMSDecisionService:
    """Get SMS decision service"""
    return SMSDecisionService(get_openai_service())

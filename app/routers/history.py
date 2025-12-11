from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import get_session
from app.repositories import (
    UserRepository,
    ConversationLogRepository,
    CallLogRepository,
    SMSLogRepository,
)
from app.schemas import (
    ConversationLogResponse,
    CallLogResponse,
    SMSDecisionResponse,
    HistoryResponse,
)
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/history", tags=["history"])


@router.get("/{user_id}", response_model=HistoryResponse)
async def get_user_history(
    user_id: int,
    limit: int = 50,
    session: AsyncSession = Depends(get_session),
) -> HistoryResponse:
    """Get conversation, call, and SMS history for a user"""
    try:
        # Check if user exists
        user_repo = UserRepository(session)
        user = await user_repo.get(user_id)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found",
            )

        # Get all history
        conversation_repo = ConversationLogRepository(session)
        call_repo = CallLogRepository(session)
        sms_repo = SMSLogRepository(session)

        conversation_logs = await conversation_repo.get_by_user_id(
            user_id, limit=limit
        )
        call_logs = await call_repo.get_by_user_id(user_id, limit=limit)
        sms_logs = await sms_repo.get_by_user_id(user_id, limit=limit)

        logger.info(
            f"History retrieved for user {user_id}: "
            f"{len(conversation_logs)} conversations, "
            f"{len(call_logs)} calls, "
            f"{len(sms_logs)} SMS"
        )

        return HistoryResponse(
            conversation_logs=[
                ConversationLogResponse.model_validate(log)
                for log in conversation_logs
            ],
            call_logs=[CallLogResponse.model_validate(log) for log in call_logs],
            sms_logs=[SMSDecisionResponse.model_validate(log) for log in sms_logs],
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error retrieving history: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error retrieving history",
        )


@router.get("/{user_id}/conversations", response_model=list[ConversationLogResponse])
async def get_conversation_history(
    user_id: int,
    limit: int = 50,
    session: AsyncSession = Depends(get_session),
) -> list[ConversationLogResponse]:
    """Get conversation history for a user"""
    try:
        # Check if user exists
        user_repo = UserRepository(session)
        user = await user_repo.get(user_id)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found",
            )

        repo = ConversationLogRepository(session)
        logs = await repo.get_by_user_id(user_id, limit=limit)

        logger.info(f"Conversation history retrieved for user {user_id}")

        return [ConversationLogResponse.model_validate(log) for log in logs]

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error retrieving conversation history: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error retrieving conversation history",
        )


@router.get("/{user_id}/calls", response_model=list[CallLogResponse])
async def get_call_history(
    user_id: int,
    limit: int = 50,
    session: AsyncSession = Depends(get_session),
) -> list[CallLogResponse]:
    """Get call history for a user"""
    try:
        # Check if user exists
        user_repo = UserRepository(session)
        user = await user_repo.get(user_id)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found",
            )

        repo = CallLogRepository(session)
        logs = await repo.get_by_user_id(user_id, limit=limit)

        logger.info(f"Call history retrieved for user {user_id}")

        return [CallLogResponse.model_validate(log) for log in logs]

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error retrieving call history: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error retrieving call history",
        )


@router.get("/{user_id}/sms", response_model=list[SMSDecisionResponse])
async def get_sms_history(
    user_id: int,
    limit: int = 50,
    session: AsyncSession = Depends(get_session),
) -> list[SMSDecisionResponse]:
    """Get SMS history for a user"""
    try:
        # Check if user exists
        user_repo = UserRepository(session)
        user = await user_repo.get(user_id)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found",
            )

        repo = SMSLogRepository(session)
        logs = await repo.get_by_user_id(user_id, limit=limit)

        logger.info(f"SMS history retrieved for user {user_id}")

        return [SMSDecisionResponse.model_validate(log) for log in logs]

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error retrieving SMS history: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error retrieving SMS history",
        )

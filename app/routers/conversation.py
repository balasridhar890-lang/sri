from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from openai import APIError
from app.database import get_session
from app.repositories import ConversationLogRepository, UserRepository
from app.services import ConversationService
from app.dependencies import get_conversation_service
from app.models import ConversationLog
from app.schemas import ConversationRequest, ConversationResponse
import logging
import time

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/conversation", tags=["conversation"])


@router.post("/", response_model=ConversationResponse)
async def process_conversation(
    request: ConversationRequest,
    session: AsyncSession = Depends(get_session),
    service: ConversationService = Depends(get_conversation_service),
) -> ConversationResponse:
    """
    Process a conversation request and get AI response

    Returns:
        ConversationResponse with AI response and metadata
    """
    start_time = time.time()

    try:
        # Check if user exists
        user_repo = UserRepository(session)
        user = await user_repo.get(request.user_id)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found",
            )

        # Process conversation
        result = await service.process_conversation(request.text)

        # Check response time
        total_time = (time.time() - start_time) * 1000
        if total_time > 2000:
            logger.warning(f"Conversation processing took {total_time:.2f}ms (> 2s)")

        # Store conversation log
        conversation_log = ConversationLog(
            user_id=request.user_id,
            input_text=request.text,
            gpt_response=result["response"],
            input_tokens=result["input_tokens"],
            output_tokens=result["output_tokens"],
            processing_time_ms=result["processing_time_ms"],
            model_used=result["model_used"],
        )

        log_repo = ConversationLogRepository(session)
        db_log = await log_repo.create(conversation_log)

        logger.info(
            f"Conversation processed for user {request.user_id} in {total_time:.2f}ms"
        )

        return ConversationResponse.model_validate(db_log)

    except HTTPException:
        raise
    except APIError as e:
        logger.error(f"OpenAI API error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI service temporarily unavailable",
        )
    except Exception as e:
        logger.error(f"Error processing conversation: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error processing conversation",
        )

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from openai import APIError
from app.database import get_session
from app.repositories import SMSLogRepository, UserRepository
from app.services import SMSDecisionService
from app.dependencies import get_sms_decision_service
from app.models import SMSLog, SMSDecisionEnum
from app.schemas import SMSDecisionRequest, SMSDecisionResponse
import logging
import time

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/sms", tags=["sms"])


@router.post("/decision", response_model=SMSDecisionResponse)
async def make_sms_decision(
    request: SMSDecisionRequest,
    session: AsyncSession = Depends(get_session),
    service: SMSDecisionService = Depends(get_sms_decision_service),
) -> SMSDecisionResponse:
    """
    Make a yes/no decision for an SMS and generate a reply

    Returns:
        SMSDecisionResponse with decision and reply text
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

        # Process SMS decision
        result = await service.make_decision(request.text)

        # Check response time
        total_time = (time.time() - start_time) * 1000
        if total_time > 2000:
            logger.warning(f"SMS decision took {total_time:.2f}ms (> 2s)")

        # Store SMS log
        decision_enum = SMSDecisionEnum(result["decision"])
        sms_log = SMSLog(
            user_id=request.user_id,
            incoming_text=request.text,
            decision=decision_enum,
            reply_text=result["reply_text"],
        )

        log_repo = SMSLogRepository(session)
        db_log = await log_repo.create(sms_log)

        logger.info(
            f"SMS decision made for user {request.user_id}: {result['decision']} in {total_time:.2f}ms"
        )

        return SMSDecisionResponse.model_validate(db_log)

    except HTTPException:
        raise
    except APIError as e:
        logger.error(f"OpenAI API error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI service temporarily unavailable",
        )
    except Exception as e:
        logger.error(f"Error processing SMS decision: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error processing SMS decision",
        )

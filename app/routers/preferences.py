from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import get_session
from app.repositories import UserPreferenceRepository, UserRepository
from app.schemas import (
    UserPreferenceCreate,
    UserPreferenceUpdate,
    UserPreferenceResponse,
)
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/preferences", tags=["preferences"])


@router.post("/", response_model=UserPreferenceResponse, status_code=status.HTTP_201_CREATED)
async def create_preferences(
    user_id: int,
    preferences: UserPreferenceCreate,
    session: AsyncSession = Depends(get_session),
) -> UserPreferenceResponse:
    """Create user preferences"""
    try:
        # Check if user exists
        user_repo = UserRepository(session)
        user = await user_repo.get(user_id)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found",
            )

        # Check if preferences already exist
        pref_repo = UserPreferenceRepository(session)
        existing_prefs = await pref_repo.get_by_user_id(user_id)

        if existing_prefs:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Preferences already exist for this user",
            )

        db_prefs = await pref_repo.create(user_id, preferences)
        logger.info(f"Preferences created for user: {user_id}")
        return UserPreferenceResponse.model_validate(db_prefs)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error creating preferences: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error creating preferences",
        )


@router.get("/{user_id}", response_model=UserPreferenceResponse)
async def get_preferences(
    user_id: int,
    session: AsyncSession = Depends(get_session),
) -> UserPreferenceResponse:
    """Get preferences for a user"""
    try:
        repo = UserPreferenceRepository(session)
        db_prefs = await repo.get_by_user_id(user_id)

        if not db_prefs:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Preferences not found for this user",
            )

        return UserPreferenceResponse.model_validate(db_prefs)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting preferences: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error getting preferences",
        )


@router.put("/{preference_id}", response_model=UserPreferenceResponse)
async def update_preferences(
    preference_id: int,
    preferences: UserPreferenceUpdate,
    session: AsyncSession = Depends(get_session),
) -> UserPreferenceResponse:
    """Update user preferences"""
    try:
        repo = UserPreferenceRepository(session)
        db_prefs = await repo.update(preference_id, preferences)

        if not db_prefs:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Preferences not found",
            )

        logger.info(f"Preferences updated: {preference_id}")
        return UserPreferenceResponse.model_validate(db_prefs)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error updating preferences: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error updating preferences",
        )


@router.delete("/{preference_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_preferences(
    preference_id: int,
    session: AsyncSession = Depends(get_session),
) -> None:
    """Delete user preferences"""
    try:
        repo = UserPreferenceRepository(session)
        success = await repo.delete(preference_id)

        if not success:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Preferences not found",
            )

        logger.info(f"Preferences deleted: {preference_id}")

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error deleting preferences: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error deleting preferences",
        )

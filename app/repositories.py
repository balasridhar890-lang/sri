from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from sqlmodel import select
from app.models import (
    User,
    UserPreference,
    ConversationLog,
    CallLog,
    SMSLog,
)
from app.schemas import (
    UserCreate,
    UserUpdate,
    UserPreferenceCreate,
    UserPreferenceUpdate,
)
from typing import Optional, List


class UserRepository:
    """Repository for user operations"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, user: UserCreate) -> User:
        """Create a new user"""
        db_user = User(**user.model_dump())
        self.session.add(db_user)
        await self.session.commit()
        await self.session.refresh(db_user)
        return db_user

    async def get(self, user_id: int) -> Optional[User]:
        """Get user by ID"""
        result = await self.session.execute(select(User).where(User.id == user_id))
        return result.scalars().first()

    async def get_by_username(self, username: str) -> Optional[User]:
        """Get user by username"""
        result = await self.session.execute(
            select(User).where(User.username == username)
        )
        return result.scalars().first()

    async def get_by_email(self, email: str) -> Optional[User]:
        """Get user by email"""
        result = await self.session.execute(select(User).where(User.email == email))
        return result.scalars().first()

    async def list_all(self, skip: int = 0, limit: int = 10) -> List[User]:
        """List all users"""
        result = await self.session.execute(
            select(User).offset(skip).limit(limit)
        )
        return result.scalars().all()

    async def update(self, user_id: int, user: UserUpdate) -> Optional[User]:
        """Update user"""
        db_user = await self.get(user_id)
        if not db_user:
            return None

        update_data = user.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(db_user, field, value)

        self.session.add(db_user)
        await self.session.commit()
        await self.session.refresh(db_user)
        return db_user

    async def delete(self, user_id: int) -> bool:
        """Delete user"""
        db_user = await self.get(user_id)
        if not db_user:
            return False

        await self.session.delete(db_user)
        await self.session.commit()
        return True


class UserPreferenceRepository:
    """Repository for user preference operations"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, user_id: int, pref: UserPreferenceCreate) -> UserPreference:
        """Create user preferences"""
        db_pref = UserPreference(user_id=user_id, **pref.model_dump())
        self.session.add(db_pref)
        await self.session.commit()
        await self.session.refresh(db_pref)
        return db_pref

    async def get(self, preference_id: int) -> Optional[UserPreference]:
        """Get preferences by ID"""
        result = await self.session.execute(
            select(UserPreference).where(UserPreference.id == preference_id)
        )
        return result.scalars().first()

    async def get_by_user_id(self, user_id: int) -> Optional[UserPreference]:
        """Get preferences by user ID"""
        result = await self.session.execute(
            select(UserPreference).where(UserPreference.user_id == user_id)
        )
        return result.scalars().first()

    async def update(
        self, preference_id: int, pref: UserPreferenceUpdate
    ) -> Optional[UserPreference]:
        """Update preferences"""
        db_pref = await self.get(preference_id)
        if not db_pref:
            return None

        update_data = pref.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(db_pref, field, value)

        self.session.add(db_pref)
        await self.session.commit()
        await self.session.refresh(db_pref)
        return db_pref

    async def delete(self, preference_id: int) -> bool:
        """Delete preferences"""
        db_pref = await self.get(preference_id)
        if not db_pref:
            return False

        await self.session.delete(db_pref)
        await self.session.commit()
        return True


class ConversationLogRepository:
    """Repository for conversation log operations"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, log: ConversationLog) -> ConversationLog:
        """Create conversation log"""
        self.session.add(log)
        await self.session.commit()
        await self.session.refresh(log)
        return log

    async def get(self, log_id: int) -> Optional[ConversationLog]:
        """Get conversation log by ID"""
        result = await self.session.execute(
            select(ConversationLog).where(ConversationLog.id == log_id)
        )
        return result.scalars().first()

    async def get_by_user_id(
        self, user_id: int, limit: int = 50
    ) -> List[ConversationLog]:
        """Get conversation logs by user ID"""
        result = await self.session.execute(
            select(ConversationLog)
            .where(ConversationLog.user_id == user_id)
            .order_by(ConversationLog.created_at.desc())
            .limit(limit)
        )
        return result.scalars().all()


class CallLogRepository:
    """Repository for call log operations"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, log: CallLog) -> CallLog:
        """Create call log"""
        self.session.add(log)
        await self.session.commit()
        await self.session.refresh(log)
        return log

    async def get(self, log_id: int) -> Optional[CallLog]:
        """Get call log by ID"""
        result = await self.session.execute(
            select(CallLog).where(CallLog.id == log_id)
        )
        return result.scalars().first()

    async def get_by_user_id(self, user_id: int, limit: int = 50) -> List[CallLog]:
        """Get call logs by user ID"""
        result = await self.session.execute(
            select(CallLog)
            .where(CallLog.user_id == user_id)
            .order_by(CallLog.created_at.desc())
            .limit(limit)
        )
        return result.scalars().all()


class SMSLogRepository:
    """Repository for SMS log operations"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, log: SMSLog) -> SMSLog:
        """Create SMS log"""
        self.session.add(log)
        await self.session.commit()
        await self.session.refresh(log)
        return log

    async def get(self, log_id: int) -> Optional[SMSLog]:
        """Get SMS log by ID"""
        result = await self.session.execute(
            select(SMSLog).where(SMSLog.id == log_id)
        )
        return result.scalars().first()

    async def get_by_user_id(self, user_id: int, limit: int = 50) -> List[SMSLog]:
        """Get SMS logs by user ID"""
        result = await self.session.execute(
            select(SMSLog)
            .where(SMSLog.user_id == user_id)
            .order_by(SMSLog.created_at.desc())
            .limit(limit)
        )
        return result.scalars().all()

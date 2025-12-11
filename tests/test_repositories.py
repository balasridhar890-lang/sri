import pytest
from app.models import User, UserPreference
from app.repositories import UserRepository, UserPreferenceRepository
from app.schemas import UserCreate, UserPreferenceCreate, UserUpdate


@pytest.mark.asyncio
class TestUserRepository:
    """Test user repository"""

    async def test_create_user(self, test_session):
        """Test creating a user"""
        repo = UserRepository(test_session)
        user_create = UserCreate(
            username="testuser",
            email="test@example.com",
        )

        user = await repo.create(user_create)

        assert user.id is not None
        assert user.username == "testuser"
        assert user.email == "test@example.com"
        assert user.is_active is True

    async def test_get_user(self, test_session):
        """Test getting a user by ID"""
        repo = UserRepository(test_session)
        user_create = UserCreate(
            username="testuser",
            email="test@example.com",
        )
        created_user = await repo.create(user_create)

        fetched_user = await repo.get(created_user.id)

        assert fetched_user is not None
        assert fetched_user.username == "testuser"

    async def test_get_nonexistent_user(self, test_session):
        """Test getting a nonexistent user returns None"""
        repo = UserRepository(test_session)
        user = await repo.get(999)
        assert user is None

    async def test_get_user_by_username(self, test_session):
        """Test getting a user by username"""
        repo = UserRepository(test_session)
        user_create = UserCreate(
            username="uniqueuser",
            email="unique@example.com",
        )
        await repo.create(user_create)

        user = await repo.get_by_username("uniqueuser")

        assert user is not None
        assert user.username == "uniqueuser"

    async def test_get_user_by_email(self, test_session):
        """Test getting a user by email"""
        repo = UserRepository(test_session)
        user_create = UserCreate(
            username="emailuser",
            email="emailtest@example.com",
        )
        await repo.create(user_create)

        user = await repo.get_by_email("emailtest@example.com")

        assert user is not None
        assert user.email == "emailtest@example.com"

    async def test_list_users(self, test_session):
        """Test listing users"""
        repo = UserRepository(test_session)

        # Create multiple users
        for i in range(3):
            user_create = UserCreate(
                username=f"user{i}",
                email=f"user{i}@example.com",
            )
            await repo.create(user_create)

        users = await repo.list_all(skip=0, limit=10)

        assert len(users) >= 3

    async def test_update_user(self, test_session):
        """Test updating a user"""
        repo = UserRepository(test_session)
        user_create = UserCreate(
            username="updateuser",
            email="update@example.com",
        )
        user = await repo.create(user_create)

        update = UserUpdate(
            email="newemail@example.com",
            is_active=False,
        )
        updated_user = await repo.update(user.id, update)

        assert updated_user is not None
        assert updated_user.email == "newemail@example.com"
        assert updated_user.is_active is False

    async def test_delete_user(self, test_session):
        """Test deleting a user"""
        repo = UserRepository(test_session)
        user_create = UserCreate(
            username="deleteuser",
            email="delete@example.com",
        )
        user = await repo.create(user_create)

        success = await repo.delete(user.id)

        assert success is True
        fetched_user = await repo.get(user.id)
        assert fetched_user is None


@pytest.mark.asyncio
class TestUserPreferenceRepository:
    """Test user preference repository"""

    async def test_create_preferences(self, test_session):
        """Test creating user preferences"""
        # First create a user
        user_repo = UserRepository(test_session)
        user_create = UserCreate(
            username="prefuser",
            email="pref@example.com",
        )
        user = await user_repo.create(user_create)

        # Create preferences
        pref_repo = UserPreferenceRepository(test_session)
        pref_create = UserPreferenceCreate(
            language="es",
            tts_voice="alloy",
        )
        pref = await pref_repo.create(user.id, pref_create)

        assert pref.id is not None
        assert pref.user_id == user.id
        assert pref.language == "es"
        assert pref.tts_voice == "alloy"

    async def test_get_preferences_by_user_id(self, test_session):
        """Test getting preferences by user ID"""
        # Create user and preferences
        user_repo = UserRepository(test_session)
        user_create = UserCreate(
            username="prefuser2",
            email="pref2@example.com",
        )
        user = await user_repo.create(user_create)

        pref_repo = UserPreferenceRepository(test_session)
        pref_create = UserPreferenceCreate()
        await pref_repo.create(user.id, pref_create)

        # Get preferences
        pref = await pref_repo.get_by_user_id(user.id)

        assert pref is not None
        assert pref.user_id == user.id

    async def test_update_preferences(self, test_session):
        """Test updating preferences"""
        # Setup
        user_repo = UserRepository(test_session)
        user_create = UserCreate(
            username="prefuser3",
            email="pref3@example.com",
        )
        user = await user_repo.create(user_create)

        pref_repo = UserPreferenceRepository(test_session)
        pref_create = UserPreferenceCreate()
        pref = await pref_repo.create(user.id, pref_create)

        # Update
        from app.schemas import UserPreferenceUpdate

        update = UserPreferenceUpdate(
            language="fr",
            auto_reply_enabled=True,
        )
        updated_pref = await pref_repo.update(pref.id, update)

        assert updated_pref is not None
        assert updated_pref.language == "fr"
        assert updated_pref.auto_reply_enabled is True

    async def test_delete_preferences(self, test_session):
        """Test deleting preferences"""
        # Setup
        user_repo = UserRepository(test_session)
        user_create = UserCreate(
            username="prefuser4",
            email="pref4@example.com",
        )
        user = await user_repo.create(user_create)

        pref_repo = UserPreferenceRepository(test_session)
        pref_create = UserPreferenceCreate()
        pref = await pref_repo.create(user.id, pref_create)

        # Delete
        success = await pref_repo.delete(pref.id)

        assert success is True
        deleted_pref = await pref_repo.get(pref.id)
        assert deleted_pref is None

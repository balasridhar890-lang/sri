import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.mark.asyncio
class TestHealthEndpoint:
    """Test health check endpoint"""

    def test_health_check(self, test_client):
        """Test health check returns healthy status"""
        response = test_client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "timestamp" in data


@pytest.mark.asyncio
class TestUserEndpoints:
    """Test user endpoints"""

    def test_create_user(self, test_client):
        """Test creating a user"""
        user_data = {
            "username": "testuser",
            "email": "test@example.com",
            "phone_number": "+1234567890",
        }
        response = test_client.post("/users/", json=user_data)
        assert response.status_code == 201
        data = response.json()
        assert data["username"] == "testuser"
        assert data["email"] == "test@example.com"
        assert data["id"] is not None

    def test_create_user_duplicate_username(self, test_client):
        """Test creating a user with duplicate username"""
        user_data = {
            "username": "duplicate",
            "email": "first@example.com",
        }
        test_client.post("/users/", json=user_data)

        # Try to create with same username
        user_data2 = {
            "username": "duplicate",
            "email": "second@example.com",
        }
        response = test_client.post("/users/", json=user_data2)
        assert response.status_code == 400

    def test_get_user(self, test_client):
        """Test getting a user"""
        # Create user first
        user_data = {
            "username": "getuser",
            "email": "getuser@example.com",
        }
        create_response = test_client.post("/users/", json=user_data)
        user_id = create_response.json()["id"]

        # Get user
        response = test_client.get(f"/users/{user_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["username"] == "getuser"

    def test_get_nonexistent_user(self, test_client):
        """Test getting a nonexistent user"""
        response = test_client.get("/users/999")
        assert response.status_code == 404

    def test_list_users(self, test_client):
        """Test listing users"""
        response = test_client.get("/users/")
        assert response.status_code == 200
        assert isinstance(response.json(), list)

    def test_update_user(self, test_client):
        """Test updating a user"""
        # Create user
        user_data = {
            "username": "updateuser",
            "email": "updateuser@example.com",
        }
        create_response = test_client.post("/users/", json=user_data)
        user_id = create_response.json()["id"]

        # Update user
        update_data = {
            "email": "newemail@example.com",
            "is_active": False,
        }
        response = test_client.put(f"/users/{user_id}", json=update_data)
        assert response.status_code == 200
        data = response.json()
        assert data["email"] == "newemail@example.com"
        assert data["is_active"] is False

    def test_delete_user(self, test_client):
        """Test deleting a user"""
        # Create user
        user_data = {
            "username": "deleteuser",
            "email": "deleteuser@example.com",
        }
        create_response = test_client.post("/users/", json=user_data)
        user_id = create_response.json()["id"]

        # Delete user
        response = test_client.delete(f"/users/{user_id}")
        assert response.status_code == 204

        # Verify user is deleted
        get_response = test_client.get(f"/users/{user_id}")
        assert get_response.status_code == 404


@pytest.mark.asyncio
class TestPreferenceEndpoints:
    """Test preference endpoints"""

    def test_create_preferences(self, test_client):
        """Test creating preferences"""
        # Create user first
        user_data = {
            "username": "prefuser",
            "email": "prefuser@example.com",
        }
        user_response = test_client.post("/users/", json=user_data)
        user_id = user_response.json()["id"]

        # Create preferences
        pref_data = {
            "language": "es",
            "tts_voice": "alloy",
            "auto_reply_enabled": True,
        }
        response = test_client.post(
            "/preferences/?user_id=" + str(user_id), json=pref_data
        )
        assert response.status_code == 201
        data = response.json()
        assert data["language"] == "es"
        assert data["tts_voice"] == "alloy"

    def test_get_preferences(self, test_client):
        """Test getting preferences"""
        # Setup
        user_data = {
            "username": "prefuser2",
            "email": "prefuser2@example.com",
        }
        user_response = test_client.post("/users/", json=user_data)
        user_id = user_response.json()["id"]

        pref_data = {
            "language": "fr",
            "tts_voice": "nova",
        }
        pref_response = test_client.post(
            "/preferences/?user_id=" + str(user_id), json=pref_data
        )
        pref_id = pref_response.json()["id"]

        # Get preferences
        response = test_client.get(f"/preferences/{user_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["language"] == "fr"

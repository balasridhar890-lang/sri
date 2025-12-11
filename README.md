# Voice Assistant - Complete System (Backend + Android)

A complete voice assistant system consisting of:
- **Backend**: Python FastAPI service with OpenAI GPT integration, SMS/call decision making, and history tracking
- **Android**: Kotlin mobile app with phone state management, auto-answer calls, intelligent SMS auto-reply, and backend synchronization

This is a full-stack implementation of AI-powered phone integration for both incoming calls and SMS messages.

## Features

### Backend (FastAPI)
- **FastAPI Framework**: Modern async Python web framework with automatic OpenAPI documentation
- **OpenAI Integration**: Robust GPT-3.5-turbo integration with error handling, retries, and timeouts
- **SQLModel Database**: SQLAlchemy ORM with Pydantic validation, async support, and migration-ready
- **Conversation Endpoint**: Process natural language conversations with AI responses and TTS metadata
- **SMS Decision Endpoint**: Make yes/no decisions and generate replies for SMS messages
- **User Management**: Create, read, update, delete users with preferences
- **History Tracking**: Complete conversation, call, and SMS logs for audit trails
- **Async/Await**: Non-blocking I/O for sub-2-second response times
- **Comprehensive Testing**: Unit tests for schemas, repositories, services, and endpoints
- **Production Ready**: CORS, error handling, logging, and security headers

### Android (Kotlin)
- **Phone State Management**: Monitor and intercept incoming/outgoing calls
- **Auto-Answer**: Automatically answer incoming calls and hand to voice assistant
- **Call Logging**: Local and remote logging of all call events
- **SMS Auto-Reply**: Intelligent workflow requesting backend decision and sending replies
- **SMS Logging**: Track all SMS events locally and sync with backend
- **Device Monitoring**: Monitor battery %, running apps, and contacts
- **Permission Management**: Comprehensive permission request flows with fallbacks
- **Local Storage**: Room database for offline call and SMS logs
- **Sync Service**: Periodic synchronization with backend
- **Hilt Dependency Injection**: Clean, testable architecture

## Project Structure

```
.
├── app/                        # Backend (FastAPI)
│   ├── __init__.py
│   ├── main.py                 # FastAPI app initialization
│   ├── config.py              # Configuration and settings
│   ├── database.py            # Database engine and session
│   ├── models.py              # SQLModel database models
│   ├── schemas.py             # Pydantic request/response schemas
│   ├── repositories.py        # Data access layer
│   ├── services.py            # Business logic (OpenAI, conversations, SMS)
│   ├── dependencies.py        # Dependency injection
│   └── routers/
│       ├── __init__.py
│       ├── health.py          # Health check endpoint
│       ├── users.py           # User management endpoints
│       ├── preferences.py     # User preferences CRUD
│       ├── conversation.py    # Conversation processing
│       ├── sms.py            # SMS decision making
│       └── history.py        # Conversation/call/SMS history
├── android/                    # Mobile App (Kotlin/Android)
│   ├── build.gradle.kts       # Android build configuration
│   ├── settings.gradle.kts    # Gradle settings
│   ├── proguard-rules.pro     # Proguard configuration
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/voiceassistant/android/
│   │       ├── MainActivity.kt              # Main entry point
│   │       ├── VoiceAssistantApp.kt         # Hilt application
│   │       ├── config/AppConfig.kt          # Configuration
│   │       ├── database/AppDatabase.kt      # Room database
│   │       ├── di/NetworkModule.kt          # Hilt modules
│   │       ├── network/BackendClient.kt     # API client
│   │       ├── permissions/PermissionManager.kt
│   │       └── services/
│   │           ├── device/DeviceInfoMonitor.kt
│   │           ├── phone/*.kt              # Call handling
│   │           ├── sms/*.kt                # SMS handling
│   │           └── sync/SyncService.kt     # Backend sync
│   └── README.md               # Android documentation
├── tests/                      # Backend tests
│   ├── __init__.py
│   ├── conftest.py           # Pytest configuration
│   ├── test_schemas.py       # Schema validation tests
│   ├── test_repositories.py  # Repository layer tests
│   ├── test_services.py      # Service logic tests
│   └── test_endpoints.py     # Endpoint integration tests
├── .env.template              # Environment variables template
├── pyproject.toml            # Project dependencies
├── README.md                 # This file
├── ANDROID_INTEGRATION.md    # Android setup & integration guide
└── .gitignore               # Git ignore rules
```

## Installation

### Backend Prerequisites
- Python 3.9+
- pip or uv

### Android Prerequisites
- Android Studio 2023.1+
- Android SDK 26+ (Kotlin 1.9.10)
- Physical Android device or emulator

### Backend Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd voice-assistant
```

2. **Create virtual environment**
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies**
```bash
pip install -e ".[dev]"
```

4. **Configure environment**
```bash
cp .env.template .env
# Edit .env with your settings (especially OPENAI_API_KEY)
```

5. **Start backend server**
```bash
python -m app.main
# Server will be at http://localhost:8000
# API docs at http://localhost:8000/docs
```

### Android Setup

1. **Open Android project in Android Studio**
```bash
# From project root
open android/
```

2. **Sync Gradle**
   - Android Studio will automatically sync dependencies

3. **Configure backend URL**
   - Edit `android/src/main/java/com/voiceassistant/android/MainActivity.kt`
   - Set `appConfig.backendUrl` to your backend URL (e.g., `http://192.168.1.100:8000`)

4. **Build and run**
```bash
./gradlew installDebug
```

For detailed Android setup and configuration, see [ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md)

## Environment Configuration

Copy `.env.template` to `.env` and configure:

```env
# App settings
APP_NAME=Backend Service
DEBUG=false
LOG_LEVEL=INFO

# Server
HOST=0.0.0.0
PORT=8000

# Database (SQLite for development, PostgreSQL for production)
DATABASE_URL=sqlite:///./app.db
# DATABASE_URL=postgresql://user:password@localhost/dbname

# OpenAI Configuration
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-3.5-turbo
OPENAI_MAX_TOKENS=500
OPENAI_TEMPERATURE=0.7

# Timeouts (seconds)
REQUEST_TIMEOUT=30
OPENAI_TIMEOUT=20

# Retry settings
MAX_RETRIES=3
RETRY_DELAY=1.0

# CORS
CORS_ORIGINS=["*"]
```

## API Endpoints

### Health Check
```
GET /health
```
Returns server health status.

### User Management

#### Create User
```
POST /users/
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "phone_number": "+1234567890"
}
```

#### Get User
```
GET /users/{user_id}
```

#### List Users
```
GET /users/?skip=0&limit=10
```

#### Update User
```
PUT /users/{user_id}
Content-Type: application/json

{
  "email": "newemail@example.com",
  "is_active": true
}
```

#### Delete User
```
DELETE /users/{user_id}
```

### User Preferences

#### Create Preferences
```
POST /preferences/?user_id=1
Content-Type: application/json

{
  "language": "en",
  "tts_voice": "nova",
  "auto_reply_enabled": false,
  "conversation_timeout": 300
}
```

#### Get Preferences
```
GET /preferences/{user_id}
```

#### Update Preferences
```
PUT /preferences/{preference_id}
Content-Type: application/json

{
  "language": "es",
  "tts_voice": "alloy"
}
```

#### Delete Preferences
```
DELETE /preferences/{preference_id}
```

### Conversation

#### Process Conversation
```
POST /conversation/
Content-Type: application/json

{
  "user_id": 1,
  "text": "What is the capital of France?"
}
```

**Response:**
```json
{
  "id": 1,
  "user_id": 1,
  "input_text": "What is the capital of France?",
  "gpt_response": "The capital of France is Paris.",
  "input_tokens": 10,
  "output_tokens": 8,
  "processing_time_ms": 1250.5,
  "model_used": "gpt-3.5-turbo",
  "created_at": "2024-01-15T10:30:00"
}
```

### SMS Decision

#### Make SMS Decision
```
POST /sms/decision
Content-Type: application/json

{
  "user_id": 1,
  "text": "Can you help me tomorrow?"
}
```

**Response:**
```json
{
  "id": 1,
  "user_id": 1,
  "incoming_text": "Can you help me tomorrow?",
  "decision": "yes",
  "reply_text": "Yes, I can help you tomorrow.",
  "created_at": "2024-01-15T10:30:00"
}
```

### History

#### Get Full History
```
GET /history/{user_id}?limit=50
```

Returns combined conversation, call, and SMS logs.

#### Get Conversation History
```
GET /history/{user_id}/conversations?limit=50
```

#### Get Call History
```
GET /history/{user_id}/calls?limit=50
```

#### Get SMS History
```
GET /history/{user_id}/sms?limit=50
```

## Running the Application

### Development

```bash
python -m app.main
```

Or with auto-reload:
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at `http://localhost:8000`
Documentation at `http://localhost:8000/docs`

### Production

```bash
gunicorn app.main:app \
  --workers 4 \
  --worker-class uvicorn.workers.UvicornWorker \
  --bind 0.0.0.0:8000
```

## Testing

Run all tests:
```bash
pytest
```

With coverage:
```bash
pytest --cov=app --cov-report=html
```

Run specific test file:
```bash
pytest tests/test_services.py -v
```

Run specific test:
```bash
pytest tests/test_services.py::TestOpenAIService::test_service_initialization -v
```

## Database

### SQLite (Development)

Default configuration uses SQLite. Database file: `app.db`

### PostgreSQL (Production)

Update `DATABASE_URL` in `.env`:
```
DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/dbname
```

Install PostgreSQL driver:
```bash
pip install asyncpg
```

### Models

- **User**: Application users with contact info
- **UserPreference**: Language, voice, timeout, and notification settings
- **ConversationLog**: AI conversation history with token usage
- **CallLog**: Call duration and status tracking
- **SMSLog**: SMS decision and reply history

## Performance Optimization

### Sub-2-Second Response Times

1. **Async/Await**: All I/O operations are non-blocking
2. **Connection Pooling**: Database connections are reused
3. **Service Caching**: OpenAI client is singleton and reused
4. **Timeouts**: OpenAI requests timeout after 20 seconds
5. **Response Monitoring**: Warnings logged when processing exceeds 2 seconds

To verify response times, check server logs:
```
INFO: Conversation processed for user 1 in 1250.50ms
```

## Error Handling

### OpenAI API Errors

- **Rate Limit (429)**: Auto-retry with exponential backoff (max 3 retries)
- **Timeout**: Logged and returns 503 Service Unavailable
- **Invalid Response**: Validation checks ensure response quality
- **Network Errors**: Caught and logged for debugging

### Database Errors

- **Connection Failures**: Caught and return 500 Internal Server Error
- **Constraint Violations**: Return 400 Bad Request with details
- **Transaction Errors**: Automatically rolled back

## Logging

Logs include:

```
2024-01-15 10:30:00 - app.services - INFO - Processing conversation: What is...
2024-01-15 10:30:01 - app.services - INFO - OpenAI request successful: 10 input tokens, 8 output tokens
2024-01-15 10:30:01 - app.routers.conversation - INFO - Conversation processed for user 1 in 1250.50ms
```

Configure log level in `.env`:
```
LOG_LEVEL=DEBUG  # DEBUG, INFO, WARNING, ERROR, CRITICAL
```

## Security

- CORS enabled (configure `CORS_ORIGINS` in `.env`)
- Trusted host middleware
- Environment variables for secrets (API keys)
- SQL injection prevention via ORM
- Request validation with Pydantic

## API Documentation

Automatic interactive docs available at:

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## Dependencies

### Core
- **fastapi**: Web framework
- **uvicorn**: ASGI server
- **sqlmodel**: ORM with Pydantic
- **openai**: OpenAI API client
- **pydantic-settings**: Settings management
- **tenacity**: Retry logic
- **python-dotenv**: Environment variable management

### Development
- **pytest**: Testing framework
- **pytest-asyncio**: Async test support
- **pytest-cov**: Coverage reporting
- **black**: Code formatting
- **isort**: Import sorting
- **flake8**: Linting
- **mypy**: Type checking

## Contributing

1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes following code style
3. Run tests: `pytest`
4. Commit: `git commit -am 'Add feature'`
5. Push: `git push origin feature/my-feature`
6. Create pull request

## License

MIT License - See LICENSE file for details

## Android Integration Guide

For comprehensive Android setup, configuration, and troubleshooting, see **[ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md)**.

### Quick Start
1. Start backend: `python -m app.main`
2. Get your machine IP: `ifconfig | grep "inet "`
3. Update Android `AppConfig.backendUrl` with your IP
4. Build and run: `./gradlew installDebug`

### Key Features
- **Auto-Answer Calls**: Calls are automatically answered via TelecomManager
- **SMS Auto-Reply**: Backend decides whether to reply, Android sends if approved
- **Call Logging**: All calls logged locally and synced with backend
- **SMS Logging**: All SMS tracked locally and synced to `/history` endpoint
- **Permission Handling**: Comprehensive permission requests with fallbacks
- **Device Monitoring**: Battery, running apps, and contacts available
- **Offline Support**: Local Room database stores events when offline

### Architecture
```
Incoming Call/SMS
  ↓
CallReceiver/SMSReceiver (BroadcastReceiver)
  ↓
PhoneStateManager/SMSHandler
  ├─ Local logging (Room database)
  ├─ Backend request (if auto-reply)
  └─ State management
  ↓
SyncService (periodic sync)
  ├─ Upload call logs
  └─ Upload SMS logs
  ↓
Backend /history endpoints
```

## Support

For issues and questions:
1. Check existing GitHub issues
2. Review [ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md) for Android issues
3. Create new issue with details
4. Include error logs and reproduction steps

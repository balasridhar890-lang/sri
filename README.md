# Backend Service - FastAPI with OpenAI Integration

A Python FastAPI backend service with OpenAI GPT integration, SQLModel-based database persistence, and comprehensive conversation/SMS handling capabilities.

## Features

- **FastAPI Framework**: Modern async Python web framework with automatic OpenAPI documentation
- **OpenAI Integration**: Robust GPT-3.5-turbo integration with error handling, retries, and timeouts
- **SQLModel Database**: SQLAlchemy ORM with Pydantic validation, async support, and migration-ready
- **Conversation Endpoint**: Process natural language conversations with AI responses and TTS metadata
- **SMS Decision Endpoint**: Make yes/no decisions and generate replies for SMS messages
- **User Management**: Create, read, update, delete users with preferences
- **History Tracking**: Complete conversation, call, and SMS logs for audit trails
- **Dependency Injection**: Clean service architecture with mockable dependencies
- **Async/Await**: Non-blocking I/O for sub-2-second response times
- **Comprehensive Testing**: Unit tests for schemas, repositories, services, and endpoints
- **Production Ready**: CORS, error handling, logging, and security headers

## Project Structure

```
.
├── app/
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
├── tests/
│   ├── __init__.py
│   ├── conftest.py           # Pytest configuration
│   ├── test_schemas.py       # Schema validation tests
│   ├── test_repositories.py  # Repository layer tests
│   ├── test_services.py      # Service logic tests
│   └── test_endpoints.py     # Endpoint integration tests
├── .env.template              # Environment variables template
├── pyproject.toml            # Project dependencies
├── README.md                 # This file
└── .gitignore               # Git ignore rules
```

## Installation

### Prerequisites

- Python 3.9+
- pip or uv

### Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd backend-service
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
# Edit .env with your settings
```

5. **Initialize database**
```bash
python -m app.main  # Will auto-initialize on startup
```

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

## Support

For issues and questions:
1. Check existing GitHub issues
2. Create new issue with details
3. Include error logs and reproduction steps

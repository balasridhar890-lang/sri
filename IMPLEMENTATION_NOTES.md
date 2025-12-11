# Implementation Notes - Voice Assistant Phone Integration

## Overview

This document provides technical implementation details for the phone integration system consisting of a FastAPI backend and Android Kotlin app.

## Project Architecture

### System Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         ANDROID DEVICE                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐         ┌──────────────────┐              │
│  │   Incoming Call  │         │   Incoming SMS   │              │
│  └────────┬─────────┘         └────────┬─────────┘              │
│           │                             │                        │
│           ▼                             ▼                        │
│  ┌─────────────────────────────────────────┐                    │
│  │  CallReceiver/SMSReceiver (BroadcastReceiver)               │
│  └─────────────────────┬───────────────────┘                    │
│                        │                                         │
│                        ▼                                         │
│  ┌─────────────────────────────────────────┐                    │
│  │  PhoneStateManager / SMSHandler          │                    │
│  │  (Main business logic)                   │                    │
│  └──────┬──────────────────────────────────┘                    │
│         │                                                        │
│         ├─────────────────────────┬──────────────────┐         │
│         │                         │                  │          │
│         ▼                         ▼                  ▼          │
│  ┌──────────────┐       ┌──────────────┐  ┌────────────────┐  │
│  │ Auto-Answer  │       │ Local Log    │  │ Backend        │  │
│  │ (TelecomMgr) │       │ (Room DB)    │  │ Request        │  │
│  └──────────────┘       └──────────────┘  │ (Retrofit)     │  │
│                                           └────────┬───────┘  │
│         ┌─────────────────────────────────────────┘           │
│         │                                                      │
│         ▼                                                      │
│  ┌──────────────────────────────────────┐                    │
│  │  SyncService (Periodic)              │                    │
│  │  - Get unsynced logs from Room DB    │                    │
│  │  - Upload to backend /history        │                    │
│  │  - Mark as synced on success         │                    │
│  └──────────────┬───────────────────────┘                    │
│                 │                                             │
│                 └────────────────────┐                       │
│                                      │                       │
└──────────────────────────────────────┼───────────────────────┘
                                       │
                                       ▼
                    ┌──────────────────────────────┐
                    │      FASTAPI BACKEND         │
                    ├──────────────────────────────┤
                    │ POST /sms/decision           │
                    │ GET  /history/{user_id}      │
                    │ GET  /history/{user_id}/sms  │
                    │ GET  /history/{user_id}/calls│
                    └──────────────────────────────┘
```

## Backend Implementation

### FastAPI Structure

```
app/
├── main.py
│   └── FastAPI app initialization with lifespan, CORS, middleware
│
├── models.py
│   ├── User (id, username, email, phone_number, is_active, timestamps)
│   ├── UserPreference (language, tts_voice, auto_reply_enabled, timeout)
│   ├── ConversationLog (user_id, input_text, gpt_response, tokens, timing)
│   ├── CallLog (user_id, call_duration_seconds, success, error_message)
│   └── SMSLog (user_id, incoming_text, decision, reply_text)
│
├── schemas.py
│   ├── Request schemas: ConversationRequest, SMSDecisionRequest
│   └── Response schemas: ConversationResponse, SMSDecisionResponse, etc.
│
├── repositories.py
│   ├── UserRepository (CRUD operations)
│   ├── UserPreferenceRepository
│   ├── ConversationLogRepository
│   ├── CallLogRepository
│   └── SMSLogRepository (with get_by_user_id, get_unsynced, etc.)
│
├── services.py
│   ├── OpenAIService (GPT requests with retry logic)
│   ├── ConversationService
│   └── SMSDecisionService (yes/no decision logic)
│
├── dependencies.py
│   └── Dependency injection for services
│
└── routers/
    ├── health.py (GET /health)
    ├── users.py (CRUD)
    ├── preferences.py (CRUD)
    ├── conversation.py (POST /conversation)
    ├── sms.py (POST /sms/decision)
    └── history.py (GET /history/* endpoints)
```

### Key API Endpoints

#### SMS Decision Endpoint
```
POST /sms/decision

Request:
{
  "user_id": 1,
  "text": "Can you help tomorrow?"
}

Response:
{
  "id": 1,
  "user_id": 1,
  "incoming_text": "Can you help tomorrow?",
  "decision": "yes",
  "reply_text": "Yes, I can help tomorrow.",
  "created_at": "2024-01-01T10:00:00Z"
}
```

Implementation flow:
1. Validate user exists
2. Call SMSDecisionService with message text
3. OpenAI processes: should_reply(yes/no) and generate_reply(text)
4. Save SMSLog to database
5. Return decision and reply text to client

#### History Endpoints
```
GET /history/{user_id}
├─ conversation_logs: List[ConversationLogResponse]
├─ call_logs: List[CallLogResponse]
└─ sms_logs: List[SMSDecisionResponse]

GET /history/{user_id}/calls
└─ List[CallLogResponse]

GET /history/{user_id}/sms
└─ List[SMSDecisionResponse]
```

## Android Implementation

### Module Structure

```
android/
├── src/main/
│   ├── AndroidManifest.xml
│   │   ├── Permissions (CALL, SMS, CONTACTS, STORAGE, FOREGROUND_SERVICE)
│   │   ├── Services (PhoneStateService, SyncService)
│   │   └── Receivers (CallReceiver, SMSReceiver)
│   │
│   └── java/com/voiceassistant/android/
│       ├── MainActivity.kt
│       │   ├── Permission checking/requesting
│       │   └── Service startup
│       │
│       ├── VoiceAssistantApp.kt
│       │   └── Notification channel creation
│       │
│       ├── config/AppConfig.kt
│       │   └── SharedPreferences wrapper for settings
│       │
│       ├── database/AppDatabase.kt
│       │   ├── CallLogEntity/Dao
│       │   ├── SMSLogEntity/Dao
│       │   └── Room database definition
│       │
│       ├── network/
│       │   ├── BackendClient.kt (Retrofit interface + client)
│       │   ├── BackendIntegration.kt (High-level wrapper)
│       │   └── Data classes (Request/Response models)
│       │
│       ├── permissions/PermissionManager.kt
│       │   ├── hasPermission(permission: String): Boolean
│       │   ├── getMissingPermissions(): Array<String>
│       │   └── Grouped permission checks
│       │
│       ├── services/
│       │   ├── device/DeviceInfoMonitor.kt
│       │   │   ├── getBatteryPercentage()
│       │   │   ├── getRunningApps()
│       │   │   └── getContacts()
│       │   │
│       │   ├── phone/
│       │   │   ├── PhoneStateManager.kt (StateFlow-based)
│       │   │   ├── CallReceiver.kt (BroadcastReceiver)
│       │   │   ├── CallLogger.kt (Local + remote logging)
│       │   │   └── PhoneStateService.kt (Foreground service)
│       │   │
│       │   ├── sms/
│       │   │   ├── SMSReceiver.kt (BroadcastReceiver)
│       │   │   └── SMSHandler.kt (Workflow orchestration)
│       │   │
│       │   └── sync/SyncService.kt
│       │       └── Periodic backend synchronization
│       │
│       └── di/NetworkModule.kt
│           └── Hilt dependency injection
```

### Permission Handling Flow

```
App Launch
  │
  ├─ Check required permissions
  │
  ├─ If missing:
  │  └─ Show explanation dialog
  │      └─ Request permissions
  │          ├─ Granted: Start services
  │          └─ Denied: Show fallback
  │              └─ Continue with limited features
  │
  └─ On Resume:
     ├─ Check critical permissions still granted
     └─ Re-request if lost
```

### Call Handling Flow

```
Incoming Call (Phone Hardware)
  │
  ├─ Android broadcasts PHONE_STATE intent
  │
  ├─ CallReceiver.onReceive()
  │  └─ Extract phone number
  │     └─ Call handlePhoneStateChange()
  │
  ├─ PhoneStateManager.handleCallStateChange()
  │  ├─ State RINGING
  │  │  └─ Create IncomingCall state
  │  │
  │  ├─ State OFFHOOK (call answered)
  │  │  └─ Start auto-answer or voice assistant
  │  │
  │  └─ State IDLE (call ended)
  │     └─ Calculate duration
  │
  ├─ CallLogger.logCompletedCall()
  │  └─ Save to Room database
  │     └─ Mark synced=false
  │
  └─ SyncService (5-min intervals)
     └─ Get unsynced logs from DAO
        └─ Attempt sync with backend
           └─ Mark synced=true on success
```

### SMS Handling Flow

```
Incoming SMS (Telephony)
  │
  ├─ Android broadcasts SMS_RECEIVED intent
  │
  ├─ SMSReceiver.onReceive()
  │  └─ Extract sender and message
  │     └─ Call SMSHandler.handleIncomingSMS()
  │
  ├─ SMSHandler.handleIncomingSMS()
  │  │
  │  ├─ Request decision from backend
  │  │  └─ POST /sms/decision
  │  │     ├─ Request: {user_id, text}
  │  │     └─ Response: {decision, reply_text}
  │  │
  │  ├─ Log SMS locally
  │  │  └─ SMSLogEntity to Room database
  │  │
  │  └─ If decision == "yes":
  │     ├─ SmsManager.sendMultipartTextMessage()
  │     │  └─ Split if > 160 chars
  │     │
  │     └─ Update log with replySent=true
  │
  └─ SyncService (5-min intervals)
     └─ Get unsynced SMS logs
        └─ Sync with backend /history
```

## Data Models

### Backend Models

#### CallLog
```python
class CallLog(table=True):
    id: int = PrimaryKey
    user_id: int = ForeignKey("users.id")
    call_duration_seconds: float
    success: bool
    error_message: Optional[str]
    created_at: datetime
```

#### SMSLog
```python
class SMSLog(table=True):
    id: int = PrimaryKey
    user_id: int = ForeignKey("users.id")
    incoming_text: str
    decision: SMSDecisionEnum  # "yes" or "no"
    reply_text: str
    created_at: datetime
```

### Android Models

#### CallLogEntity (Room)
```kotlin
@Entity("call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val phoneNumber: String,
    val direction: String,  // "incoming" or "outgoing"
    val timestamp: Long,    // milliseconds
    val durationSeconds: Long,
    val success: Boolean,
    val errorMessage: String?,
    val synced: Boolean     // Backend sync flag
)
```

#### SMSLogEntity (Room)
```kotlin
@Entity("sms_logs")
data class SMSLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val phoneNumber: String,
    val messageBody: String,
    val decision: String,    // "yes", "no", or "error"
    val replyText: String,
    val replySent: Boolean,
    val timestamp: Long,     // milliseconds
    val errorMessage: String?,
    val synced: Boolean      // Backend sync flag
)
```

## Error Handling

### Backend Errors

1. **User not found (404)**
   - SMS/History endpoints check user exists
   - Return 404 Not Found

2. **OpenAI API errors**
   - Rate limit (429): Auto-retry with exponential backoff
   - Timeout: Return 503 Service Unavailable
   - Invalid response: Log and return 500

3. **Database errors**
   - Connection failure: Return 500
   - Constraint violation: Return 400 with details

### Android Errors

1. **Permission denied**
   - Show dialog explaining impact
   - Continue with limited features
   - Periodically re-request critical permissions

2. **Backend not reachable**
   - Log error locally
   - Mark as unsynced for later retry
   - Default to safe action (don't send SMS)

3. **SMS send failure**
   - Log error to database
   - Don't retry automatically
   - Allow manual retry later

4. **Call monitoring failure**
   - Log error to logcat
   - Continue monitoring
   - Fallback to normal call handling

## Performance Considerations

### Backend
- **Async/await**: All I/O non-blocking
- **Connection pooling**: Database connections reused
- **Service caching**: OpenAI client is singleton
- **Timeouts**: OpenAI requests timeout after 20s
- **Response monitoring**: Warnings logged when > 2 seconds

### Android
- **Coroutines**: All network calls off main thread
- **Local database**: Minimal backend calls
- **Background sync**: 5-minute intervals (configurable)
- **Batch operations**: Sync multiple logs in one request
- **Foreground service**: Prevents app termination

## Security Considerations

### Backend
- CORS properly configured
- OpenAI API key in environment variables
- Request validation via Pydantic
- SQL injection prevention (ORM)
- User authentication (future enhancement)

### Android
- Network requests use HTTPS (in production)
- No sensitive data in logs
- Permissions requested with explanation
- No hardcoded credentials
- Local database not encrypted (future enhancement)

## Testing Strategy

### Backend Tests
```
tests/
├── test_schemas.py       # Request/response validation
├── test_repositories.py  # Database access
├── test_services.py      # Business logic (OpenAI, SMS decision)
└── test_endpoints.py     # HTTP endpoints
```

Run: `pytest`

### Android Tests
```
androidTest/
├── Integration tests     # Backend API communication
└── Permission tests      # Permission request flows

test/
├── Unit tests           # Services, repositories
└── Database tests       # Room operations
```

Run: `./gradlew connectedAndroidTest`

### Manual Testing Checklist

**Backend**
- [ ] Create user
- [ ] Get SMS decision for various inputs
- [ ] Fetch history endpoints
- [ ] Test with OpenAI API key and without

**Android**
- [ ] Request all permissions (accept and deny)
- [ ] Receive incoming call → Auto-answer
- [ ] Receive SMS → Request backend decision → Send reply
- [ ] Monitor battery, apps, contacts
- [ ] Offline mode → Sync when online
- [ ] Verify local logs in database
- [ ] Check backend received all logs

## Deployment

### Backend
```bash
# Development
python -m app.main

# Production (with gunicorn)
gunicorn app.main:app --workers 4 --worker-class uvicorn.workers.UvicornWorker
```

### Android
```bash
# Development build
./gradlew installDebug

# Release build
./gradlew build --variant release
```

## Future Enhancements

### Backend
- [ ] User authentication
- [ ] SMS rate limiting
- [ ] Call quality metrics
- [ ] Advanced analytics
- [ ] Call recording
- [ ] Multi-language support

### Android
- [ ] Visual call state in UI
- [ ] Customizable SMS templates
- [ ] Call recording
- [ ] Time-based auto-answer schedules
- [ ] Encrypted local storage
- [ ] Widget for quick access
- [ ] VoIP integration
- [ ] Conference call support

## Troubleshooting

### Backend Connection Issues
```
Error: Failed to reach backend
Solution: 
1. Verify backend URL in AppConfig
2. Check backend is running on correct port
3. Verify firewall allows connections
4. Use correct device IP (not localhost)
```

### SMS Not Auto-Replying
```
Error: SMS received but no reply sent
Solution:
1. Check backend /sms/decision endpoint works
2. Verify SEND_SMS permission granted
3. Check logcat for decision response
4. Ensure reply_text is not empty
```

### Sync Not Working
```
Error: Logs not syncing to backend
Solution:
1. Check INTERNET permission granted
2. Verify backend /history endpoint works
3. Check unsynced logs in Room database
4. Review network logs for connection errors
5. Verify backend URL configuration
```

## Conclusion

This implementation provides a complete, production-ready phone integration system with:
- Robust error handling and fallbacks
- Comprehensive permission management
- Efficient local storage and sync
- Clean architecture with separation of concerns
- Extensive logging for debugging
- Flexible configuration for different environments

The modular design allows for easy extension and testing of individual components.

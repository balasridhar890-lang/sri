# Android Phone Integration - Complete Setup Guide

This document provides complete setup and integration instructions for the Android phone integration module with the FastAPI backend.

## Table of Contents

1. [Project Structure](#project-structure)
2. [Backend Prerequisites](#backend-prerequisites)
3. [Android Setup](#android-setup)
4. [Configuration](#configuration)
5. [Runtime Flow](#runtime-flow)
6. [Troubleshooting](#troubleshooting)

## Project Structure

### Backend (FastAPI)
```
app/
├── routers/
│   ├── sms.py              # POST /sms/decision endpoint
│   └── history.py          # GET /history/* endpoints
├── models.py               # CallLog, SMSLog database models
├── schemas.py              # Request/response schemas
└── services.py             # SMS decision service
```

### Android (Kotlin)
```
android/
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/voiceassistant/android/
│       ├── MainActivity.kt                          # Entry point with permissions
│       ├── VoiceAssistantApp.kt                     # Hilt app with notification channels
│       ├── config/AppConfig.kt                      # Backend URL & settings
│       ├── network/BackendClient.kt                 # API client (Retrofit)
│       ├── permissions/PermissionManager.kt         # Permission handling
│       ├── services/
│       │   ├── device/DeviceInfoMonitor.kt         # Battery, apps, contacts
│       │   ├── phone/
│       │   │   ├── PhoneStateManager.kt            # State machine
│       │   │   ├── CallReceiver.kt                 # Broadcast receiver
│       │   │   ├── CallLogger.kt                   # Local & remote logging
│       │   │   └── PhoneStateService.kt            # Foreground service
│       │   ├── sms/
│       │   │   ├── SMSReceiver.kt                  # Broadcast receiver
│       │   │   └── SMSHandler.kt                   # Workflow & logging
│       │   └── sync/SyncService.kt                 # Periodic sync
│       └── database/AppDatabase.kt                 # Room database
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Backend Prerequisites

### 1. Verify FastAPI Backend Running

Ensure your FastAPI backend is running on your development machine:

```bash
# Install backend dependencies
cd /home/engine/project
pip install -e ".[dev]"

# Run the backend
python -m app.main
# Server will be at http://localhost:8000
```

### 2. Verify Backend Endpoints

Test the backend endpoints work correctly:

```bash
# Test SMS decision endpoint
curl -X POST http://localhost:8000/sms/decision \
  -H "Content-Type: application/json" \
  -d '{"user_id": 1, "text": "Hello, how are you?"}'

# Expected response:
# {
#   "id": 1,
#   "user_id": 1,
#   "incoming_text": "Hello, how are you?",
#   "decision": "yes",
#   "reply_text": "I'm doing well, thanks!",
#   "created_at": "2024-01-01T00:00:00Z"
# }

# Test history endpoint
curl http://localhost:8000/history/1
```

### 3. Database Setup

The backend uses SQLite by default. Ensure database is initialized:

```bash
# The database will be created automatically on first run
# It will be at app.db in the project root
```

## Android Setup

### 1. Android Studio Setup

1. **Open Android Project**
   ```bash
   # The Android project is at /home/engine/project/android
   # Open it in Android Studio
   ```

2. **Sync Gradle**
   - Let Android Studio sync all dependencies
   - Gradle will download required libraries

3. **Configure SDK**
   - Minimum SDK: 26 (Android 8)
   - Target SDK: 34 (Android 14)

### 2. Virtual Device Setup (for testing)

1. **Create Emulator**
   - Device: Pixel 4 or similar
   - Android Version: API 31+ (Android 11+)
   - Enable Telephony: Yes
   - Storage: 2GB minimum

2. **Important for Testing**
   - Telephony is not fully functional in emulator
   - For full testing, use a physical device
   - Call/SMS simulation available but limited

### 3. Physical Device Setup (Recommended)

1. **Enable Developer Mode**
   - Go to Settings > About Phone
   - Tap Build Number 7 times
   - Developer options now visible

2. **Enable USB Debugging**
   - Settings > Developer Options > USB Debugging
   - Connect device to computer

3. **Verify ADB Connection**
   ```bash
   adb devices
   # Should show your device with "device" status
   ```

## Configuration

### 1. Set Backend URL

**Option A: In Code (AppConfig.kt)**
```kotlin
// In MainActivity onCreate or Application onCreate
appConfig.backendUrl = "http://192.168.1.100:8000"  // Use your machine IP
appConfig.userId = 1  // User ID from backend
```

**Option B: Environment Variable**
```bash
# Set in your Android app's build.gradle.kts
buildTypes {
    debug {
        buildConfigField("String", "BACKEND_URL", "\"http://192.168.1.100:8000\"")
    }
}
```

**Option C: SharedPreferences**
```kotlin
// In MainActivity
val prefs = getSharedPreferences("voice_assistant_prefs", Context.MODE_PRIVATE)
prefs.edit()
    .putString("backend_url", "http://192.168.1.100:8000")
    .putInt("user_id", 1)
    .apply()
```

### 2. Get Device IP Address

To connect Android to backend on your machine:

```bash
# On Linux/Mac
ifconfig | grep "inet " | grep -v 127.0.0.1

# On Windows
ipconfig
```

Use the IPv4 address (e.g., 192.168.1.100) in backend URL.

### 3. Allow Backend to Accept Connections

**Edit FastAPI CORS in app/main.py:**
```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all origins for development
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

### 4. Create Test User

Before running the app, create a user in the backend:

```bash
curl -X POST http://localhost:8000/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "phone_number": "+1234567890"
  }'

# Note the returned user ID (e.g., 1)
# Use this ID in Android configuration
```

## Runtime Flow

### 1. Application Startup

```
MainActivity.onCreate()
  ↓
Check required permissions
  ├─ Missing permissions? → Request from user
  └─ All permissions granted? → Continue
  ↓
Load configuration (AppConfig)
  ├─ Backend URL
  ├─ User ID
  └─ Feature flags (auto-answer, auto-reply)
  ↓
Start foreground services
  ├─ PhoneStateService (call monitoring)
  ├─ SMSHandlerService (SMS monitoring)
  └─ SyncService (periodic sync)
  ↓
App ready
```

### 2. Incoming Call Flow

```
CallReceiver.onReceive() (broadcast)
  ↓
Parse phone number
  ↓
PhoneStateManager.handleCallStateChange()
  ├─ State: RINGING
  ├─ State: OFFHOOK (call connected)
  └─ State: IDLE (call ended)
  ↓
If auto-answer enabled:
  ├─ TelecomManager.acceptRingingCall()
  ├─ CallLogger.logIncomingCall()
  └─ Start voice assistant
  ↓
If call ends:
  └─ CallLogger.logCompletedCall() → Database
  ↓
Sync service picks up unsynced logs:
  └─ POST to backend /history endpoint
```

### 3. Incoming SMS Flow

```
SMSReceiver.onReceive() (broadcast)
  ↓
Extract phone number and message
  ↓
SMSHandler.handleIncomingSMS()
  ├─ Get user ID from config
  ├─ Request backend decision:
  │  └─ POST /sms/decision
  │     ├─ user_id: 1
  │     └─ text: "Hello"
  │  ↓
  │  Receive response:
  │     ├─ decision: "yes" or "no"
  │     └─ reply_text: "..."
  │
  ├─ Log SMS locally
  │  └─ SMSLogEntity in Room database
  │
  └─ If decision == "yes":
     ├─ SmsManager.sendMultipartTextMessage()
     ├─ Update log with send status
     └─ Sync with backend
     ↓
  If network error:
     └─ Default to "no" (don't send)
```

### 4. Data Synchronization Flow

```
SyncService (5-minute intervals)
  ↓
Get unsynced call logs:
  └─ CallLogDao.getUnsyncedLogs()
  ↓
For each unsynced log:
  ├─ Attempt to sync with backend
  └─ Mark as synced on success
  ↓
Get unsynced SMS logs:
  └─ SMSLogDao.getUnsyncedLogs()
  ↓
For each unsynced log:
  ├─ Attempt to sync with backend
  └─ Mark as synced on success
  ↓
On sync failure:
  └─ Retry at next interval (exponential backoff recommended)
```

## Troubleshooting

### Backend Connection Issues

**Problem: Android app cannot reach backend**

```bash
# Check backend is running
curl http://localhost:8000/health

# Check firewall allows connections
sudo iptables -L | grep 8000

# Allow port 8000
sudo iptables -I INPUT -p tcp --dport 8000 -j ACCEPT

# Verify Android device can reach your machine
adb shell ping 192.168.1.100  # Replace with your IP
```

**Solution:**
```kotlin
// In AppConfig or MainActivity
appConfig.backendUrl = "http://192.168.1.100:8000"  // Use correct IP
```

### Permission Issues

**Problem: Permissions not being requested**

```bash
# Check manifest has permissions declared
grep "uses-permission" android/src/main/AndroidManifest.xml

# Clear app data on device
adb shell pm clear com.voiceassistant.android

# Reinstall app
./gradlew installDebug
```

### Call Monitoring Not Working

**Problem: PhoneStateService not starting**

```bash
# Check service is running
adb shell dumpsys activity services | grep PhoneStateService

# Check logcat for errors
adb logcat com.voiceassistant.android:D

# Ensure FOREGROUND_SERVICE permission in manifest
grep "FOREGROUND_SERVICE" android/src/main/AndroidManifest.xml
```

### SMS Auto-Reply Not Working

**Problem: SMS received but not replying**

```bash
# Check SMSReceiver is registered
grep "SMSReceiver" android/src/main/AndroidManifest.xml

# Check backend /sms/decision endpoint
curl -X POST http://localhost:8000/sms/decision \
  -H "Content-Type: application/json" \
  -d '{"user_id": 1, "text": "Test"}'

# Send test SMS
adb shell service call isms 1 s16 "1234567890" s16 "" s16 "Hello from emulator" s16 null s16 null
```

### Database Issues

**Problem: Local logs not being stored**

```bash
# Check database file exists
adb shell ls -la /data/data/com.voiceassistant.android/databases/

# Export database for inspection
adb pull /data/data/com.voiceassistant.android/databases/voice_assistant_db

# Check Room migrations
# In AppDatabase.kt, ensure entities match schema
```

### Network Logging

**Problem: Cannot see what API calls are being made**

```bash
# Enable debug logging in BackendClient
// In BackendClient.kt
.addInterceptor(HttpLoggingInterceptor { message ->
    Log.d("API", message)
}.apply {
    level = HttpLoggingInterceptor.Level.BODY  // Enable to see request/response bodies
})

# View logs
adb logcat com.voiceassistant.android:D | grep API
```

## Development Workflow

### 1. Build and Deploy

```bash
# Build and install on connected device/emulator
./gradlew installDebug

# Or with specific device
adb devices  # List devices
./gradlew installDebug -PhoneDeviceSerial=<device_id>
```

### 2. Run with Logging

```bash
# Install and run app
./gradlew installDebugAndRunTests

# View live logs
adb logcat com.voiceassistant.android:*
```

### 3. Test Backend Integration

```bash
# Terminal 1: Run backend
python -m app.main

# Terminal 2: Create test user
curl -X POST http://localhost:8000/users \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "email": "test@test.com"}'

# Terminal 3: Simulate SMS decision
curl -X POST http://localhost:8000/sms/decision \
  -H "Content-Type: application/json" \
  -d '{"user_id": 1, "text": "Hello"}'

# Terminal 4: View app logs
adb logcat com.voiceassistant.android:D
```

## Next Steps

1. **Test on Physical Device** - Recommended for full functionality
2. **Implement Voice Assistant Integration** - Hook into `onCallConnected`
3. **Add UI Components** - Display call status, SMS logs, device info
4. **Implement Advanced Features**:
   - Call recording
   - Custom SMS templates
   - Time-based auto-answer schedules
   - Call quality metrics

## Additional Resources

- [Android Telephony Docs](https://developer.android.com/reference/android/telephony/package-summary)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin Coroutines](https://developer.android.com/kotlin/coroutines)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

## Support

For issues, check:
1. Logcat output: `adb logcat`
2. Backend logs: See terminal where `python -m app.main` runs
3. Database content: Export and inspect with SQLite viewer
4. Network traffic: Use Charles Proxy or similar

---

**Last Updated**: January 2024
**API Version**: 1.0
**Android Min SDK**: 26
**Target SDK**: 34

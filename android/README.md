# Voice Assistant Android Application

This is the Android/Kotlin implementation of the Voice Assistant phone integration module. It provides comprehensive phone state management, intelligent SMS auto-reply workflows, and call history logging with backend synchronization.

## Features

### 1. Phone Integration & Call Management
- **Call State Monitoring**: Intercepts incoming and outgoing calls using `PhoneStateManager`
- **Auto-Answer**: Automatically answers incoming calls (requires Android 8+ and appropriate permissions)
- **Call Logging**: Logs all call events locally with duration, success status, and error handling
- **Foreground Service**: Continuous monitoring via `PhoneStateService` with persistent notification
- **Voice Assistant Integration**: Hands control to voice assistant upon auto-answer

### 2. Intelligent SMS Auto-Reply
- **SMS Detection**: Monitors incoming SMS via `SMSReceiver`
- **Backend Decision**: Requests decision from `/sms/decision` endpoint
- **Conditional Reply**: Sends SMS reply only if backend approves
- **Local Logging**: Logs all SMS events locally in Room database
- **Remote Sync**: Syncs SMS logs with backend `/history` endpoint
- **Error Handling**: Graceful fallbacks when backend unavailable

### 3. Device Monitoring
- **Battery Status**: Monitors battery percentage, health, temperature, and charging state
- **Running Apps**: Tracks count and list of running applications
- **Contacts**: Accesses and monitors device contacts
- **Device Info Export**: Exposes all data to UI and backend logging

### 4. Permission Management
- **Rigorous Permission Flows**: Step-by-step permission requests with explanations
- **Permission Explanations**: Shows dialog explaining why each permission is needed
- **Fallback Handling**: App continues with limited functionality if permissions denied
- **Critical Permissions**: Identifies and prioritizes critical vs optional permissions
- **Permission Status Checks**: Continuously monitors permission status at runtime

### 5. Data Synchronization
- **Local Storage**: Room database for offline call and SMS logs
- **Periodic Sync**: Background sync service (5-minute intervals by default)
- **Conflict Resolution**: Handles network errors and retries
- **Privacy Protection**: Only syncs necessary data

## Architecture

```
android/
├── build.gradle.kts           # Build configuration
├── src/main/
│   ├── AndroidManifest.xml    # Permissions and service declarations
│   └── java/com/voiceassistant/android/
│       ├── MainActivity.kt              # Main UI with permission handling
│       ├── VoiceAssistantApp.kt         # Hilt application class
│       ├── config/
│       │   └── AppConfig.kt             # App settings & preferences
│       ├── database/
│       │   └── AppDatabase.kt           # Room database, DAOs, entities
│       ├── di/
│       │   └── NetworkModule.kt         # Hilt dependency injection
│       ├── network/
│       │   └── BackendClient.kt         # Retrofit API client
│       ├── permissions/
│       │   └── PermissionManager.kt     # Permission checking & handling
│       └── services/
│           ├── device/
│           │   └── DeviceInfoMonitor.kt # Battery, apps, contacts monitoring
│           ├── phone/
│           │   ├── PhoneStateManager.kt # Phone state machine
│           │   ├── CallReceiver.kt      # Incoming call broadcaster
│           │   ├── CallLogger.kt        # Local & remote call logging
│           │   └── PhoneStateService.kt # Foreground service
│           ├── sms/
│           │   ├── SMSReceiver.kt       # Incoming SMS broadcaster
│           │   └── SMSHandler.kt        # SMS workflow & logging
│           └── sync/
│               └── SyncService.kt       # Periodic backend sync
└── README.md
```

## Permissions

### Required Permissions

```xml
<!-- Phone State Management -->
android.permission.READ_PHONE_STATE
android.permission.CALL_PHONE
android.permission.ANSWER_PHONE_CALLS
android.permission.MANAGE_OWN_CALLS

<!-- SMS Handling -->
android.permission.RECEIVE_SMS
android.permission.SEND_SMS
android.permission.READ_SMS

<!-- Data Access -->
android.permission.READ_CONTACTS
android.permission.READ_EXTERNAL_STORAGE
android.permission.WRITE_EXTERNAL_STORAGE

<!-- System -->
android.permission.INTERNET
android.permission.ACCESS_NETWORK_STATE
android.permission.FOREGROUND_SERVICE
android.permission.FOREGROUND_SERVICE_PHONE_CALL
```

### Permission Request Flow

1. **On App Launch**
   - Check for missing permissions
   - Show explanation dialog
   - Request missing permissions

2. **Permission Grant**
   - Store granted status
   - Start services
   - Initialize monitoring

3. **Permission Denial**
   - Show fallback dialog
   - List denied permissions
   - Continue with limited features
   - Periodically re-request critical permissions

4. **Runtime Checks**
   - Monitor permission status on app resume
   - Handle permission revocation gracefully
   - Disable affected features when permissions lost

## Configuration

### AppConfig (SharedPreferences)

```kotlin
appConfig.backendUrl = "http://your-backend:8000"
appConfig.userId = 123
appConfig.autoAnswerEnabled = true
appConfig.autoReplyEnabled = true
appConfig.syncEnabled = true
```

### Backend Configuration

Update backend URL in `AppConfig` or environment:

```kotlin
// In MainActivity or settings
appConfig.backendUrl = "https://api.example.com"
appConfig.userId = getCurrentUserId()
```

## Backend API Integration

### SMS Decision Endpoint
```
POST /sms/decision
{
  "user_id": 123,
  "text": "Incoming message text"
}

Response:
{
  "id": 1,
  "user_id": 123,
  "incoming_text": "...",
  "decision": "yes|no",
  "reply_text": "...",
  "created_at": "2024-01-01T00:00:00Z"
}
```

### History Endpoints
```
GET /history/{user_id}
GET /history/{user_id}/calls
GET /history/{user_id}/sms
```

## Usage

### 1. Initialize in Application Class

```kotlin
@HiltAndroidApp
class VoiceAssistantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Hilt automatically initializes
    }
}
```

### 2. Handle Permissions in Activity

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var permissionManager: PermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
    }
}
```

### 3. Monitor Phone State

```kotlin
@AndroidEntryPoint
class MyFragment : Fragment() {
    @Inject
    lateinit var phoneStateManager: PhoneStateManager
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            phoneStateManager.phoneState.collect { state ->
                when (state) {
                    is PhoneStateManager.PhoneState.IncomingCall -> {
                        // Handle incoming call
                    }
                    is PhoneStateManager.PhoneState.CallConnected -> {
                        // Handle active call
                    }
                    is PhoneStateManager.PhoneState.CallDisconnected -> {
                        // Handle ended call
                    }
                    else -> {}
                }
            }
        }
    }
}
```

### 4. Monitor Device Info

```kotlin
@Inject
lateinit var deviceInfoMonitor: DeviceInfoMonitor

// Update device info
deviceInfoMonitor.updateDeviceInfo()

// Observe device info
viewLifecycleOwner.lifecycleScope.launch {
    deviceInfoMonitor.deviceInfo.collect { info ->
        info?.let {
            batteryPercentageText.text = "${it.batteryPercentage}%"
            runningAppsText.text = "${it.runningAppsCount} apps"
            contactsText.text = "${it.totalContactsCount} contacts"
        }
    }
}
```

## Services

### PhoneStateService
- Runs as foreground service
- Monitors phone state changes continuously
- Logs incoming/outgoing calls
- Integrates with voice assistant

**Start**: Automatically started by `CallReceiver` on incoming call

### SMSHandlerService
- Processes incoming SMS
- Requests backend decision
- Sends reply if approved
- Logs outcomes

**Start**: Automatically triggered by `SMSReceiver`

### SyncService
- Periodic synchronization (5 minutes)
- Syncs unsynced call logs
- Syncs unsynced SMS logs
- Handles network errors

**Start**: Manually started from MainActivity

## Data Storage

### Local Database (Room)

#### CallLogEntity
```kotlin
data class CallLogEntity(
    val id: Int,
    val phoneNumber: String,
    val direction: String,        // "incoming" or "outgoing"
    val timestamp: Long,
    val durationSeconds: Long,
    val success: Boolean,
    val errorMessage: String?,
    val synced: Boolean
)
```

#### SMSLogEntity
```kotlin
data class SMSLogEntity(
    val id: Int,
    val phoneNumber: String,
    val messageBody: String,
    val decision: String,         // "yes", "no", or "error"
    val replyText: String,
    val replySent: Boolean,
    val timestamp: Long,
    val errorMessage: String?,
    val synced: Boolean
)
```

## Error Handling

### Permission Denied
- Shows explanation dialog
- Lists specific denied permissions
- Allows continue with limited features
- Periodically re-requests critical permissions

### Network Errors
- Logs error locally
- Retries sync at next interval
- Graceful degradation (offline mode)
- Notifies user of sync failures

### Call Errors
- Logs error with reason
- Stores in database for sync
- Prevents crash on auto-answer failure
- Falls back to normal call handling

### SMS Errors
- Logs error locally
- Doesn't send reply if error
- Allows manual retry later
- Preserves SMS for sync

## Logging

All components use Android `Log` utility:
- **DEBUG**: Detailed flow and state changes
- **INFO**: Service start/stop, successful operations
- **WARN**: Permission issues, timeouts
- **ERROR**: Crashes, network failures, exceptions

Filter logs:
```bash
adb logcat com.voiceassistant.android:D
```

## Testing

### Unit Tests
```bash
./gradlew testDebug
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing
1. Grant all permissions on first launch
2. Test incoming call (should auto-answer)
3. Test incoming SMS (should request backend decision)
4. Check logs in database
5. Verify sync with backend

## Troubleshooting

### App Crashes on Startup
- Check AndroidManifest.xml is properly configured
- Ensure Hilt injection is working
- Check backend URL is valid

### Calls Not Monitoring
- Verify READ_PHONE_STATE permission granted
- Check PhoneStateService is running
- Review logs for error details

### SMS Not Auto-Replying
- Verify RECEIVE_SMS and SEND_SMS permissions
- Check backend /sms/decision endpoint is working
- Review SMSHandler logs

### Sync Not Working
- Verify INTERNET permission granted
- Check backend URL in AppConfig
- Review network logs and errors
- Check unsynced logs in database

## Dependencies

- **androidx.core:core-ktx**: Android core extensions
- **androidx.lifecycle**: Lifecycle management
- **androidx.work**: Background work scheduling
- **androidx.room**: Local database
- **com.google.dagger:hilt**: Dependency injection
- **com.squareup.retrofit2**: HTTP client
- **org.jetbrains.kotlinx:kotlinx-coroutines**: Async/await
- **org.jetbrains.kotlinx:kotlinx-serialization**: JSON serialization

## Future Enhancements

- [ ] Visual call state in UI
- [ ] Customizable SMS reply templates
- [ ] Call recording and playback
- [ ] Advanced contact filtering
- [ ] Time-based auto-answer schedules
- [ ] Call quality metrics
- [ ] SMS rate limiting
- [ ] Encrypted local storage

## License

[Add appropriate license]

## Support

For issues or questions, contact [support email]

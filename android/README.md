# VoiceCore Android Application

A complete Android voice assistant implementation with wake word detection, Google Speech API integration, and automatic call answering capabilities.

## Features

### 🎙️ Voice Processing
- **Wake Word Detection**: "Hey Jarvis" detection using Google Speech Recognition API
- **Speech-to-Text**: Real-time audio transcription with streaming support
- **Text-to-Speech**: High-quality voice synthesis using Android TTS
- **Voice Pipeline**: Complete audio processing pipeline from capture to playback

### 📞 Call Management
- **Automatic Call Answering**: Answers calls within 2 seconds using TelecomManager
- **Call Screening**: Intelligent call filtering with reputation checking
- **Graceful Fallbacks**: Multiple answer methods when permissions are limited
- **Call State Monitoring**: Real-time call status tracking and logging

### 🔋 Battery Optimization
- **Foreground Service**: Maintains service execution during Doze mode
- **Battery Exemptions**: Automatic request for optimization exemptions
- **Efficient Power Usage**: Optimized audio processing and wake word detection
- **Background Resilience**: Service survives app termination and device reboots

### 🛡️ Permission Handling
- **Comprehensive Permissions**: Microphone, phone, notifications, overlays
- **Runtime Permission Requests**: Modern Android permission flow
- **Permission Status UI**: Clear permission status and request interface
- **Graceful Degradation**: Service continues with limited functionality

### 🎨 Modern Android UI
- **Jetpack Compose**: Modern declarative UI framework
- **Material Design 3**: Latest Material Design guidelines
- **Real-time UI Updates**: Live service status and voice activity indicators
- **Accessibility**: Full accessibility support and screen reader compatibility

## Architecture

### 📁 Project Structure
```
android/app/src/main/java/com/assistant/voicecore/
├── application/           # Application class and initialization
├── di/                   # Dependency injection module
├── model/                # Data models and entities
├── network/              # API networking layer
├── repository/           # Data repository layer
├── service/              # Core services (Voice, Call, Screening)
├── ui/                   # UI components and screens
│   ├── components/       # Reusable UI components
│   └── MainActivity.kt   # Main application activity
├── viewmodel/            # State management and business logic
├── receiver/             # Broadcast receivers and event handling
└── MainActivity.kt       # Application entry point
```

### 🏗️ Component Architecture

#### VoiceCoreService
- **Foreground Service**: Maintains continuous operation
- **Wake Word Detection**: Continuous listening for "Hey Jarvis"
- **Audio Processing**: Real-time audio capture and analysis
- **State Management**: Service lifecycle and status tracking

#### CallAnsweringService
- **TelecomManager Integration**: System-level call management
- **Multi-Method Answering**: TelecomManager, telephony fallback, key simulation
- **2-Second Answer**: Meets call answering requirements
- **Call State Monitoring**: Tracks call lifecycle events

#### VoiceServiceViewModel
- **Speech Recognition**: Google Speech API integration
- **Text-to-Speech**: Android TTS with voice configuration
- **Backend Communication**: API calls to FastAPI backend
- **State Management**: Reactive state with Flow/MutableState

## Setup Instructions

### Prerequisites
- **Android Studio**: Latest stable version (Giraffe or newer)
- **Android SDK**: API level 26+ (Android 8.0)
- **Physical Device**: For testing call answering (emulator limitations)
- **Backend Server**: Running FastAPI server at `http://10.0.2.2:8000`

### Installation Steps

1. **Clone and Setup**
   ```bash
   # Open the android folder in Android Studio
   cd android
   ./gradlew clean build
   ```

2. **Configure Backend URL**
   Edit `android/local.properties`:
   ```properties
   BACKEND_BASE_URL=http://your-backend-ip:8000
   ```

3. **Build and Install**
   ```bash
   ./gradlew installDebug
   # Or use Android Studio Run button
   ```

4. **Grant Permissions**
   Launch app and grant required permissions:
   - Microphone access
   - Phone access  
   - Phone state
   - Notifications (Android 13+)
   - System overlays

## Usage Guide

### Starting the Voice Service

1. **Launch the App**: Open VoiceCore from app drawer
2. **Grant Permissions**: Tap "Grant" on all permission requests
3. **Start Service**: Tap "Start" button in service status card
4. **Verify Operation**: Look for "Listening for Hey Jarvis" status

### Testing Voice Features

1. **Wake Word Detection**:
   - Say "Hey Jarvis" clearly
   - App should show "Wake word detected" and start recording
   - Speak your request after the beep
   - Wait for AI response synthesis

2. **Manual Testing**:
   - Tap microphone icon to start recording manually
   - Speak clearly and wait for transcription
   - Check backend logs for API communication

### Testing Call Answering

1. **Setup Call Simulation** (requires physical device):
   - Enable developer options
   - Use phone testing tools to simulate incoming calls
   - Monitor app logs for call detection

2. **Manual Call Answer Test**:
   - Make a test call to the device
   - Monitor logs for automatic answering within 2 seconds
   - Check call state transitions in app UI

## Configuration

### Voice Settings
```kotlin
// In VoiceServiceViewModel.kt
companion object {
    private const val SAMPLE_RATE = 16000
    private const val WAKE_WORD = "hey jarvis"
    private const val RECORDING_TIMEOUT = 5000 // 5 seconds
}
```

### Call Answering Settings
```kotlin
// In CallAnsweringService.kt
companion object {
    private const val ANSWER_DELAY_MS = 2000L // 2 second requirement
    private const val MAX_ANSWER_ATTEMPTS = 3
}
```

### Backend API Configuration
```kotlin
// In NetworkModule.kt
@Provides
@Singleton
fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
    return Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8000/") // Android emulator localhost
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
}
```

## API Integration

### Backend Endpoints Used
- `POST /conversation/` - Process voice input and get AI responses
- `POST /sms/decision` - SMS decision making for call screening
- `GET /history/{userId}/conversations` - Recent conversation history
- `GET /health` - Backend health check

### Request/Response Format
```kotlin
// Conversation Request
data class ConversationRequest(
    val userId: Long,
    val text: String
)

// Conversation Response  
data class ConversationResponse(
    val id: Long,
    val userId: Long,
    val inputText: String,
    val gptResponse: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val processingTimeMs: Double,
    val modelUsed: String,
    val createdAt: String
)
```

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] App launches successfully
- [ ] All permissions can be granted
- [ ] Voice service starts and shows "Listening" status
- [ ] Wake word detection responds to "Hey Jarvis"
- [ ] Speech transcription works
- [ ] Backend API communication succeeds
- [ ] TTS synthesizes responses
- [ ] Service continues in background
- [ ] Auto-start on boot works (if enabled)
- [ ] Call answering responds within 2 seconds

## Troubleshooting

### Common Issues

1. **Microphone Permission Denied**
   - Go to Settings > Apps > VoiceCore > Permissions
   - Enable Microphone permission
   - Restart app

2. **Backend Connection Failed**
   - Verify backend server is running on correct IP
   - Check network security config allows HTTP for development
   - Ensure emulator can reach host machine

3. **Wake Word Not Detected**
   - Test with clear pronunciation: "Hey Jarvis"
   - Check microphone is working in other apps
   - Increase microphone sensitivity in settings

4. **Call Answering Not Working**
   - Requires physical device, not emulator
   - Grant ANSWER_PHONE_CALLS permission
   - Check TelecomManager permissions in Android settings

### Debug Logging

Enable debug logging in `local.properties`:
```properties
LOG_LEVEL=DEBUG
```

View logs:
```bash
adb logcat | grep VoiceCore
```

## Permissions Reference

| Permission | Purpose | Required |
|------------|---------|----------|
| `RECORD_AUDIO` | Voice input and wake word detection | ✅ Required |
| `CALL_PHONE` | Making and managing calls | ✅ Required |
| `READ_PHONE_STATE` | Detecting incoming calls | ✅ Required |
| `ANSWER_PHONE_CALLS` | Automatic call answering | ✅ Required |
| `POST_NOTIFICATIONS` | Service status notifications | ✅ Required (Android 13+) |
| `SYSTEM_ALERT_WINDOW` | System integration overlays | ⚠️ Optional |
| `FOREGROUND_SERVICE` | Background service operation | ✅ Required |
| `WAKE_LOCK` | Prevent device sleep | ✅ Required |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after reboot | ⚠️ Optional |

## Development Notes

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Prefer immutable data structures

### Architecture Guidelines
- Single Responsibility Principle
- Dependency injection with Hilt
- Reactive programming with Flow/StateFlow
- Proper error handling and logging

### Performance Considerations
- Minimize main thread operations
- Use background threads for network and audio processing
- Implement proper cleanup in ViewModel.onCleared()
- Optimize audio buffer sizes for performance

## Contributing

1. Follow existing code patterns and conventions
2. Add unit tests for new features
3. Update documentation for API changes
4. Test on multiple Android versions and devices
5. Ensure proper permission handling

## License

MIT License - See LICENSE file for details.
# Fluxer Android Client

A native Android client for Fluxer (an open-source Discord alternative), built with bulletproof authentication handling and a sharp, gaming-inspired UI aesthetic.

## 🎯 Key Features

### Bulletproof Authentication
The app solves the critical authentication challenges that plagued previous Swift attempts:

- **Secure CookieJar** (`SecureCookieStorage`): Uses `EncryptedSharedPreferences` to persistently store HttpOnly cookies (including `fluxer_session`) with AES-256 encryption.
- **Automatic Token Refresh** (`AuthAuthenticator`): An OkHttp `Authenticator` that seamlessly refreshes expired access tokens using the refresh token, retrying failed requests automatically.
- **CSRF Protection** (`CsrfInterceptor`): An OkHttp `Interceptor` that fetches and attaches a CSRF token (`X-CSRF-Token`) to all state-modifying requests (POST, PATCH, DELETE).

### Offline First
- **Local Database**: Messages are cached locally using Room, allowing users to view past conversations even when offline.
- **Offline Queue**: Messages sent while offline are queued and automatically sent when the connection is restored.

### Modern Chat Experience
- **Image Attachments**: View images directly in the chat.
- **Infinite Scroll**: Messages are paginated, allowing for efficient loading of long chat histories.
- **Message Search**: Quickly find messages within a channel.
- **Error Handling**: A clear error message with a retry option is shown when the network is unreliable.

- **CSRF Token Management** (`CsrfInterceptor`): Automatically extracts CSRF tokens from cookies/headers and injects them into state-changing requests
- **Token Refresh** (`AuthAuthenticator`): Handles 401 responses by attempting session refresh before requiring re-login
- **Automatic Cookie Attachment**: All requests automatically include the correct cookies thanks to OkHttp's CookieJar interface

### Architecture
- **Clean MVVM**: Fully decoupled UI layer with ViewModels, Repositories, and Data layer
- **Dependency Injection**: Hilt for clean dependency management
- **Reactive Streams**: Kotlin Flow for state management and real-time updates
- **Repository Pattern**: Abstracted data sources (REST API + WebSocket Gateway)

### Real-time Messaging
- **WebSocket Gateway**: OkHttp WebSocket with auto-reconnection, heartbeat, and session resumption
- **Event-driven Updates**: Gateway events automatically update local caches
- **Optimistic UI**: Messages appear instantly while syncing in background

### UI/UX - Persona 5 Inspired
- **Dark Mode First**: Deep blacks (`#0D0D0D`) with high contrast
- **Phantom Red Accent**: Signature red (`#E63946`) for primary actions
- **Sharp Angles**: Rectangular buttons, minimal rounding, slash-shaped elements
- **Gaming Aesthetic**: Bold typography, dramatic shadows, status indicators

## 🏗️ Project Structure

```
com.fluxer.client/
├── data/
│   ├── local/
│   │   └── SecureCookieStorage.kt      # Encrypted cookie persistence
│   ├── remote/
│   │   ├── FluxerApiService.kt         # Retrofit REST API
│   │   ├── GatewayWebSocketManager.kt  # WebSocket connection
│   │   ├── CsrfInterceptor.kt          # CSRF token handling
│   │   └── AuthAuthenticator.kt        # 401 refresh handler
│   ├── model/
│   │   ├── AuthModels.kt               # Login/Register/Auth DTOs
│   │   ├── MessageModels.kt            # Message/Channel DTOs
│   │   └── GatewayModels.kt            # WebSocket event DTOs
│   └── repository/
│       ├── AuthRepository.kt           # Auth operations
│       └── ChatRepository.kt           # Chat operations
├── di/
│   └── NetworkModule.kt                # Hilt DI configuration
├── ui/
│   ├── theme/
│   │   ├── Color.kt                    # Persona 5 color palette
│   │   ├── Theme.kt                    # Dark theme configuration
│   │   ├── Type.kt                     # Typography
│   │   └── Shape.kt                    # Custom shapes
│   ├── components/
│   │   ├── FluxerButton.kt             # Gaming-style buttons
│   │   ├── FluxerTextField.kt          # Sharp input fields
│   │   ├── MessageBubble.kt            # Chat message bubbles
│   │   └── ServerSidebar.kt            # Server/channel navigation
│   ├── screens/
│   │   ├── LoginScreen.kt              # Auth UI
│   │   └── ChatScreen.kt               # Main chat UI
│   └── viewmodel/
│       ├── AuthViewModel.kt            # Auth state management
│       └── ChatViewModel.kt            # Chat state management
└── MainActivity.kt
```

## 🔐 Authentication Flow

### Login Process
1. User enters credentials in `LoginScreen`
2. `AuthViewModel` calls `AuthRepository.login()`
3. Retrofit makes POST to `/api/auth/login`
4. Server responds with `Set-Cookie: fluxer_session=...; HttpOnly`
5. `SecureCookieStorage` intercepts and **encrypts** the cookie
6. CSRF token extracted from response and cached
7. Gateway WebSocket connects with session cookie
8. User state flows to UI, navigation to `ChatScreen`

### Request Flow (with Auth)
```
Request → CsrfInterceptor (adds X-CSRF-Token header)
        → CookieJar (adds fluxer_session cookie)
        → Server
        
Response → CookieJar (saves new cookies)
         → CsrfInterceptor (extracts new CSRF token)
         → Repository
```

### 401 Handling
1. Response returns 401
2. `AuthAuthenticator` intercepts
3. Attempts `POST /api/auth/refresh`
4. If successful: retries original request with new cookies
5. If failed: clears session, triggers re-login

## 🎨 UI Design System

### Colors
| Token | Hex | Usage |
|-------|-----|-------|
| PhantomRed | #E63946 | Primary buttons, accents |
| VelvetBlack | #0D0D0D | Background |
| VelvetDark | #141414 | Cards, panels |
| VelvetMid | #1A1A1A | Input fields |
| TextPrimary | #FFFFFF | Main text |
| TextSecondary | #B3B3B3 | Secondary text |

### Components
- **FluxerButton**: Sharp rectangular buttons with red accent
- **SlashButton**: Diagonal-edged buttons for primary CTAs
- **FluxerTextField**: Bordered inputs with red focus indicator
- **MessageBubble**: Asymmetric rounded corners (Persona 5 style)

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Configuration
1. Update `FLUXER_BASE_URL` in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "FLUXER_BASE_URL", """"https://your-fluxer-instance.com"""
```

2. Update `FLUXER_WS_URL` for WebSocket:
```kotlin
buildConfigField("String", "FLUXER_WS_URL", """"wss://your-fluxer-instance.com"""
```

### Build
```bash
./gradlew assembleDebug
```

## 🔧 Technical Highlights

### Secure Cookie Storage
```kotlin
// Uses EncryptedSharedPreferences with AES-256-GCM
class SecureCookieStorage(context: Context) : CookieJar {
    private val encryptedPrefs = EncryptedSharedPreferences.create(...)
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Encrypts and stores HttpOnly cookies
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // Decrypts and returns cookies for domain
    }
}
```

### CSRF Protection
```kotlin
class CsrfInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        // Adds X-CSRF-Token header to POST/PUT/DELETE
        if (requiresCsrf(request.method)) {
            builder.header("X-CSRF-Token", getCsrfToken())
        }
    }
}
```

### WebSocket Gateway
- Auto-reconnection with exponential backoff
- Heartbeat/ping-pong keepalive
- Session resumption on reconnect
- Event-driven architecture

## 📱 Screenshots

*(To be added once UI is finalized)*

## 📝 License

MIT License - See LICENSE file for details

## 🙏 Acknowledgments

- Persona 5 for the UI inspiration
- OkHttp team for the excellent networking library
- Jetpack Compose team for the modern UI toolkit

# Fluxer Android Client

A native Android client for the [Fluxer](https://fluxer.app/) chat platform, built with Jetpack Compose, Hilt, OkHttp, and a premium dark UI.

> This repository is named `flukavike-android`; the Android app is branded **Fluxer** (`com.fluxer.client`).

---

## Why a native Android client?

Fluxer already has an official app, so why build a native Android client from scratch?

### Goals

- **Deeper Android integration** — written directly against the Android SDK and Jetpack Compose, this client can adopt new platform features (large-screen layouts, foldables, desktop/DeX windowing, per-app language preferences, themed icons, etc.) as soon as Google ships them, without waiting for a cross-platform framework to catch up.
- **Different UX priorities** — a third-party client can experiment with alternative navigation, themes, notification behavior, and power-user features that the official app may not prioritize.
- **Native performance & battery life** — no Flutter engine or bridge overhead; foreground services, WebSocket keepalives, push routing, and background sync are implemented with direct Android lifecycle controls, which tends to yield better idle battery behavior and more predictable cpu ask scheduling.
- **Tighter system integration** — native share sheets, notification channels, app shortcuts, widgets, intents/deep-links, biometric unlock, and accessibility hooks all plug directly into the OS.
- **Smaller runtime footprint** — the APK only ships the libraries the app actually uses, rather than bundling a cross-platform runtime.

### Trade-offs

- **Maintenance burden** — every API change on Fluxer’s side has to be adapted here manually, and this client is Android-only.
- **Feature parity** — new official features may take time to land, and some platform-exclusive capabilities may never be replicated.
- **API stability risk** — unofficial/self-built clients depend on server APIs that can change without notice.
- **Resource constraints** — a smaller team means slower iteration than a first-party product backed by the platform vendor.

In short, this project is for users who want a more Android-native, customizable, and battery-conscious Fluxer experience — even if it means living on the bleeding edge.

---

## !! DO NOT EDIT — CRITICAL SYSTEMS !!

Several core systems in this app were broken, debugged, and fixed through extensive testing against the live Fluxer API.
**Do not refactor, restructure, or "improve" them unless a bug is explicitly traced there.**

The affected systems include:

- **Authentication flow** — cookie/token storage, session refresh, CSRF handling, and MFA/WebAuthn paths.
- **Network configuration** — OkHttp client setup, interceptor ordering, timeouts, and retry policy.
- **API endpoint paths and data models** — exact REST paths and serializers verified against the live API.
- **Captcha loading** — callback-based initialization.
- **Channel/guild loading logic** — verified fetch paths and UI state updates.

The specific files involved change as the codebase evolves. **See [`CLAUDE.md`](CLAUDE.md) for the current list of files and the full details on every fix.**

---

## 🎯 Features

### Authentication & Security
- **Cookie + token auth** with encrypted `SecureCookieStorage` (AES-256 via `EncryptedSharedPreferences`).
- **Automatic session refresh** via `AuthAuthenticator` on 401 responses.
- **CSRF protection** via `CsrfInterceptor` for all state-changing requests.
- **hCaptcha support** with `?onload=` callback integration.
- **WebAuthn MFA** flow for passwordless second-factor login.
- **Biometric app lock** via `BiometricLockManager`.

### Messaging
- Real-time text channels and direct messages backed by a WebSocket gateway.
- Infinite scroll pagination for message history.
- In-channel and global search.
- Reactions, edits, deletes, and typing indicators.
- Rich attachments with image previews (Coil + GIF support).

### Voice & Calls
- Voice channels powered by [LiveKit](https://livekit.io/).
- Direct message calls with native call-style UI and foreground service.

### Friends, Servers & Profiles
- Friend list, requests, and user relationships.
- Server/guild navigation with channel lists.
- User profiles with avatar support and status indicators.
- Server management and invite handling.

### Notifications
- Firebase Cloud Messaging (FCM) for push notifications.
- [UnifiedPush](https://unifiedpush.org/) support as a FCM-free alternative.
- Rich notification routing to the correct channel, DM, or call.

### Offline & Persistence
- Local caching with Room (`MessageEntity`, `ChannelEntity`, `GuildEntity`, `PendingMessageEntity`).
- Pending message queue for offline sending.
- DataStore-backed user preferences and instance configuration.

### UI / UX
- Premium dark theme with Phantom Red accent.
- Compose Material 3 with custom typography, shapes, and spacing.
- Deep-linking support (`fluxer://`, `https://web.fluxer.app/...`, canary variants).
- Settings hub: appearance, notifications, account, storage, language, accessibility.

---

## 🏗️ Project Structure

```
com.fluxer.client/
├── data/
│   ├── local/                 # Room DB, DataStore, encrypted cookie/token storage
│   ├── model/                 # DTOs for auth, messages, gateway, voice, calls, etc.
│   ├── paging/                # Paging 3 sources
│   ├── remote/                # Retrofit/OAuth/WebAuthn services + interceptors + WebSocket
│   └── repository/            # Auth, Chat, Friends, Guild, Settings, Notifications, etc.
├── di/                        # Hilt modules (network, database, features)
├── navigation/                # Route definitions and deep-link parsing
├── service/                   # FCM, UnifiedPush, call foreground service, broadcast receiver
├── ui/
│   ├── components/            # Reusable Compose components
│   ├── screens/               # Login, Chat, Settings, Friends, Voice, Profile, etc.
│   ├── theme/                 # Colors, typography, shapes, spacing
│   └── viewmodel/             # ViewModels per screen/feature
├── util/                      # Helpers: CDN URLs, notifications, UnifiedPush, Result, etc.
├── FluxerApplication.kt
└── MainActivity.kt
```

---

## 🛠️ Tech Stack

| Layer | Libraries |
|-------|-----------|
| UI | Jetpack Compose (BOM 2024.02.00), Material 3, Navigation Compose |
| DI | Hilt 2.50 + KSP |
| Networking | OkHttp 4.12, Retrofit 2.9, Kotlinx Serialization |
| Persistence | Room 2.6.1, DataStore 1.0.0, EncryptedSharedPreferences |
| Real-time | OkHttp WebSocket (`GatewayWebSocketManager`) |
| Images | Coil 2.6.0 (+ GIF) |
| Voice/Video | LiveKit Android SDK 2.5.0 |
| Push | Firebase Messaging, UnifiedPush connector |
| Auth | WebAuthn/FIDO2, Biometric 1.1.0 |
| Async | Kotlin Coroutines, Kotlin Flow, Paging 3 |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Configuration

Base URLs and hCaptcha site key are defined in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    buildConfigField("String", "FLUXER_BASE_URL", """"https://web.fluxer.app/"""")
    buildConfigField("String", "FLUXER_WS_URL", """"wss://gateway.fluxer.app"""")
    buildConfigField("String", "HCAPTCHA_SITE_KEY", """"your-site-key"""")
}
```

Override them in `buildTypes.debug` for a local/self-hosted instance.

### Firebase / Push

A placeholder `google-services.json` is included for builds. For real push notifications, replace it with a Firebase project config or configure UnifiedPush.

### Build

```bash
./gradlew assembleDebug
```

Install to a device or emulator:

```bash
./gradlew installDebug
```

---

## 🔐 Authentication Flow

1. User enters credentials (and solves captcha if required) in `LoginScreen`.
2. `AuthViewModel` calls `AuthRepository.login()`.
3. Retrofit POSTs to `/api/auth/login`; the server sets the `fluxer_session` HttpOnly cookie.
4. `SecureCookieStorage` encrypts and persists the cookie.
5. `AuthTokenStorage` persists the access token separately.
6. CSRF token is fetched from `/api/auth/csrf` and cached.
7. Gateway WebSocket connects using the session cookie.
8. Auth state flows to the UI and navigates to the main chat shell.

### Authenticated request pipeline

```
Request  → BaseUrlOverrideInterceptor
         → AuthInterceptor (Authorization header)
         → CsrfInterceptor (X-CSRF-Token for mutating methods)
         → CookieJar (fluxer_session cookie)
         → Server

Response → CookieJar (save updated cookies)
         → CsrfInterceptor (extract new CSRF token)
         → Repository
```

### 401 handling
1. Response returns 401.
2. `AuthAuthenticator` intercepts.
3. Attempts `POST /api/auth/refresh`.
4. On success: retries the original request with refreshed cookies.
5. On failure: clears the session and returns to `LoginScreen`.

---

## 🎨 Design System

### Colors
| Token | Hex | Usage |
|-------|-----|-------|
| PhantomRed | `#E15463` | Primary buttons, accents |
| PhantomRedDark | `#B83A4D` | Pressed states |
| VelvetBlack | `#0F1115` | Background |
| VelvetDark | `#151922` | Cards, panels |
| VelvetMid | `#1C2230` | Input fields |
| TextPrimary | `#F4F7FB` | Main text |
| TextSecondary | `#CCD3DF` | Secondary text |
| TextMuted | `#8B96A8` | Tertiary text |
| SuccessGreen | `#55C59A` | Online / success |
| AlertYellow | `#F3C969` | Away / warnings |
| DndRed | `#EA646B` | Do-not-disturb |

### Components
- `FluxerButton` / `SlashButton` — primary action buttons.
- `FluxerTextField` — bordered inputs with red focus indicator.
- `MessageBubble` / `DiscordMessageBubble` / `EnhancedMessageBubble` — message rendering variants.
- `ServerSidebar` — server and DM navigation rail.

---

## 🧪 Testing

Unit test dependencies are configured:

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.9")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

Run tests:

```bash
./gradlew test
```

---

## 🗺️ Roadmap

See [`ROADMAP.md`](ROADMAP.md) for planned and completed features.

---

## 📝 License

MIT License — See LICENSE file for details.

## 🙏 Acknowledgments

- [Fluxer](https://fluxer.app/) team and community
- OkHttp / Retrofit / Coil / LiveKit maintainers
- Jetpack Compose team

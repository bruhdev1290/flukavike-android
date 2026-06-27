# Fluxer Android Client - Development Roadmap

Last updated: 2026-06-27

---

## ✅ COMPLETED

### Infrastructure & Build
- [x] Gradle wrapper setup (`gradle/wrapper/`)
- [x] Hilt dependency injection (`AppModule`, `NetworkModule`, `DatabaseModule`, `FeaturesModule`)
- [x] Room database with entities, DAOs, and DI (`AppDatabase`, `DatabaseEntities`, `DatabaseDaos`)
- [x] DataStore preferences (`AppPreferencesStore`, `DataStoreExt`)
- [x] Instance configuration store (`InstanceConfigStore`, `InstanceConfig`)
- [x] BuildConfig fields: base URL, WebSocket URL, hCaptcha site key
- [x] Release build with minify + resource shrinking + ProGuard rules

### Auth System (HANDS OFF — see CLAUDE.md)
- [x] Login with session cookie + auth token
- [x] CSRF token interception
- [x] Secure cookie storage (EncryptedSharedPreferences)
- [x] Auth token storage
- [x] hCaptcha widget (callback-based, not polling)
- [x] IP auth required flow
- [x] WebAuthn support (`WebAuthnService`)
- [x] Auth authenticator + interceptor chain

### Networking
- [x] OkHttp client with tuned timeouts (20s call timeout, retryOnConnectionFailure=false)
- [x] Interceptor stack: baseUrlOverride → auth → csrf → clientProperties
- [x] Retrofit + kotlinx-serialization converter
- [x] Gateway WebSocket manager (`GatewayWebSocketManager`, `GatewayModels`)
- [x] Multiple API services: `FluxerApiService`, `FriendsApiService`, `AvatarApiService`, `UploadApiService`, `InviteApiService`, `PinApiService`, `ReadStateApiService`, `GuildManagementApiService`, `GuildMembersApiService`
- [x] `NetworkRetryInterceptor`, `BaseUrlOverrideInterceptor`, `ClientPropertiesInterceptor`
- [x] Skip-auth policy for unauthenticated endpoints

### Messaging & Chat
- [x] Guild/server list with separate channel fetch (channels not embedded in guild objects)
- [x] Message pagination via Paging 3 (`MessagePagingSource`)
- [x] Message caching via Room
- [x] Message bubbles: `MessageBubble`, `EnhancedMessageBubble`, `DiscordMessageBubble`
- [x] Image/attachment support (`ChatAttachment`, `UploadApiService`)
- [x] Coil image loading with GIF support
- [x] Pinned messages (`PinApiService`)
- [x] Read state tracking (`ReadStateApiService`)
- [x] Starred channels (`StarredChannelsScreen`, `StarredChannelsViewModel`)

### Voice & Calls
- [x] LiveKit-based voice channel implementation (`LiveKitVoiceManager`)
- [x] Voice channel screen and overlay (`VoiceChannelScreen`, `VoiceChannelOverlay`)
- [x] Active call screen (`ActiveCallScreen`, `CallOverlay`)
- [x] Telecom `ConnectionService` integration (`FluxerCallConnectionService`)
- [x] Call broadcast receiver (`CallBroadcastReceiver`)
- [x] Voice message player (`VoiceMessagePlayer`)
- [x] Voice and call data models (`VoiceModels`, `CallModels`)

### Push Notifications
- [x] Firebase Cloud Messaging (`FluxerMessagingService`)
- [x] UnifiedPush support (`FluxerUnifiedPushReceiver`, `UnifiedPushManager`)
- [x] Notification handler and entry point (`FluxerNotificationHandler`, `FluxerNotificationEntryPoint`)
- [x] Notification helper utilities (`NotificationHelper`, `NotificationPreferences`)
- [x] Notification center screen (`NotificationCenterScreen`, `NotificationCenterViewModel`)
- [x] Per-channel notification settings (`NotificationSettingsScreen`, `NotificationSettingsViewModel`)

### Friends & Relationships
- [x] Friends screen (`FriendsScreen`, `FriendsViewModel`)
- [x] Friends API service and repository (`FriendsApiService`, `FriendsRepository`)
- [x] Relationship models (`RelationshipModels`)

### Direct Messages
- [x] DM inbox screen (`MessagesScreen`, `MessagesViewModel`)
- [x] DM conversation via chat screen

### Guild Management
- [x] Guild management repository and API service (`GuildManagementRepository`, `GuildManagementApiService`)
- [x] Guild members API (`GuildMembersApiService`)
- [x] Invite system (`InviteApiService`, `InviteModels`)
- [x] Server sidebar with context menu (`ServerSidebar`, `ServerContextMenu`)

### Profile & Account
- [x] Profile screen with editing (`ProfileScreen`, `ProfileViewModel`, `ProfileRepository`)
- [x] Profile models (`ProfileModels`)
- [x] Account settings screen (`AccountScreen`)

### Settings & Preferences
- [x] Settings screen with sections (`SettingsScreen`, `SettingsViewModel`)
- [x] Appearance settings (`AppearanceScreen`)
- [x] Language settings (`LanguageScreen`)
- [x] Accessibility settings screen (`AccessibilityScreen`)
- [x] Storage management screen (`StorageScreen`)
- [x] About and support screens (`AboutScreen`, `SupportScreen`)
- [x] App preferences view model (`AppPreferencesViewModel`)

### UI & Navigation
- [x] App chrome shell with bottom nav (`AppChrome`, `ShellViewModel`)
- [x] Compose navigation with typed routes (`FluxerRoutes`, `NavigationRepository`)
- [x] Home state management (`HomeStateRepository`)
- [x] Feature bottom sheets (`FeatureSheets`)
- [x] Reusable components: `FluxerButton`, `FluxerTextField`, `ErrorState`
- [x] CDN URL builder (`CdnUrlBuilder`)

### Security
- [x] Biometric unlock (`BiometricLockManager`)
- [x] EncryptedSharedPreferences for session/token storage
- [x] ProGuard/R8 enabled in release builds

### Testing Infrastructure
- [x] JUnit 4, MockK, kotlinx-coroutines-test, MockWebServer dependencies added
- [x] Instrumented test runner configured

---

## 🟡 IN PROGRESS / PARTIALLY DONE

### Image Attachments
- [x] Upload API service (`UploadApiService`)
- [x] `ChatAttachment` composable for rendering
- [ ] File picker integration (system file chooser)
- [ ] Image compression before upload
- [ ] Upload progress tracking
- [ ] Fullscreen image viewer (tap to expand)

### Error Handling
- [x] `ErrorState` composable for error display
- [ ] Consistent error snackbars across all screens
- [ ] Exponential backoff for failed API calls
- [ ] Offline mode banner
- [ ] Specific HTTP error code handling (401 auto-logout, 429 rate limit countdown)

---

## 🔴 REMAINING

### Unit & Integration Tests
**Effort: 6-8 hours**
- [ ] `AuthRepositoryTest` — login success/failure, logout
- [ ] `SecureCookieStorageTest` — persistence, encryption, expiration
- [ ] `ChatRepositoryTest` — WebSocket events, message caching, offline queue
- [ ] `AuthViewModel` state machine tests
- [ ] `ChatViewModel` message sending tests
- [ ] Instrumented tests for critical UI flows (login → chat)

### Search
**Effort: 2-3 hours**
- [ ] Message search API endpoint wired up
- [ ] Search UI in chat screen (search bar, results list)

### Push Notifications — Server Registration
**Effort: 1-2 hours**
- [ ] Send FCM/UnifiedPush token to Fluxer server on login and token refresh
- [ ] Confirm server-side token storage and delivery

### Security Hardening
**Effort: 2-3 hours**
- [ ] Certificate pinning for API and WebSocket connections
- [ ] Screenshot prevention flag for DM screens (`FLAG_SECURE`)
- [ ] Root/emulator detection (optional, low priority)
- [ ] Audit for hardcoded secrets or debug logs in release builds

### Performance
**Effort: 2-3 hours**
- [ ] Baseline profiles for faster cold start
- [ ] Review recomposition hot spots with Android Studio profiler
- [ ] Tune R8 rules for release APK size

---

## 📋 Build & Run

```bash
# Open in Android Studio and sync, or:
./gradlew installDebug

# Generate Room code after entity changes:
./gradlew kspDebugKotlin

# Release build:
./gradlew assembleRelease
```

---

## 📝 Notes

- Voice channels use **LiveKit**, not raw WebRTC
- Push notifications support both **FCM** and **UnifiedPush** (ntfy-compatible)
- Auth system is stable and load-bearing — see `CLAUDE.md` for the full list of hands-off files
- `minSdk 26` (Android 8.0) — biometric and `ConnectionService` both require this

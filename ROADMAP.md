# Fluxer Android Client - Development Roadmap

## 🔴 CRITICAL - Must Have (Do These First)

### 1. Gradle Wrapper Setup
**Priority: BLOCKING**  
**Effort: 5 minutes**

Without these files, the project cannot be built by anyone else (including CI/CD).

Files needed:
```
gradle/
├── wrapper/
│   ├── gradle-wrapper.jar          (download from gradle.org)
│   └── gradle-wrapper.properties   (specify gradle 8.2)
├── gradlew                         (Unix wrapper script)
└── gradlew.bat                     (Windows wrapper script)
```

Quick setup:
```bash
# In project root:
gradle wrapper --gradle-version 8.2
```

---

### 2. Local Database (Room) - Offline Support
**Priority: HIGH**  
**Effort: 4-6 hours**

Currently messages disappear when app restarts. Need persistent storage.

#### Implementation Checklist:
- [ ] Add Room dependencies to `app/build.gradle.kts`
- [ ] Create `@Entity` classes:
  - `MessageEntity` (cached messages)
  - `ChannelEntity` (cached channels)
  - `GuildEntity` (cached servers)
  - `PendingMessageEntity` (offline queue)
- [ ] Create `@Dao` interfaces:
  - `MessageDao` - insert, get by channel, delete old
  - `ChannelDao` - upsert, get by guild
  - `PendingMessageDao` - queue for offline sending
- [ ] Create `AppDatabase` class
- [ ] Add to DI module (`DatabaseModule.kt`)
- [ ] Update `ChatRepository` to:
  - Check database first, then fetch from API
  - Store new messages to database
  - Sync pending messages when online

#### Dependencies to add:
```kotlin
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

---

### 3. Push Notifications (FCM)
**Priority: HIGH**  
**Effort: 3-4 hours**

Essential for real chat apps - users need notifications when mentioned/DMed while app is backgrounded.

#### Implementation Checklist:
- [ ] Create Firebase project at https://console.firebase.google.com
- [ ] Add `google-services.json` to `app/` directory
- [ ] Add FCM dependencies to `app/build.gradle.kts`
- [ ] Add `google-services` plugin to build files
- [ ] Create `FluxerMessagingService` extending `FirebaseMessagingService`
  - Override `onMessageReceived()` to show notifications
  - Override `onNewToken()` to send token to server
- [ ] Create notification channels (Android 8.0+)
  - "Direct Messages" (high priority)
  - "Mentions" (high priority)  
  - "General" (default priority)
- [ ] Add notification permission request (Android 13+)
- [ ] Update `AndroidManifest.xml` with service declaration
- [ ] Add API call to register FCM token with Fluxer server

#### Dependencies to add:
```kotlin
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-messaging")
```

---

## 🟡 IMPORTANT - Should Have

### 4. Image Loading & Attachments
**Priority: MEDIUM**  
**Effort: 3-4 hours**

Users expect to share images in chat.

#### Implementation Checklist:
- [ ] Add Coil for image loading (already in deps, verify configured)
- [ ] Add file picker integration
- [ ] Update `FluxerApiService` with multipart upload:
  ```kotlin
  @Multipart
  @POST("/api/channels/{channelId}/attachments")
  suspend fun uploadAttachment(
      @Path("channelId") channelId: String,
      @Part file: MultipartBody.Part
  ): Response<Attachment>
  ```
- [ ] Create `FileUploadManager` for:
  - Image compression before upload
  - Upload progress tracking
  - Retry on failure
- [ ] Update `MessageBubble` to show:
  - Image thumbnails
  - Loading state
  - Error state with retry
- [ ] Add image viewer (fullscreen tap-to-view)

#### Dependencies:
```kotlin
// Already included but verify:
implementation("io.coil-kt:coil-compose:2.5.0")
```

---

### 5. Unit & Integration Tests
**Priority: MEDIUM**  
**Effort: 6-8 hours**

Essential for reliability and confident refactoring.

#### Implementation Checklist:
- [ ] Add test dependencies:
  ```kotlin
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
  testImplementation("io.mockk:mockk:1.13.9")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
  ```
- [ ] Create `AuthRepositoryTest`:
  - Test login success stores cookies
  - Test login failure returns error
  - Test logout clears session
- [ ] Create `SecureCookieStorageTest`:
  - Test cookie persistence
  - Test encryption/decryption
  - Test expiration handling
- [ ] Create `ChatRepositoryTest`:
  - Mock WebSocket events
  - Test message caching
  - Test offline queue
- [ ] Create ViewModel tests:
  - `AuthViewModel` state transitions
  - `ChatViewModel` message sending
- [ ] Add instrumented tests for critical UI flows

---

### 6. Search & Pagination
**Priority: MEDIUM**  
**Effort: 2-3 hours**

Current implementation loads all messages - won't scale.

#### Implementation Checklist:
- [ ] Update `FluxerApiService.getMessages()` - already supports `before` param
- [ ] Update `ChatRepository` with paginated loading:
  ```kotlin
  fun getMessagesPaginated(channelId: String): Flow<PagingData<Message>>
  ```
- [ ] Add Paging 3 dependency and implementation
- [ ] Update `ChatScreen` with infinite scroll
- [ ] Add search API endpoint:
  ```kotlin
  @GET("/api/channels/{channelId}/messages/search")
  suspend fun searchMessages(
      @Path("channelId") channelId: String,
      @Query("q") query: String
  ): Response<List<Message>>
  ```
- [ ] Add search UI to chat screen

#### Dependencies:
```kotlin
implementation("androidx.paging:paging-runtime-ktx:3.2.1")
implementation("androidx.paging:paging-compose:3.2.1")
```

---

### 7. Error Handling & Retry Logic
**Priority: MEDIUM**  
**Effort: 2-3 hours**

Better UX when network is flaky.

#### Implementation Checklist:
- [ ] Add `SnackbarHost` to all screens for error messages
- [ ] Implement exponential backoff for API retries
- [ ] Add "Retry" buttons for failed operations
- [ ] Create offline mode banner
- [ ] Handle specific error codes:
  - 401: Session expired → auto-logout
  - 403: Show permission error
  - 429: Rate limit with countdown
  - 500+: Server error with retry

---

## 🟢 NICE TO HAVE - Polish Features

### 8. User Preferences (DataStore)
**Priority: LOW**  
**Effort: 2-3 hours**

Settings screen for customization.

#### Implementation Checklist:
- [ ] Create `UserPreferences` data class
- [ ] Create `PreferencesRepository` using DataStore
- [ ] Create Settings screen with:
  - Theme toggle (Dark/Light/System)
  - Notification settings per channel type
  - Message font size
  - Compact mode toggle
- [ ] Persist settings across app restarts
- [ ] Apply settings to UI components

---

### 9. Voice Channels (WebRTC)
**Priority: LOW**  
**Effort: 8-12 hours**

Complex feature - defer until text chat is solid.

#### Implementation Checklist:
- [ ] Add WebRTC dependency
- [ ] Implement voice channel UI (different from text)
- [ ] Handle audio permissions
- [ ] Implement voice activity detection
- [ ] Add mute/deafen controls
- [ ] Handle audio focus (respect calls/other apps)

#### Dependencies:
```kotlin
implementation("org.webrtc:google-webrtc:1.0.x")
```

---

### 10. Accessibility Improvements
**Priority: LOW**  
**Effort: 3-4 hours**

Make app usable for everyone.

#### Implementation Checklist:
- [ ] Add `contentDescription` to all icons
- [ ] Add `contentDescription` to avatars (username)
- [ ] Ensure minimum touch targets (48dp)
- [ ] Add semantic properties to message list
- [ ] Test with TalkBack screen reader
- [ ] Support high contrast mode
- [ ] Respect system font size settings

---

### 11. Performance Optimizations
**Priority: LOW**  
**Effort: 2-3 hours**

Polish for smooth 60fps.

#### Implementation Checklist:
- [ ] Add baseline profiles for faster startup
- [ ] Implement message list virtualization (already using LazyColumn)
- [ ] Add image caching strategy
- [ ] Optimize recomposition with `remember` and keys
- [ ] Profile with Android Studio profiler
- [ ] Reduce APK size (R8 rules, resource shrinking)

---

### 12. Security Hardening
**Priority: LOW**  
**Effort: 2-3 hours**

Extra security layers.

#### Implementation Checklist:
- [ ] Add certificate pinning for API calls
- [ ] Implement root detection
- [ ] Add screenshot prevention option (for sensitive DMs)
- [ ] Obfuscate sensitive strings in release builds
- [ ] Add biometric unlock option
- [ ] Audit for hardcoded secrets

---

## 📋 Quick Start Commands

### After cloning this repo:

```bash
# 1. Setup Gradle wrapper (CRITICAL)
gradle wrapper --gradle-version 8.2

# 2. Create local.properties
sdk.dir=/path/to/your/Android/Sdk

# 3. Open in Android Studio and sync

# 4. Run on device/emulator
./gradlew installDebug
```

### Adding Room (from #2):

```bash
# After adding dependencies and entities:
./gradlew kspDebugKotlin  # Generate Room code
```

### Adding FCM (from #3):

```bash
# After adding google-services.json:
./gradlew clean build
```

---

## 🎯 Suggested Implementation Order

**Phase 1: Core Stability (Week 1)**
1. Gradle wrapper (#1)
2. Room database (#2)
3. Basic tests (#5 for auth)

**Phase 2: Production Ready (Week 2)**
4. Push notifications (#3)
5. Image attachments (#4)
6. Pagination (#6)
7. Error handling (#7)

**Phase 3: Polish (Week 3-4)**
8. User preferences (#8)
9. Accessibility (#10)
10. Performance (#11)

**Phase 4: Advanced Features (Future)**
11. Voice channels (#9)
12. Security hardening (#12)

---

## 📝 Notes

- All file paths assume project root is `flukavike-android/`
- Effort estimates assume familiar with Android/Kotlin
- Test on real devices, not just emulator
- Keep dependencies updated (Dependabot/ Renovate)

---

Last updated: 2026-04-01

# CLAUDE.md — Flukavike Android

## DO NOT TOUCH — FIXED CODE

The following files and systems were broken, debugged, and fixed through extensive testing.
**Do not refactor, "clean up", or modify any of the code listed below under any circumstances.**
If a future task seems to require touching these files, stop and ask the user first.

---

### Auth System — HANDS OFF

**Files:**
- `app/src/main/java/com/fluxer/client/data/repository/AuthRepository.kt`
- `app/src/main/java/com/fluxer/client/ui/viewmodel/AuthViewModel.kt`
- `app/src/main/java/com/fluxer/client/data/remote/AuthAuthenticator.kt`
- `app/src/main/java/com/fluxer/client/data/remote/AuthInterceptor.kt`
- `app/src/main/java/com/fluxer/client/data/remote/CsrfInterceptor.kt`
- `app/src/main/java/com/fluxer/client/data/local/SecureCookieStorage.kt`
- `app/src/main/java/com/fluxer/client/data/local/AuthTokenStorage.kt`

**Why:** Auth took days to stabilize. Key issues that were fixed:
- `runDiscovery()` was called inside `login()`, blocking for 12+ minutes due to OkHttp keep-alives bypassing `readTimeout`. Moved to background coroutine scope at startup.
- `callTimeout(20s)` added to OkHttpClient — the only reliable way to bound a call when TCP keep-alives reset `readTimeout`.
- `retryOnConnectionFailure(false)` prevents 2× timeout waits.
- Stale `EncryptedSharedPreferences` session cookie caused infinite loading on startup after reinstall. Cleared with `-wipe-data`.
- CSRF token must be fetched and attached to all POST/PATCH/DELETE. Do not alter interceptor order.
- Auth token is stored separately from session cookie — both are needed.

---

### Network Module — HANDS OFF

**File:** `app/src/main/java/com/fluxer/client/di/NetworkModule.kt`

**Why:** Interceptor order, timeout values, and retry settings are all load-bearing:
- `baseUrlOverrideInterceptor` must be first
- `authInterceptor` before `csrfInterceptor`
- `callTimeout(20s)` is the absolute wall-clock limit — do not remove or increase
- `retryOnConnectionFailure(false)` — do not set to true

---

### API Endpoints — HANDS OFF

**File:** `app/src/main/java/com/fluxer/client/data/remote/FluxerApiService.kt`

**Why:** Endpoints are exact and verified against the live Fluxer API:
- Current user is `/api/users/@me` — NOT `/api/auth/me` (404)
- Guild channels is `/api/guilds/{guildId}/channels` — guild objects from `/api/users/@me/guilds` do NOT embed channels

---

### Data Models — HANDS OFF

**Files:**
- `app/src/main/java/com/fluxer/client/data/model/AuthModels.kt`
- `app/src/main/java/com/fluxer/client/data/model/MessageModels.kt`

**Why:**
- `User.email` defaults to `""` — message author objects from the API do not include email
- `Message.authorId`, `Message.content`, `Message.createdAt` all default to `""` — API may omit these in certain contexts
- `ChannelType` uses a custom `KSerializer` to deserialize integers from the API (`0=TEXT, 1=DM, 2=VOICE, 4=CATEGORY`). The API sends integers, NOT enum string names. Do not revert to a plain `@Serializable enum class`.

---

### Captcha Widget — HANDS OFF

**File:** `app/src/main/java/com/fluxer/client/ui/components/CaptchaWidget.kt`

**Why:** Uses `?onload=onCaptchaLoaded` callback parameter — not a polling loop. The polling approach timed out before hCaptcha finished loading. Do not revert to a polling/interval approach.

---

### Channel Loading — HANDS OFF

**File:** `app/src/main/java/com/fluxer/client/ui/viewmodel/ChatViewModel.kt` — `selectServer()`

**Why:** `server.channels` is always `emptyList()` when guilds are loaded from `/api/users/@me/guilds`. Channels must be fetched separately via `chatRepository.getGuildChannels(server.id)`.

---

## Safe to Modify

- UI screens (`LoginScreen.kt`, `ChatScreen.kt`) — layout and styling only
- Theme files (`Color.kt`, `Type.kt`, `Theme.kt`)
- New features that do not touch the systems above

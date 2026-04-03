# FitnessAndroidApp — Critical Issues Action Plan

**Date:** 2026-03-27  
**Status:** ACTIVE  
**Project Version:** 1.0.0  
**Total Critical Issues Identified:** 26  

---

## Executive Summary

The FitnessAndroidApp has a solid architectural foundation (Clean MVVM, native BLE) but has **26 critical/high/medium issues** that prevent production readiness. The issues fall into three categories:

| Category | Count | Impact |
|----------|-------|--------|
| **Build/Compilation** | 2 | App cannot build reliably |
| **Security** | 5 | Data exposure, MITM vulnerability |
| **Runtime Crashes** | 4 | App will crash in specific scenarios |
| **Functional Bugs** | 6 | Features don't work as intended |
| **Architecture** | 5 | Maintainability and testability blocked |
| **Quality/Testing** | 4 | No test coverage, no CI safety net |

### Roadmap Overview

| Phase | Timeline | Focus | Issues |
|-------|----------|-------|--------|
| **Phase 1: Quick Wins** | Week 1 (Days 1-5) | Build fixes, crash prevention, security patches | 8 issues |
| **Phase 2: Core Fixes** | Week 2-3 (Days 6-15) | Functional bugs, broken APIs, data flow | 9 issues |
| **Phase 3: Hardening** | Week 4-6 (Days 16-30) | Architecture, testing, long-term stability | 9 issues |

---

## PHASE 1: QUICK WINS (Week 1)

### Issue 1.1 — Build Fails: JDK Version Mismatch
**Severity:** CRITICAL  
**Impact:** Project cannot build. All development is blocked.

**Problem:** `build.gradle.kts` specifies `jvmTarget = "21"` and `JavaVersion.VERSION_21`, but the system's `JAVA_HOME` points to JDK 11. The `gradle.properties` sets `org.gradle.java.home` to Android Studio's JBR, but command-line builds fail because `JAVA_HOME` overrides it.

**Root Cause:** Inconsistent JDK configuration between `build.gradle.kts` (Java 21), `gradle.properties` (JBR path), and environment variable `JAVA_HOME` (JDK 11).

**Corrective Actions:**
1. Verify Android Studio JBR path exists at `C:\Program Files\Android\Android Studio\jbr`
2. In `gradle.properties`, ensure `org.gradle.java.home` is set correctly (already done)
3. Change `build.gradle.kts` to use `JavaVersion.VERSION_17` and `jvmTarget = "17"` — AGP 8.5.0 requires Java 17, not 21
4. Update `compileOptions` to `VERSION_17`

**Files to modify:**
- `build.gradle.kts` lines 52-53, 57

**Timeline:** Day 1  
**Owner:** Dev Lead  
**Verification:** `./gradlew assembleDebug` succeeds  

---

### Issue 1.2 — TokenManager.getToken() Hangs Forever
**Severity:** CRITICAL  
**Impact:** Any synchronous token retrieval blocks the thread indefinitely. Auth flow completely broken.

**Problem:** `TokenManager.getToken()` at line 31 uses `collect` on a Flow, which is an infinite collection. It never returns.

**Root Cause:** Confusion between `collect` (terminal operator that suspends forever) and `first()` (terminal operator that returns the first emission).

**Corrective Actions:**
1. Replace `collect` with `first()` in `getToken()`
2. Remove unused import `kotlinx.coroutines.flow.collect`

**File:** `src/main/kotlin/com/fitness/app/network/auth/TokenManager.kt:29-35`

```kotlin
// BEFORE (broken):
suspend fun getToken(): String? {
    var result: String? = null
    context.dataStore.data.collect { preferences ->
        result = preferences[TOKEN_KEY]
    }
    return result
}

// AFTER (fixed):
suspend fun getToken(): String? {
    return context.dataStore.data.first()[TOKEN_KEY]
}
```

**Timeline:** Day 1  
**Owner:** Backend Dev  
**Verification:** Unit test calling `getToken()` returns within 1 second  

---

### Issue 1.3 — AuthInterceptor Always Uses Empty Token
**Severity:** CRITICAL  
**Impact:** ALL authenticated API calls return 401. Backend sync completely non-functional.

**Problem:** `RetrofitClient.kt` line 73 has `val token = ""` hardcoded. The `TokenManager` is created but never queried for the actual token.

**Root Cause:** Incomplete implementation — developer left a placeholder and never connected `TokenManager.getToken()` to the interceptor.

**Corrective Actions:**
1. Modify `AuthInterceptor` to use a blocking call to retrieve the token from `TokenManager`
2. Since `Interceptor.intercept()` is synchronous, use `runBlocking` to call `tokenManager.getToken()` OR cache the token in-memory and update it via a Flow
3. Add token refresh logic on 401 responses

**File:** `src/main/kotlin/com/fitness/app/network/client/RetrofitClient.kt:62-85`

```kotlin
// AFTER (fixed):
private class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    @Volatile
    private var cachedToken: String? = null
    
    init {
        // Collect token updates in background
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.tokenFlow.collect { token ->
                cachedToken = token
            }
        }
    }
    
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }
        val token = cachedToken
        val requestWithToken = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        return chain.proceed(requestWithToken)
    }
}
```

**Timeline:** Day 1-2  
**Owner:** Backend Dev  
**Dependencies:** Issue 1.2 must be fixed first  
**Verification:** API call with valid token returns 200  

---

### Issue 1.4 — RetrofitClient BASE_URL is Placeholder
**Severity:** CRITICAL  
**Impact:** All network requests fail with unknown host / connection refused.

**Problem:** `BASE_URL = "https://api.fitness-app.com/"` is a non-existent placeholder domain.

**Root Cause:** Backend URL was never configured for a real server.

**Corrective Actions:**
1. Move `BASE_URL` to `BuildConfig` field in `build.gradle.kts`
2. Read from `local.properties` or use a build flavor
3. Add a `NETWORK_BASE_URL` property to `local.properties`

**Files to modify:**
- `build.gradle.kts` — add `buildConfigField` for base URL
- `RetrofitClient.kt` — read from `BuildConfig`

```kotlin
// build.gradle.kts
val networkBaseUrl = localProperties.getProperty("NETWORK_BASE_URL") ?: "https://staging.api.example.com/"
buildConfigField("String", "NETWORK_BASE_URL", "\"$networkBaseUrl\"")

// RetrofitClient.kt
private const val BASE_URL = BuildConfig.NETWORK_BASE_URL
```

**Timeline:** Day 2  
**Owner:** Dev Lead + Backend Dev  
**Verification:** Network call reaches actual server  

---

### Issue 1.5 — WebView Security: Mixed Content & JS Bridge
**Severity:** HIGH  
**Impact:** MITM attacks possible. Any injected JS can trigger login callbacks.

**Problem:** `MainActivity.kt` lines 476-477 set `mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW` and expose a `LoginBridge` JavaScript interface with no origin validation.

**Root Cause:** Developer convenience during development; security review was not performed.

**Corrective Actions:**
1. Change `MIXED_CONTENT_ALWAYS_ALLOW` to `MIXED_CONTENT_NEVER_ALLOW`
2. Add origin checking to `LoginBridge.notifyLoginSuccess()` — verify the calling URL matches an allowlist
3. If WebView is dead code (not used in navigation), remove `FitnessWebView` entirely

**File:** `src/main/kotlin/com/fitness/app/MainActivity.kt:469-496`

**Timeline:** Day 2  
**Owner:** Security Lead  
**Verification:** Penetration test confirms mixed content blocked  

---

### Issue 1.6 — FitnessApplication.getInstance() Force Unwrap
**Severity:** HIGH  
**Impact:** `NullPointerException` crash if `getInstance()` called before `onCreate()`.

**Problem:** `FitnessApplication.kt` line 23 uses `instance!!` — crashes if any code accesses the instance during `Application` construction or in tests.

**Corrective Actions:**
1. Replace `instance!!` with a safe accessor that throws a descriptive error
2. Or use lazy initialization pattern

```kotlin
fun getInstance(): FitnessApplication = instance 
    ?: throw IllegalStateException("FitnessApplication not initialized. Call initialize() first.")
```

**File:** `src/main/kotlin/com/fitness/app/FitnessApplication.kt:23`

**Timeline:** Day 2  
**Owner:** Dev Lead  
**Verification:** Unit test verifies proper exception message  

---

### Issue 1.7 — RingRepositoryImpl.getBattery() Race Condition
**Severity:** HIGH  
**Impact:** Battery level always returns stale/null data. User sees "Unknown" battery.

**Problem:** `RingRepositoryImpl.kt` lines 201-209 call `requestBattery()` (async BLE write) and immediately read `_ringData.value.battery`. BLE response hasn't arrived yet.

**Root Cause:** Misunderstanding of async BLE operations — BLE writes are fire-and-forget; data arrives via notification callback.

**Corrective Actions:**
1. Replace synchronous `getBattery()` with a Flow-based approach
2. Add a `suspend fun requestBattery()` that sends the command AND waits for the response via a `CompletableDeferred` or by collecting from `ringData` flow until battery is non-null
3. Add a timeout (5 seconds)

```kotlin
override suspend fun requestBattery(): Result<Int> {
    return try {
        bleManager.requestBattery()
        // Wait for battery data via flow with timeout
        val battery = withTimeoutOrNull(5000) {
            ringData.filter { it.battery != null }.map { it.battery!! }.first()
        }
        if (battery != null) Result.Success(battery)
        else Result.Error("Battery request timed out")
    } catch (e: Exception) {
        Result.Error("Failed to get battery: ${e.message}")
    }
}
```

**Timeline:** Day 3  
**Owner:** BLE Dev  
**Dependencies:** Issue 1.2  
**Verification:** Battery reads consistently return valid value  

---

### Issue 1.8 — LoggingInterceptor Logs Auth Tokens in Production
**Severity:** HIGH  
**Impact:** Auth tokens and health data visible in logcat. Privacy violation.

**Problem:** `RetrofitClient.kt` line 45 sets `HttpLoggingInterceptor.Level.BODY` unconditionally.

**Corrective Actions:**
1. Use `Level.BODY` only in debug builds
2. Use `Level.NONE` or `Level.BASIC` in release builds

```kotlin
val logging = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY 
            else HttpLoggingInterceptor.Level.BASIC
}
```

**Timeline:** Day 3  
**Owner:** Security Lead  
**Verification:** Release APK logcat shows no request/response bodies  

---

## PHASE 2: CORE FIXES (Week 2-3)

### Issue 2.1 — AuthRepositoryImpl Not Connected to TokenManager
**Severity:** HIGH  
**Impact:** After Firebase login succeeds, API calls still fail. Auth and API are disconnected.

**Problem:** `AuthRepositoryImpl` authenticates via Firebase but never saves the Firebase ID token to `TokenManager`. `RetrofitClient` has no way to get auth tokens.

**Root Cause:** `AppContainer.kt` creates `AuthRepositoryImpl()` with no `TokenManager` injection.

**Corrective Actions:**
1. Add `TokenManager` as a constructor parameter to `AuthRepositoryImpl`
2. After successful Firebase sign-in, get the Firebase ID token and save it to `TokenManager`
3. On sign-out, clear the token
4. Update `AppContainer` to pass `TokenManager` to `AuthRepositoryImpl`

**Files to modify:**
- `src/main/kotlin/com/fitness/app/data/repository/AuthRepositoryImpl.kt`
- `src/main/kotlin/com/fitness/app/core/di/AppContainer.kt`

```kotlin
// AuthRepositoryImpl.kt
class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val tokenManager: TokenManager
) : IAuthRepository {
    
    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            // Save Firebase ID token for API calls
            val idToken = result.user?.getIdToken(false)?.await()?.token
            if (idToken != null) {
                tokenManager.saveToken(idToken)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Sign in failed")
        }
    }
}
```

**Timeline:** Day 6-7  
**Owner:** Backend Dev  
**Dependencies:** Issues 1.2, 1.3, 1.4  
**Verification:** Login → API call with token succeeds end-to-end  

---

### Issue 2.2 — SyncWorker Runs Without Auth Check
**Severity:** HIGH  
**Impact:** Background sync fails every 2 hours when user is not logged in. Wastes battery and logs errors.

**Problem:** `SyncWorker.kt` runs unconditionally via `PeriodicWorkRequest` scheduled in `MainActivity.onCreate()`.

**Corrective Actions:**
1. Add auth state check at the start of `SyncWorker.doWork()`
2. If not logged in, return `Result.failure()` immediately
3. Only schedule `SyncWorker` after successful login (move out of `MainActivity.onCreate()`)
4. Add network constraint: `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()`

**Files to modify:**
- `src/main/kotlin/com/fitness/app/network/sync/SyncWorker.kt`
- `src/main/kotlin/com/fitness/app/MainActivity.kt`
- `src/main/kotlin/com/fitness/app/data/repository/AuthRepositoryImpl.kt` (schedule on login)

**Timeline:** Day 7-8  
**Owner:** Backend Dev  
**Dependencies:** Issue 2.1  
**Verification:** Sync only runs when logged in and network available  

---

### Issue 2.3 — Hardcoded Mock Data in Dashboard Components
**Severity:** HIGH  
**Impact:** Dashboard shows fake data that doesn't reflect real user health metrics.

**Problem:** `DashboardScreen.kt` has three components with hardcoded values:
- `WeeklyEmotionsChart()` — hardcoded mood data
- `DailySummaryCard()` — hardcoded "5.2 km", "1h 23m", "68 bpm", "85%"
- `DailyInsightsCard()` — hardcoded "45 mins", "524 kcal"

**Corrective Actions:**
1. Add data parameters to each composable function
2. Pass real data from `DashboardUiState`
3. Use placeholder UI when data is unavailable (not fake numbers)

**Files to modify:**
- `src/main/kotlin/com/fitness/app/presentation/dashboard/screens/DashboardScreen.kt`

**Timeline:** Day 8-10  
**Owner:** UI Dev  
**Verification:** Dashboard reflects actual ring data when connected  

---

### Issue 2.4 — StepRepository Memory Leak
**Severity:** HIGH  
**Impact:** CoroutineScope never cancelled. Memory leaks on configuration changes.

**Problem:** `StepRepository.kt` line 19 creates `CoroutineScope(SupervisorJob() + Dispatchers.Main)` in constructor. This scope is never cancelled.

**Corrective Actions:**
1. Make `StepRepository` implement `Closeable` or `AutoCloseable`
2. Cancel the scope in `close()`
3. Or inject the scope from `AppContainer` so lifecycle is managed externally
4. Call `close()` when the repository is no longer needed

```kotlin
class StepRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : Closeable {
    // ... existing code ...
    
    override fun close() {
        scope.cancel()
    }
}
```

**Timeline:** Day 8  
**Owner:** Dev Lead  
**Verification:** LeakCanary shows no coroutine leaks  

---

### Issue 2.5 — DashboardScreen Permission Request on Every Resume
**Severity:** MEDIUM  
**Impact:** Permission dialog re-appears every time app resumes, even if already granted.

**Problem:** `DashboardScreen.kt` line 91 launches permission request on every `ON_RESUME` event without checking if permission is already granted.

**Corrective Actions:**
1. Add `ContextCompat.checkSelfPermission()` guard before launching permission request
2. Only request if permission is not already granted

**File:** `src/main/kotlin/com/fitness/app/presentation/dashboard/screens/DashboardScreen.kt`

**Timeline:** Day 9  
**Owner:** UI Dev  
**Verification:** Permission dialog only appears once  

---

### Issue 2.6 — Dual Result Type Confusion (kotlin.Result vs Custom Result)
**Severity:** MEDIUM  
**Impact:** Code confusion. Some callers expect custom `Result`, others get `kotlin.Result`.

**Problem:** `AuthRepositoryImpl` uses `kotlin.Result` while the rest of the codebase uses `com.fitness.app.core.util.Result`. This creates type confusion and potential runtime issues.

**Corrective Actions:**
1. Standardize on the custom `com.fitness.app.core.util.Result` sealed class across ALL repositories
2. Update `AuthRepositoryImpl` to return `com.fitness.app.core.util.Result`
3. Update `IAuthRepository` interface accordingly
4. Update `AuthViewModel` to handle the custom Result type

**Files to modify:**
- `src/main/kotlin/com/fitness/app/data/repository/AuthRepositoryImpl.kt`
- `src/main/kotlin/com/fitness/app/domain/repository/IAuthRepository.kt`
- `src/main/kotlin/com/fitness/app/presentation/auth/AuthViewModel.kt`

**Timeline:** Day 10-11  
**Owner:** Dev Lead  
**Verification:** All repositories use consistent Result type  

---

### Issue 2.7 — Database Destructive Migration Wipes All User Data
**Severity:** MEDIUM  
**Impact:** Every app update that changes Room schema destroys all user health history.

**Problem:** `AppContainer.kt` line 70 uses `fallbackToDestructiveMigration()`.

**Corrective Actions:**
1. Implement proper Room migrations for each schema version
2. Remove `fallbackToDestructiveMigration()`
3. Add migration test

**File:** `src/main/kotlin/com/fitness/app/core/di/AppContainer.kt`

**Timeline:** Day 11-13  
**Owner:** Data Dev  
**Verification:** App update preserves existing data  

---

### Issue 2.8 — NativeGattManager Permission Suppression
**Severity:** MEDIUM  
**Impact:** BLE operations silently fail or crash when permissions are revoked at runtime.

**Problem:** `@SuppressLint("MissingPermission")` used on 8+ methods in `NativeGattManager.kt`. All BLE operations skip permission checks.

**Corrective Actions:**
1. Remove all `@SuppressLint("MissingPermission")` annotations
2. Add proper `ContextCompat.checkSelfPermission()` guards before each BLE operation
3. Emit error state when permissions are missing

**File:** `src/main/kotlin/com/fitness/app/ble/NativeGattManager.kt`

**Timeline:** Day 12-14  
**Owner:** BLE Dev  
**Verification:** App handles permission revocation gracefully  

---

### Issue 2.9 — Deprecated BluetoothAdapter.getDefaultAdapter()
**Severity:** MEDIUM  
**Impact:** Deprecation warnings. Will break on future Android versions.

**Problem:** `NativeGattManager.kt` line 107 uses `BluetoothAdapter.getDefaultAdapter()` which is deprecated in API 31+.

**Corrective Actions:**
1. Replace with `context.getSystemService(BluetoothManager::class.java).adapter`
2. Add null safety check

**File:** `src/main/kotlin/com/fitness/app/ble/NativeGattManager.kt:107`

**Timeline:** Day 13  
**Owner:** BLE Dev  
**Verification:** No deprecation warnings in build  

---

## PHASE 3: HARDENING (Week 4-6)

### Issue 3.1 — Zero Test Coverage
**Severity:** CRITICAL (long-term)  
**Impact:** No safety net for refactoring. Regressions go undetected.

**Problem:** No `test/` or `androidTest/` directories exist. Zero unit tests, zero integration tests, zero UI tests.

**Corrective Actions:**
1. Create test directory structure
2. Add test dependencies (JUnit 5, MockK, Turbine, Compose Testing)
3. Write unit tests for:
   - `ConnectRingUseCase` (MAC validation)
   - `TokenManager` (token persistence)
   - `AuthViewModel` (state transitions)
   - `NativeGattManager` byte parsing (extract to utility first)
   - `DashboardViewModel` (flow combining)
4. Write integration tests for:
   - `RingRepositoryImpl` (BLE state mapping)
   - `AuthRepositoryImpl` (Firebase integration with emulator)
5. Write UI tests for:
   - `LoginScreen` (form validation)
   - `RingSetupScreen` (permission flow)

**Test directory structure:**
```
src/test/kotlin/com/fitness/app/
├── domain/usecase/
│   ├── ConnectRingUseCaseTest.kt
│   └── ScanDevicesUseCaseTest.kt
├── network/auth/
│   └── TokenManagerTest.kt
├── presentation/auth/
│   └── AuthViewModelTest.kt
└── ble/
    └── ByteParserTest.kt

src/androidTest/kotlin/com/fitness/app/
├── data/repository/
│   └── RingRepositoryImplTest.kt
└── presentation/
    └── LoginScreenTest.kt
```

**Timeline:** Day 16-25  
**Owner:** QA Lead + All Devs  
**Verification:** `./gradlew test && ./gradlew connectedAndroidTest` pass  

---

### Issue 3.2 — No BLE Abstraction (Untestable)
**Severity:** HIGH  
**Impact:** Cannot mock BLE for tests. Cannot unit test ViewModels that depend on BLE.

**Problem:** `NativeGattManager` is a concrete singleton with no interface. `RingRepositoryImpl` directly depends on it.

**Corrective Actions:**
1. Create `GattManager` interface with all public methods from `NativeGattManager`
2. Make `NativeGattManager` implement `GattManager`
3. Update `RingRepositoryImpl` to depend on `GattManager` interface
4. Create `FakeGattManager` for tests

**Files to modify:**
- New: `src/main/kotlin/com/fitness/app/ble/GattManager.kt`
- `src/main/kotlin/com/fitness/app/ble/NativeGattManager.kt`
- `src/main/kotlin/com/fitness/app/data/repository/RingRepositoryImpl.kt`

**Timeline:** Day 16-18  
**Owner:** Architecture Lead  
**Dependencies:** Issue 3.1  
**Verification:** Unit tests run with `FakeGattManager`  

---

### Issue 3.3 — Domain Layer Imports BLE Types
**Severity:** MEDIUM  
**Impact:** Clean Architecture violation. Domain layer is coupled to BLE layer.

**Problem:** `IRingRepository.kt` imports `com.fitness.app.ble.MeasurementTimer`, leaking BLE types into the domain layer.

**Corrective Actions:**
1. Create a domain-level `MeasurementTimer` model in `domain/model/`
2. Map BLE `MeasurementTimer` to domain `MeasurementTimer` in `RingRepositoryImpl`
3. Remove BLE imports from `IRingRepository`

**Files to modify:**
- New: `src/main/kotlin/com/fitness/app/domain/model/MeasurementState.kt`
- `src/main/kotlin/com/fitness/app/domain/repository/IRingRepository.kt`
- `src/main/kotlin/com/fitness/app/data/repository/RingRepositoryImpl.kt`

**Timeline:** Day 19-20  
**Owner:** Architecture Lead  
**Verification:** `grep -r "com.fitness.app.ble" src/main/kotlin/com/fitness/app/domain/` returns nothing  

---

### Issue 3.4 — No StepRepository Interface
**Severity:** MEDIUM  
**Impact:** Step logic scattered across 3 files. No abstraction for testing.

**Problem:** `StepRepository` is a concrete class in `data/` with no `IStepRepository` interface. `DashboardViewModel` uses it directly.

**Corrective Actions:**
1. Create `IStepRepository` interface in `domain/repository/`
2. Make `StepRepository` implement `IStepRepository`
3. Inject `IStepRepository` into `DashboardViewModel`

**Files to modify:**
- New: `src/main/kotlin/com/fitness/app/domain/repository/IStepRepository.kt`
- `src/main/kotlin/com/fitness/app/data/repository/StepRepository.kt`
- `src/main/kotlin/com/fitness/app/presentation/dashboard/DashboardViewModel.kt`

**Timeline:** Day 20-21  
**Owner:** Architecture Lead  
**Verification:** `DashboardViewModel` compiles with mock `IStepRepository`  

---

### Issue 3.5 — Large Files Need Decomposition
**Severity:** MEDIUM  
**Impact:** Maintainability. `BleManager.kt` (2037 lines), `NativeGattManager.kt` (1295 lines), `RingSetupScreen.kt` (1053 lines).

**Corrective Actions:**
1. Extract `NativeGattManager` byte parsing into `BleDataParser.kt`
2. Extract `NativeGattManager` command building into `BleCommandBuilder.kt`
3. Split `RingSetupScreen` into `ScanContent.kt`, `ConnectedContent.kt`, `PermissionContent.kt`
4. Remove or archive `BleManager.kt.deprecated` (2037 lines of dead code)

**Timeline:** Day 22-25  
**Owner:** All Devs  
**Verification:** No file exceeds 500 lines  

---

### Issue 3.6 — ProGuard/R8 Disabled for Release
**Severity:** MEDIUM  
**Impact:** Release APK is not obfuscated. Larger size. Easier to reverse-engineer.

**Problem:** `build.gradle.kts` line 42: `isMinifyEnabled = false`.

**Corrective Actions:**
1. Set `isMinifyEnabled = true` for release builds
2. Add comprehensive ProGuard rules for:
   - Firebase Auth
   - Retrofit / OkHttp
   - Gson (model classes)
   - Room (entities, DAOs)
   - Coroutines
   - Compose
   - `@JavascriptInterface` bridge
3. Test release build thoroughly

**File:** `build.gradle.kts`, `proguard-rules.pro`

**Timeline:** Day 25-27  
**Owner:** Dev Lead  
**Verification:** Release APK works correctly with R8 enabled  

---

### Issue 3.7 — NativeGattManager Coroutine Scope Leak
**Severity:** MEDIUM  
**Impact:** CoroutineScope created at class level, never cancelled when manager is destroyed.

**Problem:** `NativeGattManager.kt` line 278 creates `CoroutineScope(Dispatchers.Main).launch` without tracking the scope.

**Corrective Actions:**
1. Add a managed `CoroutineScope` field to `NativeGattManager`
2. Cancel all coroutines in a `destroy()` or `disconnect()` cleanup method
3. Call cleanup from `RingRepositoryImpl` lifecycle

**Timeline:** Day 22-23  
**Owner:** BLE Dev  
**Dependencies:** Issue 3.2  
**Verification:** LeakCanary shows no coroutine leaks after connect/disconnect cycles  

---

### Issue 3.8 — google-services.json Committed to VCS
**Severity:** LOW-MEDIUM  
**Impact:** Firebase API keys exposed in repository.

**Corrective Actions:**
1. Add `google-services.json` to `.gitignore`
2. Document setup instructions for new developers
3. Use a template file (`google-services.json.example`) with placeholder values

**Timeline:** Day 28  
**Owner:** Dev Lead  
**Verification:** `git status` shows `google-services.json` ignored  

---

### Issue 3.9 — No Error Recovery / Reconnection UX
**Severity:** MEDIUM  
**Impact:** When BLE disconnects, user sees generic "Disconnected" with no guidance.

**Corrective Actions:**
1. Add auto-reconnect with exponential backoff (partially exists in `NativeGattManager`)
2. Surface reconnect attempts in UI with countdown
3. Add "Retry" button in error state
4. Show toast/snackbar on connection loss

**Timeline:** Day 28-30  
**Owner:** UI Dev + BLE Dev  
**Verification:** User can recover from disconnect without restarting app  

---

## Dependency Map

```
Issue 1.2 (TokenManager fix)
    ├── Issue 1.3 (AuthInterceptor)
    ├── Issue 2.1 (AuthRepository + TokenManager)
    │   ├── Issue 2.2 (SyncWorker auth check)
    │   └── Issue 2.6 (Result type standardization)
    └── Issue 1.7 (Battery race condition)

Issue 3.1 (Test infrastructure)
    └── Issue 3.2 (BLE interface abstraction)
        ├── Issue 3.3 (Domain layer cleanup)
        ├── Issue 3.7 (Coroutine scope leak)
        └── Issue 3.4 (StepRepository interface)
```

---

## Risk Register

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Backend API not ready | High | Phase 2 blocked | Use mock server (WireMock) for development |
| Firebase project misconfigured | Medium | Auth broken | Verify `google-services.json` package matches `applicationId` |
| BLE protocol changes with firmware update | Low | Data parsing breaks | Add version detection + fallback parsers |
| R8 breaks Retrofit/Gson | Medium | Release crashes | Add comprehensive ProGuard rules + test release build |
| Team unfamiliar with Coroutines | Medium | More bugs introduced | Code review + pair programming on async fixes |

---

## Success Criteria

### Phase 1 Complete When:
- [ ] `./gradlew assembleDebug` succeeds on clean checkout
- [ ] `TokenManager.getToken()` returns within 1 second
- [ ] API call with valid token returns 200
- [ ] No auth tokens visible in release logcat
- [ ] Battery level reads correctly within 5 seconds
- [ ] WebView mixed content blocked

### Phase 2 Complete When:
- [ ] Login → API call → data sync works end-to-end
- [ ] Dashboard shows real ring data (not mock)
- [ ] No memory leaks after 10 connect/disconnect cycles
- [ ] Permission dialog appears only once
- [ ] All repositories use consistent Result type

### Phase 3 Complete When:
- [ ] 80%+ code coverage on use cases and ViewModels
- [ ] All BLE operations go through `GattManager` interface
- [ ] No file exceeds 500 lines
- [ ] Release APK builds with R8 enabled
- [ ] CI pipeline runs tests on every PR

---

## Resource Requirements

| Role | Effort | Phase |
|------|--------|-------|
| Dev Lead | 40% for 6 weeks | All phases |
| BLE Developer | 60% for 4 weeks | Phase 1 + 3 |
| Backend Developer | 80% for 3 weeks | Phase 1 + 2 |
| UI Developer | 50% for 3 weeks | Phase 2 |
| QA Lead | 30% for 4 weeks | Phase 2 + 3 |
| Security Lead | 20% for 2 weeks | Phase 1 |

---

*Generated: 2026-03-27 | FitnessAndroidApp Critical Issues Action Plan v1.0*

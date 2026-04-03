# FitnessAndroidApp 💍 Project Documentation Report

This report provides a comprehensive overview of the **FitnessAndroidApp**, a smart health monitoring application designed to work with the **R9 Smart Ring**. It covers the architecture, technology stack, feature set, and technical implementation details.

---

## 1. Project Overview
- **App Name**: FitnessAndroidApp (BLE Ring Health Monitor)
- **Purpose**: To provide real-time health monitoring and personalized wellness coaching by connecting to the R9 Smart Ring via Bluetooth Low Energy (BLE).
- **Target Audience**: Health-conscious individuals using the R9 Smart Ring for tracking vital metrics like heart rate, sleep, activity, and stress.
- **Tech Stack**:
    - **Language**: Kotlin (1.9.22)
    - **UI Framework**: Jetpack Compose (Material 3)
    - **Architecture**: Clean Architecture + MVVM (Model-View-ViewModel)
    - **Asynchronous Logic**: Kotlin Coroutines & StateFlow
    - **Local Database**: Room (with KSP)
    - **Networking**: Retrofit 2.10.0 & OkHttp
    - **Authentication**: Firebase Auth (Email/Password + Google Sign-In)
    - **Background Jobs**: WorkManager (Periodic health data synchronization)
    - **BLE**: Pure Native GATT implementation (No proprietary SDK used at runtime)
    - **Dependency Injection**: Manual DI via `AppContainer.kt`

### Project Structure
- `ble/`: Core logic for Bluetooth GATT communication, packet parsing, and command building.
- `presentation/`: UI Layer (Compose screens, ViewModels, and UI state models).
- `domain/`: Business Layer (Use Cases, Repository Interfaces, and Domain Models).
- `data/`: Data Layer (Repository implementations, Room DB entities, and data sources).
- `network/`: Retrofit API definitions and background synchronization workers.
- `core/`: Shared utilities and the manual Dependency Injection container.
- `ui/`: Theme definitions (colors, typography) and common premium UI components.

---

## 2. How Everything Works

### Architecture: Clean MVVM
The app follows Clean Architecture principles to ensure separation of concerns:
- **UI (View)**: Observes `StateFlow` from the ViewModel and triggers events.
- **ViewModel**: Manages UI state and calls Use Cases in the Domain layer.
- **Use Cases (Domain)**: Orchestrates business rules (e.g., "Connect to Ring", "Fetch Sleep History").
- **Repositories (Data)**: Bridges the gap between the Domain layer and data sources (BLE, local DB, or remote API).

### Navigation Flow
1. **Auth Check**: If not logged in, users are directed to the `AuthNavGraph` (Login/SignUp).
2. **Setup Check**: If the ring isn't paired, users see the `RingSetupRoute` (BLE Scanning).
3. **Main Dashboard**: Once authenticated and paired, the user lands on the `Dashboard`.
4. **Bottom Navigation**: Allows switching between `Dashboard`, `Sleep`, `AURA (Coach)`, `Wellness`, and `Streaks`.

### State Management
- **StateFlow**: Every ViewModel exposes a `UiState` (e.g., `RingUiState`, `DashboardUiState`) as a `StateFlow`.
- **Reactive Streams**: The `NativeGattManager` emits raw data via `StateFlow`, which is processed by the Repository, passed through Use Cases, and eventually updated in the ViewModel for the UI to recompose.

### Authentication Flow
- **Email/Password**: standard Firebase Auth login/creation.
- **Google Sign-In**:
    - App uses `GoogleAuthUiClient` to launch the Google Sign-In intent.
    - Captures the `idToken` from the result.
    - Passes the token to `FirebaseAuth` to authenticate the user session.
- **Persistence**: User session is managed by `FirebaseAuth`.

### Data Flow (UI → ViewModel → Repository → Source)
1. **Outgoing (Command)**: `Dashboard` (Event) → `ViewModel` → `ConnectUseCase` → `IRingRepository` → `NativeGattManager` (Sends 20-byte BLE packet).
2. **Incoming (Data)**: `R9 Ring` (20-byte Notify) → `NativeGattManager` (Normalization & Parsing) → `RingRepositoryImpl` (Mapping to Domain Model) → `ViewModel` (Update State) → `Dashboard` (Recompose).

---

## 3. Screen-by-Screen Breakdown

| Screen | Purpose | Key Components | User Actions |
|--------|---------|----------------|--------------|
| **Login / Sign Up** | User authentication | Custom TextFields, Google Button | Login, Sign Up, Social Sign-In |
| **Ring Setup** | Pairing the smart ring | BLE Device List, Scan Button | Scan, Connect, Save MAC Address |
| **Dashboard** | Main health overview | Pulse Waveforms, Metric Cards | View HR/SpO2/Steps, Start Measurements |
| **AURA AI Coach** | Personalized guidance | Chat-style interface | Chat with AI, View workout suggestions |
| **Sleep Tracker** | Sleep quality analysis | 24h Sleep Cycle Chart | View Deep/Light/Awake time |
| **Wellness Hub** | Mental health tools | Meditation Category Cards | Select Meditation, Start Journaling |
| **Meditation Timer** | Guided relaxation | Circular Countdown, Audio controls | Start/Pause, View progress |
| **Mood Journal** | Emotional tracking | Emoji Picker, Text Journal | Save Mood, Record daily reflections |
| **Streaks** | Gamified engagement | Achievement List, Streak Count | View badges, track daily goals |
| **Settings** | Configuration | Theme toggle, Profile management | Change Theme (Dark/Light), Logout |

---

## 4. Database & Backend

### Firebase Services
- **Firebase Authentication**: Handles all user identity management.

### Local Database (Room)
The app uses Room for offline persistence of user-generated health tracking:
- **`CoachMessageEntity`**: Stores conversation history with the AURA AI.
- **`JournalEntry`**: Stores user's text-based daily reflections.
- **`MoodEntry`**: Stores tracked moods and associated emojis.
- **`SleepEntry`**: Stores calculated sleep hours and dates.
- **`StreakEntry`**: Tracks user's consecutive activity days.

### Data Read/Write Logic
- **Write**: Data from BLE or user input is saved to Room via DAOs.
- **Read**: ViewModels observe Room data as `Flow<List<Entity>>` for real-time UI updates.
- **Sync**: `SyncWorker` (WorkManager) periodically reads local tables and pushes summarized data to a remote Retrofit API.

---

## 5. Key Files & Their Roles

- **`NativeGattManager.kt`**: The "brain" of the BLE communication. Handles low-level GATT operations and byte-array parsing.
- **`MainActivity.kt`**: Main entry point; manages high-level navigation and DI initialization.
- **`AppContainer.kt`**: Manual dependency injection container that provides singletons for repositories and ViewModels.
- **`RingViewModel.kt`**: Orchestrates state for health metrics and active measurements (HR, BP, etc.).
- **`RingRepositoryImpl.kt`**: Bridges the pure BLE layer with the Domain models, mapping raw bytes to readable health data.
- **`SyncWorker.kt`**: Manages background data synchronization between local storage and the remote backend.
- **`AppColors.kt` & `Theme.kt`**: Defines the "Premium Glass UI" design system (gradients, neon colors, and glassmorphism).

---

## 6. Current Features List
- ✅ **Real-time Monitoring**: Heart Rate, SpO2, Blood Pressure, and Stress.
- ✅ **Activity Tracking**: Steps, distance, and calories burned.
- ✅ **Sleep Analysis**: Deep sleep, light sleep, and awake time tracking.
- ✅ **AURA AI Coach**: Interactive AI-driven personalized health coaching.
- ✅ **Wellness Suite**: Guided meditations (Morning Calm, Breathing, Sleep) and Mood Journaling.
- ✅ **Gamification**: Streaks and achievement tracking.
- ✅ **Google Sign-In**: One-tap secure authentication with Firebase.
- ✅ **Native BLE**: Performance-optimized GATT communication without proprietary SDKs.
- ✅ **Glassmorphism UI**: High-end modern design with animations and gradients.

---

## 7. Known Limitations or Issues
- **Temperature Storage**: Body temperature is parsed but not currently saved in the `RingData` model or DB.
- **Sleep History**: Multi-packet assembly for detailed historical sleep is incomplete; currently only shows daily summaries.
- **Unit Testing**: Lack of comprehensive unit tests for the complex BLE parsing logic.
- **Write Delivery**: Uses `WRITE_TYPE_NO_RESPONSE` for some commands; missing application-level delivery confirmation.
- **BLE Scan Filtering**: Standard scan picks up all BLE devices; could benefit from a service UUID filter.

---

## 8. Future Improvements & Recommendations
- **Room Persistence for Health Data**: Move all real-time metrics (HR, SpO2) into Room to support local historical charts.
- **Interactive Charts**: Integrate a charting library (e.g., MPAndroidChart or Compose-based charts) for health trends.
- **Hilt Migration**: Replace manual DI (`AppContainer`) with Hilt for better lifecycle management and testing.
- **Background Health Sync**: Use `WorkManager` with the BLE layer to periodically wake the app and fetch data without user intervention.
- **Security Audit**: Ensure Firestore rules (if added later) and API tokens are securely managed; implement certificate pinning for Retrofit.
- **UX/UI Polish**: Add haptic feedback for meditation timers and successful BLE connections.

---
*Generated by DKGS Labs Documentation Engine*

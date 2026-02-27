package com.fitness.app.core.di

import android.content.Context
import com.fitness.app.FitnessAPI
import com.fitness.app.data.repository.FitnessRepositoryImpl
import com.fitness.app.data.repository.MeditationRepositoryImpl
import com.fitness.app.data.repository.RingRepositoryImpl
import com.fitness.app.domain.repository.IFitnessRepository
import com.fitness.app.domain.repository.IMeditationRepository
import com.fitness.app.domain.repository.IRingRepository
import com.fitness.app.domain.usecase.ConnectRingUseCase
import com.fitness.app.domain.usecase.DisconnectRingUseCase
import com.fitness.app.domain.usecase.GetRingDataUseCase
import com.fitness.app.domain.usecase.ScanDevicesUseCase
import com.fitness.app.network.client.RetrofitClient
import com.fitness.app.network.repository.FitnessRepository as NetworkFitnessRepository
import androidx.room.Room
import com.fitness.app.data.local.db.AppDatabase
import com.fitness.app.data.repository.SleepRepositoryImpl
import com.fitness.app.domain.repository.SleepRepository

/**
 * Manual Dependency Injection Container
 * 
 * Provides singleton instances of repositories, use cases, and services.
 * This is a simple DI solution without Hilt/Dagger complexity.
 * 
 * Usage:
 * ```
 * val container = AppContainer.getInstance(context)
 * val useCase = container.connectRingUseCase
 * ```
 */
class AppContainer private constructor(private val context: Context) {

    // ── Core Services ──────────────────────────────────────────────

    /** Local fitness data (SharedPreferences-based) */
    val fitnessAPI: FitnessAPI by lazy {
        FitnessAPI(context)
    }

    /** Retrofit HTTP client */
    val retrofitClient: RetrofitClient by lazy {
        RetrofitClient.getInstance(context)
    }

    /** Network repository for API calls */
    val networkRepository: NetworkFitnessRepository by lazy {
        NetworkFitnessRepository(
            retrofitClient.getApiService(),
            retrofitClient.getTokenManager()
        )
    }

    // ── Ring / BLE ─────────────────────────────────────────────────

    /** Ring BLE repository (singleton) */
    val ringRepository: IRingRepository by lazy {
        RingRepositoryImpl(context)
    }

    /** Local database */
    val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "fitness_app_db"
        ).fallbackToDestructiveMigration().build()
    }

    /** Sleep Repository */
    val sleepRepository: SleepRepository by lazy {
        SleepRepositoryImpl(appDatabase.sleepDao())
    }

    val scanDevicesUseCase: ScanDevicesUseCase by lazy {
        ScanDevicesUseCase(ringRepository)
    }

    val connectRingUseCase: ConnectRingUseCase by lazy {
        ConnectRingUseCase(ringRepository)
    }

    val disconnectRingUseCase: DisconnectRingUseCase by lazy {
        DisconnectRingUseCase(ringRepository)
    }

    val getRingDataUseCase: GetRingDataUseCase by lazy {
        GetRingDataUseCase(ringRepository)
    }

    // ── Fitness / Health ───────────────────────────────────────────

    /** Local meditation repository */
    val meditationLocalRepository: IMeditationRepository by lazy {
        MeditationRepositoryImpl()
    }

    /** Local fitness repository (wraps FitnessAPI behind a clean interface) */
    val fitnessLocalRepository: IFitnessRepository by lazy {
        FitnessRepositoryImpl(fitnessAPI)
    }

    // ── Singleton ──────────────────────────────────────────────────

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * Initialize container (call from Application.onCreate or MainActivity.onCreate)
         */
        fun initialize(context: Context) {
            getInstance(context)
        }
    }
}

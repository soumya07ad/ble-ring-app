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
import com.fitness.app.domain.repository.ICoachRepository
import com.fitness.app.data.repository.CoachRepositoryImpl
import com.fitness.app.domain.repository.SleepRepository
import com.fitness.app.data.repository.StreakRepository
import com.fitness.app.data.repository.SettingsRepository
import com.fitness.app.domain.repository.IStreakRepository
import com.fitness.app.domain.repository.ISettingsRepository

/**
 * Manual Dependency Injection Container
 * 
 * Provides singleton instances of repositories, use cases, and services.
 * All repositories are exposed via their interfaces for testability.
 * 
 * Usage:
 * ```
 * val container = AppContainer.getInstance(context)
 * val factory   = container.viewModelFactory
 * ```
 */
class AppContainer private constructor(private val context: Context) {

    // ── Core Services ──────────────────────────────────────────────

    val fitnessAPI: FitnessAPI by lazy { FitnessAPI(context) }

    val retrofitClient: RetrofitClient by lazy { RetrofitClient.getInstance(context) }

    val networkRepository: NetworkFitnessRepository by lazy {
        NetworkFitnessRepository(
            retrofitClient.getApiService(),
            retrofitClient.getTokenManager()
        )
    }

    // ── Database ────────────────────────────────────────────────────

    val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "fitness_app_database"
        ).fallbackToDestructiveMigration().build()
    }

    // ── Repositories (all exposed via interface) ────────────────────

    val ringRepository: IRingRepository by lazy { RingRepositoryImpl(context) }

    val sleepRepository: SleepRepository by lazy { SleepRepositoryImpl(appDatabase.sleepDao()) }

    val coachRepository: ICoachRepository by lazy { CoachRepositoryImpl(appDatabase.coachDao()) }

    val streakRepository: IStreakRepository by lazy { StreakRepository(appDatabase.streakDao()) }

    val settingsRepository: ISettingsRepository by lazy { SettingsRepository(context) }

    val meditationLocalRepository: IMeditationRepository by lazy { MeditationRepositoryImpl() }

    val fitnessLocalRepository: IFitnessRepository by lazy { FitnessRepositoryImpl(fitnessAPI) }

    // ── Use Cases ──────────────────────────────────────────────────

    val scanDevicesUseCase: ScanDevicesUseCase by lazy { ScanDevicesUseCase(ringRepository) }
    val connectRingUseCase: ConnectRingUseCase by lazy { ConnectRingUseCase(ringRepository) }
    val disconnectRingUseCase: DisconnectRingUseCase by lazy { DisconnectRingUseCase(ringRepository) }
    val getRingDataUseCase: GetRingDataUseCase by lazy { GetRingDataUseCase(ringRepository) }

    // ── ViewModel Factory ──────────────────────────────────────────

    val viewModelFactory: AppViewModelFactory by lazy { AppViewModelFactory(this) }

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

        fun initialize(context: Context) {
            getInstance(context)
        }
    }
}

package com.fitness.app.domain.usecase

import com.fitness.app.domain.model.RingHealthData
import com.fitness.app.domain.repository.IRingRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for observing ring health data
 */
class GetRingDataUseCase(
    private val repository: IRingRepository
) {
    /**
     * Observe ring health data as a Flow
     * @return StateFlow of ring health data
     */
    operator fun invoke(): StateFlow<RingHealthData> {
        return repository.ringData
    }
    
    /**
     * Get current ring data snapshot
     */
    fun getCurrentData(): RingHealthData {
        return repository.ringData.value
    }
}

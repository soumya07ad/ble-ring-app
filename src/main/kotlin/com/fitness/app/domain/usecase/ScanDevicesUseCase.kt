package com.fitness.app.domain.usecase

import com.fitness.app.core.util.Result
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.repository.IRingRepository

/**
 * Use case for scanning BLE devices
 * Encapsulates the business logic for device discovery
 */
class ScanDevicesUseCase(
    private val repository: IRingRepository
) {
    /**
     * Execute scan for devices
     * @param durationSeconds How long to scan (default 6 seconds)
     * @return Result containing list of found devices
     */
    suspend operator fun invoke(durationSeconds: Int = 6): Result<List<Ring>> {
        return repository.startScan(durationSeconds)
    }
    
    /**
     * Stop ongoing scan
     */
    fun stopScan() {
        repository.stopScan()
    }
}

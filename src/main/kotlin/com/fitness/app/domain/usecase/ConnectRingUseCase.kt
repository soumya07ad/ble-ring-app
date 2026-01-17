package com.fitness.app.domain.usecase

import com.fitness.app.core.util.Result
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.repository.IRingRepository

/**
 * Use case for connecting to a ring device
 * Encapsulates the business logic for device connection
 */
class ConnectRingUseCase(
    private val repository: IRingRepository
) {
    /**
     * Connect to a ring device
     * @param macAddress BLE MAC address
     * @param deviceName Optional device name for display
     * @return Result containing connected ring or error
     */
    suspend operator fun invoke(macAddress: String, deviceName: String? = null): Result<Ring> {
        // Validate MAC address format
        if (!isValidMacAddress(macAddress)) {
            return Result.error("Invalid MAC address format")
        }
        
        return repository.connect(macAddress, deviceName)
    }
    
    /**
     * Connect to a Ring object
     */
    suspend fun connect(ring: Ring): Result<Ring> {
        return invoke(ring.macAddress, ring.name)
    }
    
    /**
     * Validate MAC address format
     */
    private fun isValidMacAddress(mac: String): Boolean {
        val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
        return macPattern.matches(mac)
    }
}

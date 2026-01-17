package com.fitness.app.ble

/**
 * Manual MAC entry helper for BLE connection
 * WHY: Fallback when scan is unreliable (ring already paired, weak signal, etc.)
 */
object MacAddressValidator {
    
    // MAC address regex: XX:XX:XX:XX:XX:XX (hex digits, colon-separated)
    private val MAC_REGEX = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
    
    /**
     * Validate MAC address format
     * @return true if valid MAC format
     */
    fun isValidMacAddress(mac: String): Boolean {
        return MAC_REGEX.matches(mac.trim())
    }
    
    /**
     * Format MAC address with colons
     * Accepts input like: "AABBCCDDEEFF" or "AA:BB:CC:DD:EE:FF"
     * @return formatted MAC or null if invalid
     */
    fun formatMacAddress(input: String): String? {
        // Remove all non-hex characters
        val cleaned = input.replace(Regex("[^0-9A-Fa-f]"), "")
        
        // Must be exactly 12 hex digits
        if (cleaned.length != 12) return null
        
        // Insert colons every 2 characters
        return cleaned.chunked(2).joinToString(":")
    }
    
    /**
     * Get error message for invalid MAC
     */
    fun getErrorMessage(mac: String): String? {
        if (mac.isBlank()) return null
        
        val cleaned = mac.replace(Regex("[^0-9A-Fa-f]"), "")
        
        return when {
            cleaned.length < 12 -> "MAC address too short (need 12 hex digits)"
            cleaned.length > 12 -> "MAC address too long (need 12 hex digits)"
            !isValidMacAddress(mac) -> "Invalid format. Use XX:XX:XX:XX:XX:XX"
            else -> null
        }
    }
}

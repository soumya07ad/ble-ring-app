package com.fitness.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fitness.app.domain.model.Ring
import com.manridy.sdk_mrd2019.Manridy
import com.manridy.sdk_mrd2019.install.MrdPushCore
import com.manridy.sdk_mrd2019.read.MrdReadEnum
import com.manridy.sdk_mrd2019.read.MrdReadRequest
import com.manridy.sdk_mrd2019.send.MrdSendListRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * MRD SDK-based BLE Manager for R9 Ring
 * 
 * WORKING SDK that detects fresh R9 rings!
 * Uses Manridy MRD SDK for:
 * - BLE standard operations (scan/connect)
 * - Data parsing (battery, HR, steps)
 * - Commands (via Manridy.getMrdSend())
 */
class MrdBleManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MrdBleManager"
        
        // UUID for R9 Ring (MRD SDK)
        private val SERVICE_UUID = UUID.fromString("f000efe0-0451-4000-0000-00000000b000")
        private val NOTIFY_CHAR_UUID = UUID.fromString("f000efe3-0451-4000-0000-00000000b000")
        private val WRITE_CHAR_UUID = UUID.fromString("f000efe1-0451-4000-0000-00000000b000")
        private val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        
        @Volatile
        private var INSTANCE: MrdBleManager? = null
        
        fun getInstance(context: Context): MrdBleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MrdBleManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    // StateFlows
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _ringData = MutableStateFlow(RingData())
    val ringData: StateFlow<RingData> = _ringData.asStateFlow()
    
    private val _scanResults = MutableStateFlow<List<Ring>>(emptyList())
    val scanResults: StateFlow<List<Ring>> = _scanResults.asStateFlow()
    
    // BLE objects
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Scan state
    private var isScanning = false
    
    init {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "MRD BLE Manager initialized!")
        Log.i(TAG, "Ready to detect R9 rings")
        Log.i(TAG, "═══════════════════════════════════")
    }
    
    // ==================== Scanning ====================
    
    @SuppressLint("MissingPermission")
    fun startScan(durationSeconds: Int = 6) {
        if (isScanning) {
            Log.w(TAG, "⚠️ Already scanning")
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔍 MRD Starting Scan ($durationSeconds seconds)")
        Log.i(TAG, "NO FILTER - Will find fresh R9 rings!")
        Log.i(TAG, "═══════════════════════════════════")
        
        _scanResults.value = emptyList()
        isScanning = true
        
        try {
            bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
            
            // Stop scan after duration
            handler.postDelayed({
                stopScan()
            }, durationSeconds * 1000L)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Scan failed: ${e.message}", e)
            isScanning = false
        }
    }
    
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        
        Log.i(TAG, "🛑 MRD Stop Scan")
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        }
        isScanning = false
    }
    
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Ring"
            val mac = device.address
            val rssi = result.rssi
            
            // Check if already in list
            val currentResults = _scanResults.value.toMutableList()
            if (currentResults.none { it.macAddress == mac }) {
                Log.d(TAG, "📍 Found: $name ($mac) RSSI: $rssi")
                
                val ring = Ring(
                    name = name,
                    macAddress = mac,
                    rssi = rssi,
                    isConnected = false
                )
                currentResults.add(ring)
                _scanResults.value = currentResults
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "❌ Scan failed: errorCode=$errorCode")
            isScanning = false
        }
    }
    
    // ==================== Connection ====================
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(macAddress: String, deviceName: String? = null) {
        val currentState = _connectionState.value
        if (currentState is BleConnectionState.Connected) {
            Log.w(TAG, "⚠️ Already connected, disconnecting first...")
            disconnect()
        }
        
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device == null) {
            Log.e(TAG, "❌ Device not found: $macAddress")
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔗 MRD Connecting to: $macAddress")
        Log.i(TAG, "═══════════════════════════════════")
        
        _connectionState.value = BleConnectionState.Connecting
        connectedDevice = device
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        MrdPushCore.getInstance().init(bluetoothGatt)
    }
    
    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.i(TAG, "🔌 MRD Disconnecting")
        
        bluetoothGatt?.let {
            it.disconnect()
            it.close()
        }
        bluetoothGatt = null
        connectedDevice = null
        _connectionState.value = BleConnectionState.Disconnected
        _ringData.value = RingData()
    }
    
    // ==================== GATT Callback ====================
    
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✓ Connected, discovering services...")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "❌ Disconnected")
                    handler.post {
                        _connectionState.value = BleConnectionState.Disconnected
                    }
                }
            }
        }
        
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ Services discovered")
                
                // Enable notifications
                val service = gatt.getService(SERVICE_UUID)
                val notifyChar = service?.getCharacteristic(NOTIFY_CHAR_UUID)
                
                if (notifyChar != null) {
                    gatt.setCharacteristicNotification(notifyChar, true)
                    
                    val descriptor = notifyChar.getDescriptor(CLIENT_CONFIG_UUID)
                    descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    gatt.writeDescriptor(descriptor)
                    
                    handler.post {
                        connectedDevice?.let { device ->
                            val ring = Ring(
                                name = device.name ?: "R9 Ring",
                                macAddress = device.address,
                                isConnected = true
                            )
                            _connectionState.value = BleConnectionState.Connected(ring)
                        }
                        
                        // Request initial data
                        Log.i(TAG, "📊 Requesting initial battery data...")
                        handler.postDelayed({
                            requestBattery()
                        }, 500)
                    }
                } else {
                    Log.e(TAG, "❌ Notify characteristic not found!")
                }
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            if (data != null) {
                Log.d(TAG, "📥 Data received: ${bytesToHex(data)}")
                
                // Parse data using MRD SDK
                MrdPushCore.getInstance().readData(data)
                parseDataFromDevice(data)
            }
        }
        
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✓ Write success")
                MrdPushCore.getInstance().onCharacteristicWrite(status, characteristic)
            } else {
                Log.w(TAG, "⚠️ Write failed: status=$status")
            }
        }
        
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ MTU changed: $mtu")
                MrdPushCore.getInstance().onMtuChanged(mtu, status)
            }
        }
    }
    
    // ==================== Data Operations ====================
    
    /**
     * Request battery level from ring
     */
    fun requestBattery() {
        val command = Manridy.getMrdSend().getSystem(com.manridy.sdk_mrd2019.bean.send.SystemEnum.battery).datas
        writeData(command)
    }
    
    /**
     * Request heart rate from ring
     */
    fun requestHeartRate() {
        val command = Manridy.getMrdSend().getHrData(0).datas
        writeData(command)
    }
    
    /**
     * Request steps from ring
     */
    fun requestSteps() {
        val command = Manridy.getMrdSend().getStep(3).datas
        writeData(command)
    }
    
    /**
     * Request SpO2 (blood oxygen) from ring
     */
    fun requestSpO2() {
        val command = Manridy.getMrdSend().getBoData(0).datas
        writeData(command)
    }
    
    /**
     * Request blood pressure from ring
     */
    fun requestBloodPressure() {
        val command = Manridy.getMrdSend().getBpData(0).datas
        writeData(command)
    }
    
    /**
     * Request stress/HRV data from ring
     */
    fun requestStress() {
        val command = Manridy.getMrdSend().getHRVHistory(2).datas
        writeData(command)
    }
    
    /**
     * Write data to ring
     */
    @SuppressLint("MissingPermission")
    private fun writeData(data: ByteArray) {
        val gatt = bluetoothGatt
        if (gatt == null) {
            Log.e(TAG, "❌ Not connected")
            return
        }
        
        val service = gatt.getService(SERVICE_UUID)
        val writeChar = service?.getCharacteristic(WRITE_CHAR_UUID)
        
        if (writeChar != null) {
            writeChar.value = data
            writeChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            gatt.writeCharacteristic(writeChar)
            Log.d(TAG, "📤 Wrote: ${bytesToHex(data)}")
        } else {
            Log.e(TAG, "❌ Write characteristic not found")
        }
    }
    
    /**
     * Parse incoming data from ring using MRD SDK
     */
    private fun parseDataFromDevice(data: ByteArray) {
        try {
            val readRequest: MrdReadRequest = Manridy.getMrdRead().read(data)
            
            if (readRequest.status == 0 || readRequest.mrdReadEnum == MrdReadEnum.Failure) {
                Log.w(TAG, "⚠️ Parse failed or unsupported command")
                return
            }
            
            val json = readRequest.json

            Log.i(TAG, "📦 MRD Data: ${readRequest.mrdReadEnum.name} = $json")

            if (!json.isNullOrEmpty()) {
                val battery = parseJsonInt(json, "battery")
                if (battery != null && battery in 1..100) {
                    Log.i(TAG, "🔋 Battery: $battery%")
                    handler.post {
                        _ringData.value = _ringData.value.copy(battery = battery, lastUpdate = System.currentTimeMillis())
                    }
                }

                val hr = parseJsonInt(json, "heartRate") ?: parseJsonInt(json, "heart")
                if (hr != null && hr in 40..220) {
                    Log.i(TAG, "❤️ Heart Rate: $hr bpm")
                    handler.post {
                        _ringData.value = _ringData.value.copy(heartRate = hr, lastUpdate = System.currentTimeMillis())
                    }
                }

                // Parse Steps with enhanced data (calories, distance)
                val steps = parseJsonInt(json, "step") ?: parseJsonInt(json, "steps")
                val calories = parseJsonInt(json, "stepCalorie") ?: parseJsonInt(json, "calories")
                val distanceMeters = parseJsonInt(json, "stepMileage") ?: parseJsonInt(json, "distance")
                
                if (steps != null && steps >= 0) {
                    val distanceKm = (distanceMeters ?: 0) / 1000.0
                    Log.i(TAG, "👟 Steps: $steps (${calories ?: 0} cal, ${String.format("%.2f", distanceKm)} km)")
                    handler.post {
                        _ringData.value = _ringData.value.copy(
                            steps = steps,
                            calories = calories ?: 0,
                            distance = distanceMeters ?: 0,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
                
                // Parse SpO2 (Blood Oxygen)
                val spo2 = parseJsonInt(json, "bo") ?: parseJsonInt(json, "spo2")
                if (spo2 != null && spo2 in 80..100) {
                    Log.i(TAG, "🫁 SpO2: $spo2%")
                    handler.post {
                        _ringData.value = _ringData.value.copy(spO2 = spo2, lastUpdate = System.currentTimeMillis())
                    }
                }
                
                // Parse Blood Pressure
                val systolic = parseJsonInt(json, "hightBp") ?: parseJsonInt(json, "systolic")
                val diastolic = parseJsonInt(json, "lowBp") ?: parseJsonInt(json, "diastolic")
                if (systolic != null && diastolic != null) {
                    Log.i(TAG, "🩺 BP: $systolic/$diastolic mmHg")
                    handler.post {
                        _ringData.value = _ringData.value.copy(
                            bloodPressureSystolic = systolic,
                            bloodPressureDiastolic = diastolic,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
                
                // Parse Stress/HRV
                val stress = parseJsonInt(json, "hrv") ?: parseJsonInt(json, "stress")
                if (stress != null && stress in 0..100) {
                    Log.i(TAG, "😰 Stress: $stress")
                    handler.post {
                        _ringData.value = _ringData.value.copy(stress = stress, lastUpdate = System.currentTimeMillis())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}", e)
        }
    }
    
    // ==================== Helper Functions ====================
    
    private fun parseJsonInt(json: String?, key: String): Int? {
        if (json.isNullOrEmpty()) return null
        return try {
            val regex = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
            regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}


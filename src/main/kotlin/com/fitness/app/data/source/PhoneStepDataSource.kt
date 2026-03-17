package com.fitness.app.data.source

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhoneStepDataSource(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val prefs: SharedPreferences = context.getSharedPreferences("PhoneStepData", Context.MODE_PRIVATE)

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private val _isSupported = MutableStateFlow(stepSensor != null)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        if (stepSensor != null) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceReboot = event.values[0].toInt()
            handleStepCalculation(totalStepsSinceReboot)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }

    private fun handleStepCalculation(totalStepsSinceReboot: Int) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastSavedDate = prefs.getString("last_saved_date", "")

        if (lastSavedDate != currentDate) {
            // First time today, save as morning_steps
            prefs.edit()
                .putString("last_saved_date", currentDate)
                .putInt("morning_steps", totalStepsSinceReboot)
                .apply()
        }

        val morningSteps = prefs.getInt("morning_steps", totalStepsSinceReboot)
        var todaySteps = totalStepsSinceReboot - morningSteps

        if (todaySteps < 0) {
            // Device rebooted during the day, counter reset to 0
            // Update morning_steps to reflect the reboot
            prefs.edit()
                .putInt("morning_steps", 0)
                .apply()
            todaySteps = totalStepsSinceReboot
        }

        _steps.value = todaySteps
    }
}

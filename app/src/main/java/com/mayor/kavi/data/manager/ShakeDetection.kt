package com.mayor.kavi.data.manager

import android.content.Context
import android.hardware.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

class ShakeDetectionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var accelerometerListener: SensorEventListener? = null
    private var lastShakeTime = 0L
    private val shakeCooldown = 1000L
    private val shakeThreshold = 20f  // Increased threshold for better detection

    private var onShakeListener: (() -> Unit)? = null
    private var isEnabled = false
    private var isListening = false

    init {
        // Observe shake enabled setting
        CoroutineScope(Dispatchers.Main).launch {
            settingsManager.getShakeEnabled().collect { enabled ->
                isEnabled = enabled
                if (enabled) {
                    startListening()
                } else {
                    stopListening()
                }
            }
        }
    }

    fun setOnShakeListener(listener: () -> Unit) {
        onShakeListener = listener
    }

    fun clearOnShakeListener() {
        onShakeListener = null
        stopListening()
    }

    fun startListening() {
        if (!isEnabled || isListening || accelerometerListener != null) return

        accelerometerListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (!isEnabled) return
                    if (it.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                    if (System.currentTimeMillis() - lastShakeTime < shakeCooldown) return

                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]
                    val acceleration = sqrt(x * x + y * y + z * z)

                    if (acceleration > shakeThreshold) {
                        lastShakeTime = System.currentTimeMillis()
                        onShakeListener?.invoke()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                accelerometerListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            isListening = true
        }
    }

    fun stopListening() {
        accelerometerListener?.let {
            if (isListening) {
                sensorManager.unregisterListener(it)
                accelerometerListener = null
                isListening = false
            }
        }
    }

}
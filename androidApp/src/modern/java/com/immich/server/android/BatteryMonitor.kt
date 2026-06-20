package com.immich.server.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Battery status monitor
 * Tracks battery level, charging state, and time since fully charged
 */
class BatteryMonitor(private val context: Context) {
    
    data class BatteryState(
        val level: Int = 0,                    // Battery level (0-100)
        val isCharging: Boolean = false,       // Is charging
        val isFull: Boolean = false,           // Is fully charged (level >= 100)
        val chargeType: String = "",           // AC, USB, Wireless, or Unknown
        val temperature: Float = 0f,           // Battery temperature in °C
        val voltage: Float = 0f,               // Battery voltage in V
        val health: String = "",               // Good, Overheat, Dead, etc.
        val timeSinceFullMillis: Long = 0L,    // Time since battery reached 100%
        val isOvercharging: Boolean = false    // Charging while already full
    )
    
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()
    
    // Track when battery first reached 100%
    private var fullChargeStartTime: Long? = null
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateBatteryState(it) }
        }
    }
    
    fun start() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // For Android 13+, need to register with explicit flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                batteryReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(batteryReceiver, filter)
        }
        
        // Get initial state
        val initialIntent = context.registerReceiver(null, filter)
        initialIntent?.let { updateBatteryState(it) }
    }
    
    fun stop() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
    
    private fun updateBatteryState(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPercent = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else 0
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = batteryPercent >= 100
        
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val chargeType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Unknown"
        }
        
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
        
        val healthStatus = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val health = when (healthStatus) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
        
        // Track time since full charge
        val currentTime = System.currentTimeMillis()
        if (isFull && isCharging) {
            if (fullChargeStartTime == null) {
                fullChargeStartTime = currentTime
            }
        } else {
            // Reset when not full or not charging
            fullChargeStartTime = null
        }
        
        val timeSinceFullMillis = if (isFull && fullChargeStartTime != null) {
            currentTime - fullChargeStartTime!!
        } else 0L
        
        // Overcharging: charging while already full for more than threshold
        val overchargeThreshold = TimeUnit.HOURS.toMillis(1) // 1 hour
        val isOvercharging = isFull && isCharging && timeSinceFullMillis > overchargeThreshold
        
        _batteryState.value = BatteryState(
            level = batteryPercent,
            isCharging = isCharging,
            isFull = isFull,
            chargeType = chargeType,
            temperature = temperature,
            voltage = voltage,
            health = health,
            timeSinceFullMillis = timeSinceFullMillis,
            isOvercharging = isOvercharging
        )
    }
    
    fun formatDuration(millis: Long): String {
        if (millis <= 0) return ""
        
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    fun getOverchargeWarning(millis: Long): String {
        if (millis <= TimeUnit.HOURS.toMillis(1)) return ""
        
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        return when {
            hours >= 4 -> "⚠️ 已充满 ${hours} 小时！请断开充电器以保护电池"
            hours >= 2 -> "⚠️ 已充满 ${hours} 小时，建议断开充电器"
            hours >= 1 -> "已充满 ${hours} 小时，可断开充电器"
            else -> ""
        }
    }
}
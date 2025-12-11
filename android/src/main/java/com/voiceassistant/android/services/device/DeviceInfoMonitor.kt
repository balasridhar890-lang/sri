package com.voiceassistant.android.services.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract

/**
 * Monitors device information: battery, running apps, contacts
 */
@Singleton
class DeviceInfoMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DeviceInfoMonitor"
    }
    
    data class DeviceInfo(
        val batteryPercentage: Int,
        val isCharging: Boolean,
        val batteryHealth: String,
        val temperature: Int,
        val runningAppsCount: Int,
        val totalContactsCount: Int,
        val timestamp: Long
    )
    
    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo
    
    /**
     * Get current battery percentage
     */
    fun getBatteryPercentage(): Int {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = ContextCompat.registerReceiver(
                context,
                null,
                ifilter,
                ContextCompat.RECEIVER_EXPORTED
            ) ?: return 0
            
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            
            (level / scale.toFloat() * 100).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery percentage", e)
            0
        }
    }
    
    /**
     * Check if device is charging
     */
    fun isCharging(): Boolean {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = ContextCompat.registerReceiver(
                context,
                null,
                ifilter,
                ContextCompat.RECEIVER_EXPORTED
            ) ?: return false
            
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check charging status", e)
            false
        }
    }
    
    /**
     * Get battery health
     */
    fun getBatteryHealth(): String {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = ContextCompat.registerReceiver(
                context,
                null,
                ifilter,
                ContextCompat.RECEIVER_EXPORTED
            ) ?: return "unknown"
            
            val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            
            when (health) {
                BatteryManager.BATTERY_HEALTH_COLD -> "cold"
                BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
                BatteryManager.BATTERY_HEALTH_GOOD -> "good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "unspecified_failure"
                BatteryManager.BATTERY_HEALTH_UNKNOWN -> "unknown"
                else -> "unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery health", e)
            "unknown"
        }
    }
    
    /**
     * Get battery temperature
     */
    fun getBatteryTemperature(): Int {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = ContextCompat.registerReceiver(
                context,
                null,
                ifilter,
                ContextCompat.RECEIVER_EXPORTED
            ) ?: return 0
            
            batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery temperature", e)
            0
        }
    }
    
    /**
     * Get count of running applications
     */
    fun getRunningAppsCount(): Int {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = am.runningAppProcesses
            processes?.size ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get running apps count", e)
            0
        }
    }
    
    /**
     * Get list of running application packages
     */
    fun getRunningApps(): List<String> {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = am.runningAppProcesses
            processes?.mapNotNull { it.processName }?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get running apps", e)
            emptyList()
        }
    }
    
    /**
     * Get total contacts count
     */
    fun getTotalContactsCount(): Int {
        return try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null,
                null,
                null
            )
            
            val count = cursor?.count ?: 0
            cursor?.close()
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contacts count", e)
            0
        }
    }
    
    /**
     * Get list of all contacts
     */
    fun getContacts(): List<ContactInfo> {
        return try {
            val contacts = mutableListOf<ContactInfo>()
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                null,
                null,
                null
            )
            
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val name = c.getString(1)
                    val hasPhone = c.getInt(2) > 0
                    
                    var phoneNumber = ""
                    if (hasPhone) {
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )
                        
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                phoneNumber = pc.getString(0)
                            }
                        }
                    }
                    
                    contacts.add(ContactInfo(id, name, phoneNumber, hasPhone))
                }
            }
            
            contacts
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contacts", e)
            emptyList()
        }
    }
    
    /**
     * Update device info state
     */
    fun updateDeviceInfo() {
        Log.d(TAG, "Updating device info")
        
        try {
            val info = DeviceInfo(
                batteryPercentage = getBatteryPercentage(),
                isCharging = isCharging(),
                batteryHealth = getBatteryHealth(),
                temperature = getBatteryTemperature(),
                runningAppsCount = getRunningAppsCount(),
                totalContactsCount = getTotalContactsCount(),
                timestamp = System.currentTimeMillis()
            )
            
            _deviceInfo.value = info
            Log.d(TAG, "Device info updated: battery=${info.batteryPercentage}%, apps=${info.runningAppsCount}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update device info", e)
        }
    }
    
    data class ContactInfo(
        val id: String,
        val name: String,
        val phoneNumber: String,
        val hasPhoneNumber: Boolean
    )
}

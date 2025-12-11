package com.voiceassistant.android.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android runtime permissions with fallback handling
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // Core permissions for phone integration
        val CALL_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.MANAGE_OWN_CALLS,
            Manifest.permission.CALL_PHONE
        )
        
        val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )
        
        val CONTACT_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS
        )
        
        val STORAGE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val ALL_REQUIRED_PERMISSIONS = CALL_PERMISSIONS + SMS_PERMISSIONS + 
            CONTACT_PERMISSIONS + STORAGE_PERMISSIONS
    }
    
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasAllPermissions(permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }
    
    fun getMissingPermissions(permissions: Array<String>): Array<String> {
        return permissions.filter { !hasPermission(it) }.toTypedArray()
    }
    
    fun hasCallPermissions(): Boolean {
        return hasAllPermissions(CALL_PERMISSIONS)
    }
    
    fun hasSMSPermissions(): Boolean {
        return hasAllPermissions(SMS_PERMISSIONS)
    }
    
    fun hasContactPermissions(): Boolean {
        return hasAllPermissions(CONTACT_PERMISSIONS)
    }
    
    fun hasStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses scoped storage
            hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            hasAllPermissions(STORAGE_PERMISSIONS)
        }
    }
    
    fun hasLocationPermissions(): Boolean {
        return hasAllPermissions(LOCATION_PERMISSIONS)
    }
    
    fun getRequiredCallPermissions(): Array<String> {
        return getMissingPermissions(CALL_PERMISSIONS)
    }
    
    fun getRequiredSMSPermissions(): Array<String> {
        return getMissingPermissions(SMS_PERMISSIONS)
    }
    
    fun getRequiredContactPermissions(): Array<String> {
        return getMissingPermissions(CONTACT_PERMISSIONS)
    }
    
    fun getRequiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                .filter { !hasPermission(it) }
                .toTypedArray()
        } else {
            getMissingPermissions(STORAGE_PERMISSIONS)
        }
    }
    
    /**
     * Check if permission should be requested (hasn't been permanently denied)
     */
    fun shouldRequestPermission(activity: Context, permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !hasPermission(permission)
        } else {
            false
        }
    }
    
    /**
     * Get all critical permissions that must be granted for app to function
     */
    fun getCriticalPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.INTERNET
        )
    }
    
    /**
     * Check if critical permissions are granted
     */
    fun hasCriticalPermissions(): Boolean {
        return hasAllPermissions(getCriticalPermissions())
    }
}

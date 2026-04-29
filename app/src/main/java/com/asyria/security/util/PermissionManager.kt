package com.asyria.security.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    /**
     * Checks if a single permission is granted.
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if all dangerous permissions required by the app are granted.
     */
    fun hasAllDangerousPermissions(): Boolean {
        return getDangerousPermissions().all { hasPermission(it) }
    }

    /**
     * Returns a list of dangerous permissions that need to be requested at runtime.
     */
    fun getDangerousPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        return permissions
    }
    
    /**
     * Identifies permissions that are not yet granted.
     */
    fun getMissingPermissions(): Array<String> {
        return getDangerousPermissions().filter { !hasPermission(it) }.toTypedArray()
    }
}

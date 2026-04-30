package com.asyria.security

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.asyria.security.data.SessionManager
import com.asyria.security.ui.navigation.NavGraph
import com.asyria.security.ui.theme.SentinelTheme
import com.asyria.security.ui.theme.ThemeMode

import android.content.IntentFilter
import android.hardware.usb.UsbManager
import com.asyria.security.services.UsbBroadcastReceiver

class MainActivity : FragmentActivity() {
    private val usbReceiver = UsbBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(usbReceiver, filter)

        enableEdgeToEdge()
        val sessionManager = SessionManager(this)
        
        setContent {
            val themeMode by sessionManager.themeMode.collectAsState(initial = ThemeMode.STANDARD)
            
            SentinelTheme(mode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            // Context might have already unregistered
        }
    }
}

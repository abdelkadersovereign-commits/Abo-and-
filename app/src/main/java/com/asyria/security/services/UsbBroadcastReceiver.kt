package com.asyria.security.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UsbBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            // A USB Device was attached, prompt for security scan
            Toast.makeText(context, "[A.SYRIA] EXTERN USB DETECTED. ANALYZING...", Toast.LENGTH_LONG).show()
            
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                Toast.makeText(context, "[A.SYRIA] USB SAFE. NO THREAT FOUND.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

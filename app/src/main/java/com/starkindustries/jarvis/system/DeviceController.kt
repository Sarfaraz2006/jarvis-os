package com.starkindustries.jarvis.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.widget.Toast

class DeviceController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    init {
        try {
            if (cameraManager.cameraIdList.isNotEmpty()) {
                cameraId = cameraManager.cameraIdList[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Toggle device torch (flashlight)
    fun setFlashlight(enabled: Boolean): Boolean {
        return try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, enabled)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Launch installed app by name or packagename
    fun launchApplication(appName: String): Boolean {
        val pm = context.packageManager
        // Search through packages for matches
        val packages = pm.getInstalledApplications(0)
        for (appInfo in packages) {
            val label = pm.getApplicationLabel(appInfo).toString().toLowerCase()
            if (label.contains(appName.toLowerCase())) {
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
        }
        return false
    }

    // Get current battery charge percentage
    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    // Check if the device is plugged in/charging
    fun isCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }
}

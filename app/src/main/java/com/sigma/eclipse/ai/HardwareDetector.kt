package com.sigma.eclipse.ai

import android.app.ActivityManager
import android.content.Context

enum class DeviceCapability {
    LOW, MEDIUM, HIGH, UNSUPPORTED
}

class HardwareDetector(private val context: Context) {
    fun getAvailableRAM(): Long {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    fun getDeviceCapability(): DeviceCapability {
        val totalRamGb = getAvailableRAM() / (1024 * 1024 * 1024)
        return when {
            totalRamGb < 4 -> DeviceCapability.LOW // Under 4GB -> model_s
            totalRamGb < 8 -> DeviceCapability.MEDIUM // 4-8GB -> Qwen 1.5B/4B highly quantized
            else -> DeviceCapability.HIGH // 8GB+ -> Qwen 4B runs comfortably
        }
    }
    
    fun canRunLocalModel(): Boolean {
        return getDeviceCapability() != DeviceCapability.UNSUPPORTED
    }
}

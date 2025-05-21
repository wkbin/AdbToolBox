package model

data class DeviceInfo(
    val model: String = "",
    val androidVersion: String = "",
    val buildNumber: String = "",
    val deviceId: String = "",
    val cpuInfo: CpuInfo = CpuInfo(),
    val totalMemory: String = "",
    val usedMemory: String = "",
    val memoryUsagePercent: Float = 0f,
    val totalStorage: String = "",
    val usedStorage: String = "",
    val storageUsagePercent: Float = 0f,
    val availableStorage:String = "",
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val wifiEnabled: Boolean = false,
    val mobileDataEnabled: Boolean = false,
    val ipAddress: String = "",
    val error: String = ""
)

data class CpuInfo(
    val architecture: String = "",
    val cores: Int = 0,
    val frequency: String = "",
    val cache: String = "",
    val frequencies: List<String> = emptyList()
) 
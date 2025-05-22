package model


data class DeviceInfo(
    val model: String = "",
    val androidVersion: String = "",
    val buildNumber: String = "",
    val deviceId: String = "",
    val isRooted: Boolean = false,
    val error: String? = null
)


package model

data class NetworkInfo(
    val wifiEnabled: Boolean = false,
    val mobileDataEnabled: Boolean = false,
    val ipAddress: String = ""
)
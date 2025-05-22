package model

data class StorageInfo(
    val total: String = "0 B",
    val used: String = "0 B",
    val available: String = "0 B",
    val usagePercent: Float = 0f
)
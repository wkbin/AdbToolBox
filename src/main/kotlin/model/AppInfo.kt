package model

data class AppInfo(
    val packageName: String = "",
    val appName: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val isSystem: Boolean = false,
    val isEnabled: Boolean = true,
    val installTime: Long = 0,
    val updateTime: Long = 0,
    val size: Long = 0,
    val targetSdk: Int = 0,
    val minSdk: Int = 0,
    val permissions: List<String> = emptyList()
) 
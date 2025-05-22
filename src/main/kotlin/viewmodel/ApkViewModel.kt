package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import model.AppInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import runtime.adb.AdbDevicePoller

class ApkViewModel : ViewModel(), KoinComponent {
    private val adbDevicePoller: AdbDevicePoller by inject()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _selectedApp = MutableStateFlow<AppInfo?>(null)
    val selectedApp: StateFlow<AppInfo?> = _selectedApp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun refreshApps() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 获取应用列表
                val output = adbDevicePoller.exec("shell pm list packages -f")
                if (output.firstOrNull()?.contains("No device connected") == true) {
                    _apps.value = emptyList()
                    return@launch
                }
                val apps = parseAppList(output)
                _apps.value = apps
            } catch (e: Exception) {
                // 处理错误
                e.printStackTrace()
                _apps.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectApp(app: AppInfo) {
        _selectedApp.value = app
    }

    fun enableApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.exec("shell pm enable $packageName")
                refreshApps()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun disableApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.exec("shell pm disable $packageName")
                refreshApps()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun extractApk(packageName: String, outputPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 获取 APK 路径
                val pathOutput = adbDevicePoller.exec("shell pm path $packageName")
                val apkPath = pathOutput.firstOrNull()?.substringAfter("package:") ?: return@launch
                
                // 拉取 APK 文件
                adbDevicePoller.exec("pull $apkPath $outputPath")
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun installApk(apkPath: String) {
        viewModelScope.launch {
            try {
                adbDevicePoller.exec("install -r $apkPath")
                refreshApps()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    private suspend fun parseAppList(output: List<String>): List<AppInfo> {
        return output.mapNotNull { line ->
            try {
                // 解析格式：package:/data/app/com.example.app-1/base.apk=com.example.app
                val parts = line.substringAfter("package:").split("=")
                if (parts.size != 2) return@mapNotNull null

                val path = parts[0]
                val packageName = parts[1]

                // 获取应用详细信息
                getAppInfo(packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun getAppInfo(packageName: String): AppInfo {
        val dumpOutput = adbDevicePoller.exec("shell dumpsys package $packageName")
        
        return AppInfo(
            packageName = packageName,
            appName = getAppName(packageName),
            versionName = parseVersionName(dumpOutput),
            versionCode = parseVersionCode(dumpOutput),
            isSystem = parseIsSystem(dumpOutput),
            isEnabled = parseIsEnabled(dumpOutput),
            installTime = parseInstallTime(dumpOutput),
            updateTime = parseUpdateTime(dumpOutput),
            size = parseSize(dumpOutput),
            targetSdk = parseTargetSdk(dumpOutput),
            minSdk = parseMinSdk(dumpOutput),
            permissions = parsePermissions(dumpOutput)
        )
    }

    private suspend fun getAppName(packageName: String): String {
        val output = adbDevicePoller.exec("shell pm dump $packageName | grep -A1 'android.intent.action.MAIN'")
        return output.firstOrNull()?.substringAfter("android.intent.action.MAIN:")?.trim() ?: packageName
    }

    private fun parseVersionName(output: List<String>): String {
        return output.find { it.contains("versionName=") }?.substringAfter("versionName=")?.trim() ?: ""
    }

    private fun parseVersionCode(output: List<String>): Long {
        return output.find { it.contains("versionCode=") }?.substringAfter("versionCode=")?.trim()?.toLongOrNull() ?: 0L
    }

    private fun parseIsSystem(output: List<String>): Boolean {
        return output.any { it.contains("pkgFlags= SYSTEM") }
    }

    private fun parseIsEnabled(output: List<String>): Boolean {
        return !output.any { it.contains("enabled=false") }
    }

    private fun parseInstallTime(output: List<String>): Long {
        return output.find { it.contains("firstInstallTime=") }?.substringAfter("firstInstallTime=")?.trim()?.toLongOrNull() ?: 0L
    }

    private fun parseUpdateTime(output: List<String>): Long {
        return output.find { it.contains("lastUpdateTime=") }?.substringAfter("lastUpdateTime=")?.trim()?.toLongOrNull() ?: 0L
    }

    private suspend fun parseSize(output: List<String>): Long {
        return output.find { it.contains("codePath=") }?.let { line ->
            val path = line.substringAfter("codePath=").trim()
            val sizeOutput = adbDevicePoller.exec("shell ls -l $path")
            sizeOutput.firstOrNull()?.split("\\s+".toRegex())?.getOrNull(4)?.toLongOrNull() ?: 0L
        } ?: 0L
    }

    private fun parseTargetSdk(output: List<String>): Int {
        return output.find { it.contains("targetSdk=") }?.substringAfter("targetSdk=")?.trim()?.toIntOrNull() ?: 0
    }

    private fun parseMinSdk(output: List<String>): Int {
        return output.find { it.contains("minSdk=") }?.substringAfter("minSdk=")?.trim()?.toIntOrNull() ?: 0
    }

    private fun parsePermissions(output: List<String>): List<String> {
        val permissions = mutableListOf<String>()
        var inPermissions = false
        
        for (line in output) {
            if (line.contains("requested permissions:")) {
                inPermissions = true
                continue
            }
            if (inPermissions) {
                if (line.trim().isEmpty()) {
                    break
                }
                if (line.contains(":")) {
                    permissions.add(line.trim())
                }
            }
        }
        
        return permissions
    }
} 
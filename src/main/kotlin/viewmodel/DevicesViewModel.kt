package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import model.DeviceInfo
import model.CpuInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import runtime.adb.AdbDevice
import runtime.adb.AdbDevicePoller
import util.FormatUtil

class DevicesViewModel : ViewModel(), KoinComponent {

    companion object {
        // 设备基本信息
        private const val CMD_DEVICE_MODEL = "shell getprop ro.product.model"
        private const val CMD_ANDROID_VERSION = "shell getprop ro.build.version.release"
        private const val CMD_BUILD_NUMBER = "shell getprop ro.build.display.id"
        private const val CMD_DEVICE_ID = "shell getprop ro.serialno"

        // CPU 信息
        private const val CMD_CPU_ARCH = "shell getprop ro.product.cpu.abi"
        private const val CMD_CPU_CORES = "shell cat /proc/cpuinfo | grep 'processor' | wc -l"
        private const val CMD_CPU_CACHE = "shell cat /proc/cpuinfo | grep 'cache size'"
        private const val CMD_CPU_FREQ = "shell cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq"

        // 内存信息
        private const val CMD_MEMORY_INFO = "shell cat /proc/meminfo"

        // 存储信息
        private const val CMD_STORAGE_INFO = "shell df /data"

        // 电池信息
        private const val CMD_BATTERY_LEVEL = "shell dumpsys battery | grep level"
        private const val CMD_BATTERY_STATUS = "shell dumpsys battery | grep status"

        // 网络信息
        private const val CMD_WIFI_STATUS = "shell dumpsys wifi | grep 'Wi-Fi is'"
        private const val CMD_MOBILE_DATA_STATUS = "shell dumpsys telephony.registry | grep 'mDataConnectionState'"
        private const val CMD_IP_ADDRESS = "shell ip addr show wlan0 | grep 'inet '"
    }


    private val adbDevicePoller: AdbDevicePoller by inject()

    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<AdbDevice>>(emptyList())
    val connectedDevices: StateFlow<List<AdbDevice>> get() = _connectedDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<AdbDevice?>(null)
    val selectedDevice: StateFlow<AdbDevice?> get() = _selectedDevice.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        startDevicePolling()
        startCpuUpdates()
    }

    private fun startDevicePolling() {
        adbDevicePoller.poll { devices ->
            _connectedDevices.value = devices
            updateSelectedDevice(devices)
        }
    }

    /**
     * Update the selected device when the device list changes
     */
    private fun updateSelectedDevice(devices: List<AdbDevice>) {
        if (devices.isEmpty()) {
            _selectedDevice.value = null
            adbDevicePoller.disconnect()
        } else if (_selectedDevice.value == null || !devices.contains(_selectedDevice.value)) {
            devices.firstOrNull()?.let { device ->
                _selectedDevice.value = device
                adbDevicePoller.connect(device)
            }
        }
    }

    fun connect(device: AdbDevice){
        adbDevicePoller.connect(device)
        _selectedDevice.value = device
    }

    private fun startCpuUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_selectedDevice.value == null){
                    continue
                }

                try {
                    val cpuFreqDeferred = async { adbDevicePoller.exec(CMD_CPU_FREQ) }
                    val frequencies = parseCpuFrequencies(cpuFreqDeferred.await())

                    _deviceInfo.value = _deviceInfo.value.copy(
                        cpuInfo = _deviceInfo.value.cpuInfo.copy(
                            frequencies = frequencies
                        )
                    )

                    delay(500) // 每0.5秒更新一次
                } catch (e: Exception) {
                    // 忽略更新错误，继续尝试
                }
            }
        }
    }

    fun refreshDeviceInfo() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 并发执行所有命令
                val deviceModelDeferred = async { adbDevicePoller.exec(CMD_DEVICE_MODEL) }
                val androidVersionDeferred = async { adbDevicePoller.exec(CMD_ANDROID_VERSION) }
                val buildNumberDeferred = async { adbDevicePoller.exec(CMD_BUILD_NUMBER) }
                val deviceIdDeferred = async { adbDevicePoller.exec(CMD_DEVICE_ID) }
                val cpuArchDeferred = async { adbDevicePoller.exec(CMD_CPU_ARCH) }
                val cpuCoresDeferred = async { adbDevicePoller.exec(CMD_CPU_CORES) }
                val cpuCacheDeferred = async { adbDevicePoller.exec(CMD_CPU_CACHE) }
                val cpuFreqDeferred = async { adbDevicePoller.exec(CMD_CPU_FREQ) }
                val memoryInfoDeferred = async { adbDevicePoller.exec(CMD_MEMORY_INFO) }
                val storageInfoDeferred = async { adbDevicePoller.exec(CMD_STORAGE_INFO) }
                val batteryLevelDeferred = async { adbDevicePoller.exec(CMD_BATTERY_LEVEL) }
                val batteryStatusDeferred = async { adbDevicePoller.exec(CMD_BATTERY_STATUS) }
                val wifiStatusDeferred = async { adbDevicePoller.exec(CMD_WIFI_STATUS) }
                val mobileDataStatusDeferred = async { adbDevicePoller.exec(CMD_MOBILE_DATA_STATUS) }
                val ipAddressDeferred = async { adbDevicePoller.exec(CMD_IP_ADDRESS) }

                // 等待所有命令执行完成
                val deviceModel = deviceModelDeferred.await().firstOrNull() ?: ""
                val androidVersion = androidVersionDeferred.await().firstOrNull() ?: ""
                val buildNumber = buildNumberDeferred.await().firstOrNull() ?: ""
                val deviceId = deviceIdDeferred.await().firstOrNull() ?: ""
                val cpuInfo = parseCpuInfo(
                    cpuArchDeferred.await().firstOrNull() ?: "",
                    cpuCoresDeferred.await().firstOrNull() ?: "0",
                    cpuCacheDeferred.await(),
                    cpuFreqDeferred.await()
                )
                val (totalMemory, usedMemory, memoryUsagePercent) = parseMemoryInfo(memoryInfoDeferred.await())
                val (totalStorage, availableStorage, storageUsagePercent) = parseStorageInfo(storageInfoDeferred.await())
                val batteryLevel = parseBatteryLevel(batteryLevelDeferred.await())
                val isCharging = parseBatteryStatus(batteryStatusDeferred.await())
                val wifiEnabled = parseWifiStatus(wifiStatusDeferred.await())
                val mobileDataEnabled = parseMobileDataStatus(mobileDataStatusDeferred.await())
                val ipAddress = parseIpAddress(ipAddressDeferred.await())

                // 更新设备信息
                _deviceInfo.value = DeviceInfo(
                    model = deviceModel,
                    androidVersion = androidVersion,
                    buildNumber = buildNumber,
                    deviceId = deviceId,
                    cpuInfo = cpuInfo,
                    totalMemory = totalMemory,
                    usedMemory = usedMemory,
                    memoryUsagePercent = memoryUsagePercent,
                    totalStorage = totalStorage,
                    availableStorage = availableStorage,
                    storageUsagePercent = storageUsagePercent,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    wifiEnabled = wifiEnabled,
                    mobileDataEnabled = mobileDataEnabled,
                    ipAddress = ipAddress
                )
            } catch (e: Exception) {
                _deviceInfo.value = DeviceInfo(error = "获取设备信息失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseCpuInfo(
        archOutput: String,
        coresOutput: String,
        cacheOutput: List<String>,
        freqOutput: List<String>
    ): CpuInfo {
        // 解析 CPU 架构
        val architecture = archOutput.trim()

        // 解析 CPU 核心数
        val cores = coresOutput.trim().toIntOrNull() ?: 0

        // 解析 CPU 缓存
        val cache = cacheOutput.firstOrNull()?.let { line ->
            line.split(":").getOrNull(1)?.trim()
        } ?: "Unknown"

        // 解析 CPU 频率
        val frequencies = parseCpuFrequencies(freqOutput)
        val frequency = frequencies.firstOrNull() ?: "Unknown"

        return CpuInfo(
            architecture = architecture,
            cores = cores,
            frequency = frequency,
            cache = cache,
            frequencies = frequencies
        )
    }

    private fun parseCpuFrequencies(output: List<String>): List<String> {
        return output.mapNotNull { freq ->
            freq.trim().toIntOrNull()?.let { freqValue ->
                when {
                    freqValue >= 1000000 -> String.format("%.2f GHz", freqValue / 1000000.0)
                    freqValue >= 1000 -> String.format("%.0f MHz", freqValue / 1000.0)
                    else -> "$freqValue Hz"
                }
            }
        }
    }

    private fun parseMemoryInfo(output: List<String>): Triple<String, String, Float> {
        val totalMem = output.find { it.contains("MemTotal") }?.split(":")?.get(1)?.trim() ?: "0 KB"
        val availMem = output.find { it.contains("MemAvailable") }?.split(":")?.get(1)?.trim() ?: "0 KB"

        // 将 KB 转换为字节并格式化
        val totalBytes = FormatUtil.parseMemoryString(totalMem)
        val availBytes = FormatUtil.parseMemoryString(availMem)
        val usedBytes = totalBytes - availBytes

        // 计算使用率
        val usagePercent = if (totalBytes > 0) {
            (usedBytes.toFloat() / totalBytes * 100).coerceIn(0f, 100f)
        } else {
            0f
        }

        return Triple(
            FormatUtil.formatMemory(totalBytes),
            FormatUtil.formatMemory(usedBytes),
            usagePercent
        )
    }

    private fun parseStorageInfo(output: List<String>): Triple<String, String, Float> {
        val parts = output.getOrNull(1)?.split("\\s+".toRegex()) ?: return Triple("0 B", "0 B", 0f)
        val total = parts.getOrNull(1) ?: "0"
        val available = parts.getOrNull(3) ?: "0"

        // 将 KB 转换为字节并格式化
        val totalBytes = total.toLongOrNull()?.times(1024) ?: 0L
        val availableBytes = available.toLongOrNull()?.times(1024) ?: 0L
        val usedBytes = totalBytes - availableBytes

        // 计算使用率
        val usagePercent = if (totalBytes > 0) {
            (usedBytes.toFloat() / totalBytes * 100).coerceIn(0f, 100f)
        } else {
            0f
        }

        return Triple(
            FormatUtil.formatMemory(totalBytes),
            FormatUtil.formatMemory(usedBytes),
            usagePercent
        )
    }

    private fun parseBatteryLevel(output: List<String>): Int {
        return output.firstOrNull()?.let { line ->
            line.split(":").getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        } ?: 0
    }

    private fun parseBatteryStatus(output: List<String>): Boolean {
        return output.firstOrNull()?.contains("Charging") ?: false
    }

    private fun parseWifiStatus(output: List<String>): Boolean {
        return output.firstOrNull()?.contains("enabled") ?: false
    }

    private fun parseMobileDataStatus(output: List<String>): Boolean {
        return output.firstOrNull()?.contains("2") ?: false
    }

    private fun parseIpAddress(output: List<String>): String {
        return output.firstOrNull()?.let { line ->
            line.split("inet ").getOrNull(1)?.split("/")?.getOrNull(0) ?: "Unknown"
        } ?: "Unknown"
    }

    override fun onCleared() {
        super.onCleared()
    }
}
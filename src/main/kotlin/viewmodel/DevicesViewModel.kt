package viewmodel

import MemoryInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import model.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import runtime.adb.AdbDevice
import runtime.adb.AdbDevicePoller
import utils.FormatUtils

class DevicesViewModel : ViewModel(), KoinComponent {

    companion object {
        // 设备基本信息
        private const val CMD_DEVICE_MODEL = "shell getprop ro.product.model"
        private const val CMD_ANDROID_VERSION = "shell getprop ro.build.version.release"
        private const val CMD_BUILD_NUMBER = "shell getprop ro.build.display.id"
        private const val CMD_DEVICE_ID = "shell getprop ro.serialno"
        private const val CMD_ROOT_STATUS = "shell which su"

        // CPU 信息
        private const val CMD_CPU_ARCH = "shell getprop ro.product.cpu.abi"
        private const val CMD_CPU_CORES = "shell cat /proc/cpuinfo | grep 'processor' | wc -l"
        private const val CMD_CPU_CACHE = "shell cat /proc/cpuinfo | grep 'cache size'"
        private const val CMD_CPU_FREQ = "shell cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq"

        // 内存信息
        private const val CMD_MEMORY_INFO = "shell cat /proc/meminfo"
        private const val CMD_SWAP_INFO = "shell cat /proc/swaps"
        private const val CMD_ZRAM_INFO = "shell cat /sys/block/zram0/disksize"
        private const val CMD_ZRAM_USED = "shell cat /sys/block/zram0/mem_used_total"

        // 存储信息
        private const val CMD_STORAGE_INFO = "shell df /data"

        // 电池信息
        private const val CMD_BATTERY_LEVEL = "shell dumpsys battery | grep level"
        private const val CMD_BATTERY_STATUS = "shell dumpsys battery | grep status"
        private const val CMD_BATTERY_INFO = "shell dumpsys battery"

        // 网络信息
        private const val CMD_WIFI_STATUS = "shell dumpsys wifi | grep 'Wi-Fi is'"
        private const val CMD_MOBILE_DATA_STATUS = "shell dumpsys telephony.registry | grep 'mDataConnectionState'"
        private const val CMD_IP_ADDRESS = "shell ip addr show wlan0 | grep 'inet '"
    }


    private val adbDevicePoller: AdbDevicePoller by inject()

    // 基本设备信息
    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    // CPU 信息
    private val _cpuInfo = MutableStateFlow(CpuInfo())
    val cpuInfo: StateFlow<CpuInfo> = _cpuInfo.asStateFlow()

    // 内存信息
    private val _memoryInfo = MutableStateFlow(MemoryInfo())
    val memoryInfo: StateFlow<MemoryInfo> = _memoryInfo.asStateFlow()

    // 电池信息
    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    // 存储信息
    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    // 网络信息
    private val _networkInfo = MutableStateFlow(NetworkInfo())
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<AdbDevice>>(emptyList())
    val connectedDevices: StateFlow<List<AdbDevice>> = _connectedDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<AdbDevice?>(null)
    val selectedDevice: StateFlow<AdbDevice?> = _selectedDevice.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        println("Debug - Initializing DevicesViewModel")
        startDevicePolling()
        startCpuUpdates()
    }

    private fun startDevicePolling() {
        println("Debug - Starting device polling")
        adbDevicePoller.poll { devices ->
            println("Debug - Received devices from poller: $devices")
            _connectedDevices.value = devices
            updateSelectedDevice(devices)
        }
    }

    /**
     * Update the selected device when the device list changes
     */
    private fun updateSelectedDevice(devices: List<AdbDevice>) {
        println("Debug - Updating selected device from list: $devices")
        if (devices.isEmpty()) {
            println("Debug - No devices available, disconnecting")
            _selectedDevice.value = null
            adbDevicePoller.disconnect()
        } else if (_selectedDevice.value == null || !devices.contains(_selectedDevice.value)) {
            devices.firstOrNull()?.let { device ->
                println("Debug - Selecting new device: ${device.deviceId}")
                _selectedDevice.value = device
                adbDevicePoller.connect(device)
            }
        }
    }

    fun connect(device: AdbDevice) {
        println("Debug - Connecting to device: ${device.deviceId}")
        adbDevicePoller.connect(device)
        _selectedDevice.value = device
    }

    private fun startCpuUpdates() {
        println("Debug - Starting CPU updates")
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_selectedDevice.value == null) {
                    println("Debug - No device selected, skipping CPU update")
                    continue
                }

                try {
                    println("Debug - Updating CPU frequencies")
                    val cpuFreqDeferred = async { adbDevicePoller.exec(CMD_CPU_FREQ) }
                    val frequencies = parseCpuFrequencies(cpuFreqDeferred.await())
                    println("Debug - Parsed CPU frequencies: $frequencies")

                    _cpuInfo.value = _cpuInfo.value.copy(frequencies = frequencies)

                    delay(500) // 每0.5秒更新一次
                } catch (e: Exception) {
                    println("Debug - Error updating CPU frequencies: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun refreshDeviceInfo() {
        println("Debug - Starting device info refresh")
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_selectedDevice.value == null) {
                    println("Debug - No device selected, cannot refresh info")
                    return@launch
                }

                // 并发执行所有命令
                val deviceModelDeferred = async { adbDevicePoller.exec(CMD_DEVICE_MODEL) }
                val androidVersionDeferred = async { adbDevicePoller.exec(CMD_ANDROID_VERSION) }
                val buildNumberDeferred = async { adbDevicePoller.exec(CMD_BUILD_NUMBER) }
                val deviceIdDeferred = async { adbDevicePoller.exec(CMD_DEVICE_ID) }
                val rootStatusDeferred = async { adbDevicePoller.exec(CMD_ROOT_STATUS) }

                // 更新基本设备信息
                _deviceInfo.value = DeviceInfo(
                    model = deviceModelDeferred.await().firstOrNull() ?: "",
                    androidVersion = androidVersionDeferred.await().firstOrNull() ?: "",
                    buildNumber = buildNumberDeferred.await().firstOrNull() ?: "",
                    deviceId = deviceIdDeferred.await().firstOrNull() ?: "",
                    isRooted = rootStatusDeferred.await().firstOrNull()?.isNotEmpty() == true
                )

                // 刷新其他信息
                refreshCpuInfo()
                refreshMemoryInfo()
                refreshBatteryInfo()
                refreshStorageInfo()
                refreshNetworkInfo()

            } catch (e: Exception) {
                println("Debug - Error refreshing device info: ${e.message}")
                e.printStackTrace()
                _deviceInfo.value = DeviceInfo(error = "获取设备信息失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun refreshCpuInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cpuArchDeferred = async { adbDevicePoller.exec(CMD_CPU_ARCH) }
                val cpuCoresDeferred = async { adbDevicePoller.exec(CMD_CPU_CORES) }
                val cpuCacheDeferred = async { adbDevicePoller.exec(CMD_CPU_CACHE) }
                val cpuFreqDeferred = async { adbDevicePoller.exec(CMD_CPU_FREQ) }

                _cpuInfo.value = parseCpuInfo(
                    cpuArchDeferred.await().firstOrNull() ?: "",
                    cpuCoresDeferred.await().firstOrNull() ?: "0",
                    cpuCacheDeferred.await(),
                    cpuFreqDeferred.await()
                )
            } catch (e: Exception) {
                println("Debug - Error refreshing CPU info: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun refreshMemoryInfo() {
        println("Debug - Refreshing memory info")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val memoryInfoDeferred = async { adbDevicePoller.exec(CMD_MEMORY_INFO) }
                val swapInfoDeferred = async { adbDevicePoller.exec(CMD_SWAP_INFO) }
                val zramInfoDeferred = async { adbDevicePoller.exec(CMD_ZRAM_INFO) }
                val zramUsedDeferred = async { adbDevicePoller.exec(CMD_ZRAM_USED) }

                _memoryInfo.value = parseMemoryInfo(
                    memoryInfoDeferred.await(),
                    swapInfoDeferred.await(),
                    zramInfoDeferred.await(),
                    zramUsedDeferred.await()
                )
            } catch (e: Exception) {
                println("Debug - Error refreshing memory info: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun refreshBatteryInfo() {
        println("Debug - Refreshing battery info")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val batteryInfoDeferred = async { adbDevicePoller.exec(CMD_BATTERY_INFO) }
                _batteryInfo.value = parseBatteryInfo(batteryInfoDeferred.await())
            } catch (e: Exception) {
                println("Debug - Error refreshing battery info: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun refreshStorageInfo() {
        println("Debug - Refreshing storage info")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val storageInfoDeferred = async { adbDevicePoller.exec(CMD_STORAGE_INFO) }
                _storageInfo.value = parseStorageInfo(storageInfoDeferred.await())
            } catch (e: Exception) {
                println("Debug - Error refreshing storage info: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun refreshNetworkInfo() {
        println("Debug - Refreshing network info")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wifiStatusDeferred = async { adbDevicePoller.exec(CMD_WIFI_STATUS) }
                val mobileDataStatusDeferred = async { adbDevicePoller.exec(CMD_MOBILE_DATA_STATUS) }
                val ipAddressDeferred = async { adbDevicePoller.exec(CMD_IP_ADDRESS) }

                _networkInfo.value = NetworkInfo(
                    wifiEnabled = parseWifiStatus(wifiStatusDeferred.await()),
                    mobileDataEnabled = parseMobileDataStatus(mobileDataStatusDeferred.await()),
                    ipAddress = parseIpAddress(ipAddressDeferred.await())
                )
            } catch (e: Exception) {
                println("Debug - Error refreshing network info: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun parseCpuInfo(
        archOutput: String,
        coresOutput: String,
        cacheOutput: List<String>,
        freqOutput: List<String>
    ): CpuInfo {
        println("Debug - Parsing CPU info:")
        println("Architecture: $archOutput")
        println("Cores: $coresOutput")
        println("Cache: $cacheOutput")
        println("Frequencies: $freqOutput")

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
        println("Debug - Parsing CPU frequencies from: $output")
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

    private fun parseMemoryInfo(
        memoryInfoOutput: List<String>,
        swapOutput: List<String>,
        zramOutput: List<String>,
        zramUsedOutput: List<String>
    ): MemoryInfo {

        val memInfo = memoryInfoOutput.associate { line ->
            val parts = line.split(":")
            if (parts.size >= 2) {
                parts[0].trim() to parts[1].replace("kB", "").trim()
            } else {
                println("Debug - Invalid memory info line format: '$line'")
                "" to ""
            }
        }

        println("Debug - Parsed memory info map:")
        memInfo.forEach { (key, value) ->
            println("$key: $value")
        }

        // 总内存
        val totalMem = memInfo["MemTotal"]?.toLongOrNull() ?: 0L
        // 可用内存
        val availableMen = memInfo["MemAvailable"]?.toLongOrNull() ?: 0L
        // 已用内存
        val usedMem = totalMem - availableMen
        println("Debug - Calculated used memory: $usedMem")

        // 计算使用率
        val usagePercent = if (totalMem > 0) {
            (usedMem.toFloat() * 100 / totalMem.toFloat()).coerceIn(0f, 100f)
        } else {
            println("Debug - Total memory is 0, cannot calculate usage percent")
            0f
        }

        // 解析 swap 信息
        val swapTotal =
            swapOutput.getOrNull(1)?.split("\\s+".toRegex())?.getOrNull(2)?.toLongOrNull()?.times(1024) ?: 0L
        val swapUsed = swapOutput.getOrNull(1)?.split("\\s+".toRegex())?.getOrNull(3)?.toLongOrNull()?.times(1024) ?: 0L

        println("Debug - Swap partition info:")
        println("Total: $swapTotal")
        println("Used: $swapUsed")

        // 如果没有 swap 分区，检查是否有 zram
        var finalSwapTotal = swapTotal
        var finalSwapUsed = swapUsed
        if (swapTotal == 0L) {
            println("Debug - No swap partition, checking ZRAM")
            val zramSize = zramOutput.firstOrNull()?.toLongOrNull() ?: 0L
            val zramUsed = zramUsedOutput.firstOrNull()?.toLongOrNull() ?: 0L

            println("Debug - ZRAM info:")
            println("Size: $zramSize")
            println("Used: $zramUsed")

            if (zramSize > 0) {
                finalSwapTotal = zramSize
                finalSwapUsed = zramUsed
            }
        }

        // 计算 swap 使用率
        val swapUsagePercent = if (finalSwapTotal > 0) {
            (finalSwapUsed.toFloat() / finalSwapTotal * 100).coerceIn(0f, 100f)
        } else {
            0f
        }

        println("Debug - Swap usage percent: $swapUsagePercent%")

        return MemoryInfo(
            total = FormatUtils.formatMemory(totalMem.toString()),
            free = FormatUtils.formatMemory(memInfo["MemFree"]),
            available = FormatUtils.formatMemory(availableMen.toString()),
            used = FormatUtils.formatMemory(usedMem.toString()),
            usagePercent = usagePercent,
            buffers = FormatUtils.formatMemory(memInfo["Buffers"]),
            cached = FormatUtils.formatMemory(memInfo["Cached"]),
            swapCached = FormatUtils.formatMemory(memInfo["SwapCached"]),
            active = FormatUtils.formatMemory(memInfo["Active"]),
            inactive = FormatUtils.formatMemory(memInfo["Inactive"]),
            activeAnon = FormatUtils.formatMemory(memInfo["Active(anon)"]),
            inactiveAnon = FormatUtils.formatMemory(memInfo["Inactive(anon)"]),
            activeFile = FormatUtils.formatMemory(memInfo["Active(file)"]),
            inactiveFile = FormatUtils.formatMemory(memInfo["Inactive(file)"]),
            swapTotal = FormatUtils.formatMemory(finalSwapTotal),
            swapFree = FormatUtils.formatMemory(finalSwapTotal - finalSwapUsed),
            swapUsed = FormatUtils.formatMemory(finalSwapUsed),
            swapUsagePercent = swapUsagePercent,
            anonPages = FormatUtils.formatMemory(memInfo["AnonPages"]),
            mapped = FormatUtils.formatMemory(memInfo["Mapped"]),
            shmem = FormatUtils.formatMemory(memInfo["Shmem"]),
            slab = FormatUtils.formatMemory(memInfo["Slab"]),
            kernelStack = FormatUtils.formatMemory(memInfo["KernelStack"]),
            pageTables = FormatUtils.formatMemory(memInfo["PageTables"]),
            vmallocUsed = FormatUtils.formatMemory(memInfo["VmallocUsed"])
        )
    }

    private fun parseStorageInfo(output: List<String>): StorageInfo {
        println("Debug - Parsing storage info from: $output")
        val parts = output.getOrNull(1)?.split("\\s+".toRegex()) ?: run {
            println("Debug - Invalid storage info format")
            return StorageInfo()
        }
        val total = parts.getOrNull(1) ?: "0"
        val available = parts.getOrNull(3) ?: "0"

        println("Debug - Storage info parts:")
        println("Total: $total")
        println("Available: $available")

        // 将 KB 转换为字节并格式化
        val totalBytes = total.toLongOrNull()?.times(1024) ?: 0L
        val availableBytes = available.toLongOrNull()?.times(1024) ?: 0L
        val usedBytes = totalBytes - availableBytes

        println("Debug - Storage bytes:")
        println("Total bytes: $totalBytes")
        println("Available bytes: $availableBytes")
        println("Used bytes: $usedBytes")

        // 计算使用率
        val usagePercent = if (totalBytes > 0) {
            (usedBytes.toFloat() / totalBytes * 100).coerceIn(0f, 100f)
        } else {
            println("Debug - Total storage is 0, cannot calculate usage percent")
            0f
        }

        println("Debug - Storage usage percent: $usagePercent%")

        return StorageInfo(
            total = FormatUtils.formatMemory(totalBytes),
            used = FormatUtils.formatMemory(usedBytes),
            available = FormatUtils.formatMemory(availableBytes),
            usagePercent = usagePercent
        )
    }

    private fun parseBatteryLevel(output: List<String>): Int {
        println("Debug - Parsing battery level from: $output")
        return output.firstOrNull()?.let { line ->
            line.split(":").getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        } ?: 0
    }

    private fun parseBatteryStatus(output: List<String>): Boolean {
        println("Debug - Parsing battery status from: $output")
        return output.firstOrNull()?.contains("Charging") ?: false
    }

    private fun parseWifiStatus(output: List<String>): Boolean {
        println("Debug - Parsing WiFi status from: $output")
        return output.firstOrNull()?.contains("enabled") ?: false
    }

    private fun parseMobileDataStatus(output: List<String>): Boolean {
        println("Debug - Parsing mobile data status from: $output")
        return output.firstOrNull()?.contains("2") ?: false
    }

    private fun parseIpAddress(output: List<String>): String {
        println("Debug - Parsing IP address from: $output")
        return output.firstOrNull()?.let { line ->
            line.split("inet ").getOrNull(1)?.split("/")?.getOrNull(0) ?: "Unknown"
        } ?: "Unknown"
    }

    private fun parseBatteryInfo(output: List<String>): BatteryInfo {
        println("Debug - Parsing battery info from: $output")
        var level = 0
        var scale = 100
        var voltage = 0
        var temperature = 0f
        var technology = ""
        var health = 0
        var status = 0
        var isAcPowered = false
        var isUsbPowered = false
        var isWirelessPowered = false
        var chargeCounter = 0
        var maxChargingCurrent = 0
        var maxChargingVoltage = 0
        var isPresent = true

        output.forEach { line ->
            when {
                line.contains("AC powered:") -> isAcPowered = line.contains("true")
                line.contains("USB powered:") -> isUsbPowered = line.contains("true")
                line.contains("Wireless powered:") -> isWirelessPowered = line.contains("true")
                line.contains("Max charging current:") -> maxChargingCurrent =
                    line.substringAfter(":").trim().toIntOrNull() ?: 0

                line.contains("Max charging voltage:") -> maxChargingVoltage =
                    line.substringAfter(":").trim().toIntOrNull() ?: 0

                line.contains("Charge counter:") -> chargeCounter = line.substringAfter(":").trim().toIntOrNull() ?: 0
                line.contains("status:") -> status = line.substringAfter(":").trim().toIntOrNull() ?: 0
                line.contains("health:") -> health = line.substringAfter(":").trim().toIntOrNull() ?: 0
                line.contains("present:") -> isPresent = line.contains("true")
                line.contains("level:") -> level = line.substringAfter(":").trim().toIntOrNull() ?: 0
                line.contains("scale:") -> scale = line.substringAfter(":").trim().toIntOrNull() ?: 100
                line.contains("voltage:") -> voltage = line.substringAfter(":").trim().toIntOrNull() ?: 0
                line.contains("temperature:") -> {
                    val temp = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    temperature = temp / 10f // 转换为摄氏度
                }

                line.contains("technology:") -> technology = line.substringAfter(":").trim()
            }
        }

        return BatteryInfo(
            level = level,
            scale = scale,
            voltage = voltage,
            temperature = temperature,
            technology = technology,
            health = health,
            status = status,
            isAcPowered = isAcPowered,
            isUsbPowered = isUsbPowered,
            isWirelessPowered = isWirelessPowered,
            chargeCounter = chargeCounter,
            maxChargingCurrent = maxChargingCurrent,
            maxChargingVoltage = maxChargingVoltage,
            isPresent = isPresent
        )
    }

    override fun onCleared() {
        println("Debug - Clearing DevicesViewModel")
        super.onCleared()
    }
}
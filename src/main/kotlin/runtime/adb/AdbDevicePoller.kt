package runtime.adb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import runtime.adb.env.AppContext

class AdbDevicePoller(
    private val adb: Adb,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val POLLING_INTERVAL_MS = 3000L
    }

    private var pollingJob: Job? = null
    private var currentDevice: AdbDevice? = null

    fun poll(onResult: (List<AdbDevice>) -> Unit) {
        stopPolling()

        pollingJob = coroutineScope.launch {
            while (true) {
                try {
                    val deviceIds = adb.devices()
                    val devices = deviceIds.map { deviceId ->
                        val wifiState = adb.wifiState(deviceId)
                        AdbDevice(deviceId, wifiState)
                    }
                    onResult.invoke(devices)
                } catch (e: Exception) {
                    // Handle error silently, will try again on next poll
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    suspend fun request(): List<AndroidVirtualDevice> {
        return try {
            adb.listAvds()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun exec(cmd: String): List<String> {
        if (currentDevice == null) {
            return listOf("No device connected")
        }

        return try {
            adb.exec(currentDevice!!, cmd)
        } catch (e: Exception) {
            listOf("Error: ${e.message}")
        }
    }

    fun connect(device: AdbDevice) {
        currentDevice = device
        AppContext.adbDevice = device
    }

    fun disconnect() {
        currentDevice = null
        AppContext.adbDevice = null
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
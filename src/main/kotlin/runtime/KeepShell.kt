package runtime

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import runtime.adb.AdbDevicePoller
import runtime.adb.env.AppContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException

class KeepShell(adbDevicePoller: AdbDevicePoller, private val adbStore: AdbStore) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    private var process: Process? = null

    private var inputReader: BufferedReader? = null
    private var outputWriter: BufferedWriter? = null
    private var errorReader: BufferedReader? = null

    // 当前连接设备Id
    private var currentConnectId: String = ""


    init {
        adbDevicePoller.poll {
            val lastConnectId = AppContext.adbDevice?.deviceId ?: ""
            if (lastConnectId != currentConnectId) {
                if (lastConnectId.isEmpty()) {
                    if (currentConnectId.isNotEmpty()) {
                        disposeCurrentShell()
                        currentConnectId = ""
                    }
                } else {
                    // 重新连接设备
                    disposeCurrentShell()
                    // 建立新连接
                    currentConnectId = lastConnectId
                    scope.launch {
                        startShellProcess()
                    }
                }
            }
        }
    }

    private var isDisposed = false


    /**
     * 启动并初始化 shell 进程
     */
    private suspend fun startShellProcess() {
        if (currentConnectId.isEmpty()) {
            return
        }
        try {
            process = withContext(Dispatchers.IO) {
                ProcessBuilder(adbStore.adbHostFile.absolutePath, "-s", currentConnectId, "shell")
                    .redirectErrorStream(true)
                    .start()
            }.also {
                outputWriter = it.outputStream.bufferedWriter()
                inputReader = it.inputStream.bufferedReader()
                errorReader = it.errorStream.bufferedReader()

                // 清除启动时的初始输出
                clearInitialOutput()
            }
        } catch (e: Exception) {
            disposeCurrentShell()
            println("Failed to connect to $currentConnectId: ${e.message}")
        }
    }

    /**
     * 执行 shell 命令并返回输出
     */
    suspend fun execute(cmd: String, timeoutMillis: Long = 10000): List<String> = mutex.withLock {
        check(!isDisposed) { "Shell manager is already disposed" }

        if (currentConnectId.isEmpty()) {
            return@withLock emptyList<String>()
        }


        // 执行前清理输入和错误缓冲区
        clearBuffer(inputReader)
        clearBuffer(errorReader)

        // 写入命令
        outputWriter?.write("$cmd\n")
        outputWriter?.flush()

        // 读取命令输出
        return withTimeoutOrNull(timeoutMillis) {
            val result = mutableListOf<String>()
            var line: String?

            // 读取直到找到命令提示符或超时
            while (true) {
                line = inputReader?.readLine() ?: break
                if (isCommandPrompt(line)) break
                result.add(line)
            }

            result
        } ?: throw Exception("Command execution timed out")
    }

    /**
     * 判断是否为命令提示符 (简化实现)
     */
    private fun isCommandPrompt(line: String): Boolean {
        // 简单判断，实际使用时可能需要更复杂的逻辑
        return line.trim().endsWith("#") || line.trim().endsWith("$")
    }

    /**
     * 清除 shell 启动时的初始输出
     */
    private suspend fun clearInitialOutput() {
        withContext(Dispatchers.IO) {
            // 给 shell 启动一些时间
            delay(500)

            // 读取并丢弃初始输出
            while (inputReader?.ready() == true) {
                inputReader?.readLine()
            }
        }
    }


    private fun disposeCurrentShell() {
        currentConnectId = ""
        try {
            outputWriter?.close()
            inputReader?.close()
            errorReader?.close()
        } catch (e: IOException) {
            // 忽略关闭错误
        }

        process?.destroyForcibly()
        process = null
        outputWriter = null
        inputReader = null
        errorReader = null
    }

    /**
     * 释放资源并终止 shell 进程
     */
    fun dispose() = scope.launch {
        mutex.withLock {
            if (isDisposed) return@withLock

            disposeCurrentShell()
            currentConnectId = ""
            isDisposed = true
            scope.cancel()
        }
    }

    fun isConnected(): Boolean = currentConnectId.isNotEmpty() && process?.isAlive == true

    private fun clearBuffer(reader: BufferedReader?) {
        try {
            while (reader?.ready() == true) {
                reader.read() // 丢弃所有数据
            }
        } catch (e: IOException) {
            // 忽略
        }
    }
}

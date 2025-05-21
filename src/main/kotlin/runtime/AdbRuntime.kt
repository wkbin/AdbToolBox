package runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.jixin.translato.toolbox.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Runtime

@Composable
fun initAdbRuntime(adbStore: AdbStore, onInitialized: () -> Unit) {
    LaunchedEffect(Unit) {
        val adbExecutable = File(adbStore.adbHostFile, "platform-tools/adb")
        
        // 检查 ADB 是否已经安装
        if (!adbExecutable.exists()) {
            // 如果 ADB 不存在，则进行安装
            adbStore.installRuntime(
                Res.readBytes(adbStore.resourceName),
                adbStore.adbHostFile.absolutePath
            )
        }

        // 检查并修复 ADB 执行权限（针对 Linux/Mac 系统）
        try {
            val osName = System.getProperty("os.name").lowercase()
            if (osName.contains("linux") || osName.contains("mac") || osName.contains("unix")) {
                if (adbExecutable.exists() && !adbExecutable.canExecute()) {
                    // 尝试设置可执行权限
                    withContext(Dispatchers.IO) {
                        val result = Runtime.getRuntime().exec(
                            arrayOf("chmod", "755", adbExecutable.absolutePath)
                        ).waitFor()

                        if (result != 0) {
                            // 如果 chmod 命令失败，尝试使用 ProcessBuilder
                            val processBuilder = ProcessBuilder("chmod", "755", adbExecutable.absolutePath)
                            processBuilder.start().waitFor()
                        }

                        println("已自动设置 ADB 可执行权限: ${adbExecutable.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            println("设置 ADB 可执行权限时出错: ${e.message}")
            e.printStackTrace()
        }

        onInitialized()
    }
}
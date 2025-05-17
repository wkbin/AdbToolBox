import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jixin.translato.toolbox.generated.resources.Res
import com.jixin.translato.toolbox.generated.resources.home
import com.jixin.translato.toolbox.generated.resources.settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import runtime.AdbStore
import runtime.ContextStore
import runtime.adb.Adb
import runtime.adb.Terminal
import java.io.File


val LocalWindow = compositionLocalOf<ComposeWindow> { error("Window not provided") }
val LocalAdb = compositionLocalOf<Adb> { error("Adb not provided") }
val LocalAdbStore = compositionLocalOf<AdbStore> { error("AdbStore not provided") }

@Composable
private fun initAdbRuntime(adbStore: AdbStore, onInitialized: () -> Unit) {
    LaunchedEffect(Unit) {
        adbStore.installRuntime(
            Res.readBytes(adbStore.resourceName),
            adbStore.adbHostFile.absolutePath
        )

        // 检查并修复 ADB 执行权限（针对 Linux/Mac 系统）
        try {
            val osName = System.getProperty("os.name").lowercase()
            if (osName.contains("linux") || osName.contains("mac") || osName.contains("unix")) {
                val adbExecutable = File(adbStore.adbHostFile, "platform-tools/adb")
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

@Composable
@Preview
fun App() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home" to Res.drawable.home, "Settings" to Res.drawable.settings)
    NavigationRail {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                icon = {
                    Icon(painter = painterResource(item.second), contentDescription = null)
                },
                label = { Text(item.first) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}

fun main() = application {
    val adbStore = AdbStore(ContextStore().fileDir)
    var isRuntimeInitialized by remember { mutableStateOf(false) }

    // 初始化ADB运行时
    initAdbRuntime(adbStore) {
        isRuntimeInitialized = true
    }

    Window(onCloseRequest = ::exitApplication) {
        if (isRuntimeInitialized){
            val adb = Adb("${adbStore.adbHostFile.absolutePath}${File.separator}platform-tools${File.separator}adb", Terminal())
            CompositionLocalProvider(
                LocalWindow provides window,
                LocalAdb provides adb,
                LocalAdbStore provides adbStore
            ) {
                App()
            }
        }
    }
}

package di

import org.koin.dsl.module
import runtime.adb.Adb
import runtime.adb.AdbDevicePoller
import runtime.adb.Terminal
import runtime.AdbStore
import runtime.ContextStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

val appModule = module {
    // 单例
    single { ContextStore() }
    single { AdbStore(get<ContextStore>().fileDir) }
    single { Terminal() }
    single {
        Adb(
            adbPath = "${get<AdbStore>().adbHostFile.absolutePath}${File.separator}platform-tools${File.separator}adb",
            terminal = get()
        )
    }
    single { CoroutineScope(Dispatchers.IO) }
    single { AdbDevicePoller(get(), get()) }
} 
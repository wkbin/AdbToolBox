import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TooltipDefaults.rememberRichTooltipPositionProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.compose.rememberNavController
import com.jixin.translato.toolbox.generated.resources.Res
import com.jixin.translato.toolbox.generated.resources.app_name
import di.appModule
import navigation.AppNavGraph
import navigation.NavScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import runtime.AdbStore
import runtime.initAdbRuntime

val LocalWindow = compositionLocalOf<ComposeWindow> { error("Window not provided") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    var selectedItem by remember { mutableStateOf(NavScreen.Devices) }

    Row {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavScreen.entries.forEach { item ->
                    TooltipBox(
                        positionProvider = rememberRichTooltipPositionProvider(), tooltip = {
                            PlainTooltip {
                                Text(
                                    stringResource(item.title), style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }, state = rememberTooltipState(), enableUserInput = selectedItem != item
                    ) {
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = stringResource(item.title)
                                )
                            },
                            label = { Text(stringResource(item.title)) },
                            selected = selectedItem == item,
                            onClick = {
                                selectedItem = item
                                navController.navigate(item.id)
                            },
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        }
        AppNavGraph(navController)
    }
}

class MainApplication : KoinComponent {
    val adbStore: AdbStore by inject()
}

fun main() = application {
    // 初始化 Koin
    startKoin {
        modules(appModule)
    }

    val mainApp = MainApplication()
    var isRuntimeInitialized by remember { mutableStateOf(false) }

    // 初始化ADB运行时
    initAdbRuntime(mainApp.adbStore) {
        isRuntimeInitialized = true
    }

    Window(title = stringResource(Res.string.app_name), onCloseRequest = ::exitApplication) {
        if (isRuntimeInitialized) {
            CompositionLocalProvider(
                LocalWindow provides window
            ) {
                App()
            }
        }
    }
}
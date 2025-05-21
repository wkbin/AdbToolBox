package navigation

import com.jixin.translato.toolbox.generated.resources.*
import com.jixin.translato.toolbox.generated.resources.Res
import com.jixin.translato.toolbox.generated.resources.apk
import com.jixin.translato.toolbox.generated.resources.devices
import com.jixin.translato.toolbox.generated.resources.home
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class NavScreen(val id: String, val title: StringResource, val icon: DrawableResource) {
    Devices("devices", Res.string.devices, Res.drawable.devices),
    Home("home", Res.string.home, Res.drawable.home),
    Apk("apk", Res.string.apk, Res.drawable.apk),
    File("file", Res.string.file, Res.drawable.file),
    Settings("settings", Res.string.settings, Res.drawable.settings),
}
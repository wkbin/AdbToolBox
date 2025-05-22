package ui.apk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jixin.translato.toolbox.generated.resources.Block
import com.jixin.translato.toolbox.generated.resources.Check
import com.jixin.translato.toolbox.generated.resources.Download
import com.jixin.translato.toolbox.generated.resources.Res
import model.AppInfo
import org.jetbrains.compose.resources.painterResource
import utils.FormatUtils
import viewmodel.ApkViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ApkScreen(
    viewModel: ApkViewModel = viewModel { ApkViewModel() }
) {
//    val apps by viewModel.apps.collectAsState()
//    val selectedApp by viewModel.selectedApp.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//
//    LaunchedEffect(Unit) {
//        viewModel.refreshApps()
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Row(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            // 应用列表
//            LazyColumn(
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxHeight()
//                    .padding(16.dp)
//            ) {
//                items(apps) { app ->
//                    AppListItem(
//                        app = app,
//                        onClick = { viewModel.selectApp(app) },
//                        isSelected = app == selectedApp
//                    )
//                }
//            }
//
//            // 应用详情
//            selectedApp?.let { app ->
//                AppDetails(
//                    app = app,
//                    onEnable = { viewModel.enableApp(app.packageName) },
//                    onDisable = { viewModel.disableApp(app.packageName) },
//                    onExtract = { viewModel.extractApk(app.packageName, "apks/${app.packageName}.apk") }
//                )
//            }
//        }
//
//        // 加载指示器
//        if (isLoading) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(48.dp),
//                    color = MaterialTheme.colorScheme.primary
//                )
//            }
//        }
//    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!app.isEnabled) {
                Icon(
                    painter = painterResource(Res.drawable.Block),
                    contentDescription = "已禁用",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AppDetails(
    app: AppInfo,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onExtract: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .width(400.dp)
            .fillMaxHeight()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = app.appName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (app.isEnabled) {
                    Button(
                        onClick = onDisable,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(painter = painterResource(Res.drawable.Block), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("冻结")
                    }
                } else {
                    Button(
                        onClick = onEnable,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(painter = painterResource(Res.drawable.Check), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("解冻")
                    }
                }
                Button(
                    onClick = onExtract,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painter = painterResource(Res.drawable.Download), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("提取")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 详细信息
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item {
                    DetailItem("版本", "${app.versionName} (${app.versionCode})")
                    DetailItem("大小", FormatUtils.formatMemory(app.size))
                    DetailItem("安装时间", dateFormat.format(Date(app.installTime)))
                    DetailItem("更新时间", dateFormat.format(Date(app.updateTime)))
                    DetailItem("目标 SDK", app.targetSdk.toString())
                    DetailItem("最低 SDK", app.minSdk.toString())
                    DetailItem("系统应用", if (app.isSystem) "是" else "否")
                    DetailItem("状态", if (app.isEnabled) "已启用" else "已禁用")
                }

                if (app.permissions.isNotEmpty()) {
                    item {
                        Text(
                            text = "权限",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(app.permissions) { permission ->
                        Text(
                            text = permission,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
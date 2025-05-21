package ui.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jixin.translato.toolbox.generated.resources.*
import com.jixin.translato.toolbox.generated.resources.NetworkCell
import com.jixin.translato.toolbox.generated.resources.Res
import com.jixin.translato.toolbox.generated.resources.wifi
import com.jixin.translato.toolbox.generated.resources.wifiOff
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ui.devices.components.CpuFrequencyChart
import viewmodel.DevicesViewModel

@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshDeviceInfo()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 基本信息
        InfoSection("基本信息") {
            DeviceInfoItem("设备型号", deviceInfo.model)
            DeviceInfoItem("Android 版本", deviceInfo.androidVersion)
            DeviceInfoItem("系统版本", deviceInfo.buildNumber)
            DeviceInfoItem("设备序列号", deviceInfo.deviceId)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CPU 信息
        InfoSection("CPU 信息") {
            DeviceInfoItem("架构", deviceInfo.cpuInfo.architecture)
            DeviceInfoItem("核心数", "${deviceInfo.cpuInfo.cores} cores")
            DeviceInfoItem("缓存", deviceInfo.cpuInfo.cache)

            // CPU 频率
            if (deviceInfo.cpuInfo.frequencies.isNotEmpty()) {
                DeviceInfoItem("当前频率", deviceInfo.cpuInfo.frequency)
                if (deviceInfo.cpuInfo.frequencies.size > 1) {
                    DeviceInfoItem("所有核心频率", deviceInfo.cpuInfo.frequencies.joinToString(", "))
                }

                // CPU 频率趋势图
                CpuFrequencyChart(
                    frequencies = deviceInfo.cpuInfo.frequencies,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 硬件信息
        InfoSection("硬件信息") {
            // 内存信息
            DeviceInfoItemWithProgress(
                label = "内存使用",
                value = "${deviceInfo.usedMemory} / ${deviceInfo.totalMemory}",
                progress = deviceInfo.memoryUsagePercent / 100f,
                progressText = "${deviceInfo.memoryUsagePercent.toInt()}%",
                progressColor = when {
                    deviceInfo.memoryUsagePercent >= 80 -> Color(0xFFF44336) // 红色
                    deviceInfo.memoryUsagePercent >= 50 -> Color(0xFFFFC107) // 黄色
                    else -> Color(0xFF4CAF50) // 绿色
                }
            )

            // 存储信息
            DeviceInfoItemWithProgress(
                label = "存储使用",
                value = "${deviceInfo.availableStorage} / ${deviceInfo.totalStorage}",
                progress = deviceInfo.storageUsagePercent / 100f,
                progressText = "${deviceInfo.storageUsagePercent.toInt()}%",
                progressColor = when {
                    deviceInfo.storageUsagePercent >= 80 -> Color(0xFFF44336) // 红色
                    deviceInfo.storageUsagePercent >= 50 -> Color(0xFFFFC107) // 黄色
                    else -> Color(0xFF4CAF50) // 绿色
                }
            )

            DeviceInfoItem(
                "电池状态",
                "${deviceInfo.batteryLevel}% ${if (deviceInfo.isCharging) "(充电中)" else ""}"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 网络信息
        InfoSection("网络信息") {
            DeviceInfoItem(
                "WiFi",
                if (deviceInfo.wifiEnabled) "已启用" else "已禁用",
                if (deviceInfo.wifiEnabled) Res.drawable.wifi else Res.drawable.wifiOff
            )
            DeviceInfoItem(
                "移动数据",
                if (deviceInfo.mobileDataEnabled) "已启用" else "已禁用",
                if (deviceInfo.mobileDataEnabled) Res.drawable.NetworkCell else Res.drawable.NetworkCellOff
            )
            DeviceInfoItem("IP 地址", deviceInfo.ipAddress)
        }
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DeviceInfoItem(
    label: String,
    value: String,
    iconRes: DrawableResource? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Composable
private fun DeviceInfoItemWithProgress(
    label: String,
    value: String,
    progress: Float,
    progressText: String,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp),
                color = progressColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
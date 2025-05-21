package ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }

    // 初始加载
    LaunchedEffect(Unit) {
        viewModel.refreshDeviceInfo()
    }

    // 当选择的设备改变时重新加载
    LaunchedEffect(selectedDevice) {
        if (selectedDevice != null) {
            viewModel.refreshDeviceInfo()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 设备选择器
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedDevice?.deviceId ?: "未连接",
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.devices),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.arrowDropDown),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    connectedDevices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = device.deviceId,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            onClick = {
                                viewModel.connect(device)
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 设备信息卡片
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InfoCard(
                            title = "CPU",
                            value = "${deviceInfo.cpuInfo.cores} cores",
                            subtitle = deviceInfo.cpuInfo.frequency,
                            icon = Res.drawable.cpu
                        )
                        InfoCard(
                            title = "内存",
                            value = "${deviceInfo.memoryUsagePercent}%",
                            subtitle = "${deviceInfo.usedMemory} / ${deviceInfo.totalMemory}",
                            icon = Res.drawable.memory
                        )
                        InfoCard(
                            title = "存储",
                            value = "${deviceInfo.storageUsagePercent}%",
                            subtitle = "${deviceInfo.availableStorage} / ${deviceInfo.totalStorage}",
                            icon = Res.drawable.storage
                        )
                    }
                }
            }

            // 详细信息部分
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // CPU 信息
                InfoSection(
                    title = "CPU 信息",
                    icon = Res.drawable.cpu
                ) {
                    DeviceInfoItem("架构", deviceInfo.cpuInfo.architecture)
                    DeviceInfoItem("核心数", "${deviceInfo.cpuInfo.cores} cores")
                    DeviceInfoItem("缓存", deviceInfo.cpuInfo.cache)

                    if (deviceInfo.cpuInfo.frequencies.isNotEmpty()) {
                        DeviceInfoItem("当前频率", deviceInfo.cpuInfo.frequency)
                        if (deviceInfo.cpuInfo.frequencies.size > 1) {
                            DeviceInfoItem("所有核心频率", deviceInfo.cpuInfo.frequencies.joinToString(", "))
                        }

                        CpuFrequencyChart(
                            frequencies = deviceInfo.cpuInfo.frequencies,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 硬件信息
                InfoSection(
                    title = "硬件信息",
                    icon = Res.drawable.hardware
                ) {
                    DeviceInfoItemWithProgress(
                        label = "内存使用",
                        value = "${deviceInfo.usedMemory} / ${deviceInfo.totalMemory}",
                        progress = deviceInfo.memoryUsagePercent / 100f,
                        progressText = "${deviceInfo.memoryUsagePercent.toInt()}%",
                        progressColor = MaterialTheme.colorScheme.primary
                    )

                    DeviceInfoItemWithProgress(
                        label = "存储使用",
                        value = "${deviceInfo.availableStorage} / ${deviceInfo.totalStorage}",
                        progress = deviceInfo.storageUsagePercent / 100f,
                        progressText = "${deviceInfo.storageUsagePercent.toInt()}%",
                        progressColor = MaterialTheme.colorScheme.primary
                    )

                    DeviceInfoItem(
                        "电池状态",
                        "${deviceInfo.batteryLevel}% ${if (deviceInfo.isCharging) "(充电中)" else ""}",
                        Res.drawable.battery
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 网络信息
                InfoSection(
                    title = "网络信息",
                    icon = Res.drawable.network
                ) {
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

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RowScope.InfoCard(
    title: String,
    value: String,
    subtitle: String,
    icon: DrawableResource
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    icon: DrawableResource,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
            .padding(vertical = 8.dp),
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
            .padding(vertical = 8.dp)
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
                progress = progress,
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
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
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
import org.jetbrains.compose.resources.stringResource
import ui.devices.components.CpuFrequencyChart
import viewmodel.DevicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val cpuInfo by viewModel.cpuInfo.collectAsState()
    val memoryInfo by viewModel.memoryInfo.collectAsState()
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val networkInfo by viewModel.networkInfo.collectAsState()
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
                    value = selectedDevice?.deviceId ?: stringResource(Res.string.no_device_connected),
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
                            value = "${cpuInfo.cores} cores",
                            subtitle = cpuInfo.frequency,
                            icon = Res.drawable.cpu
                        )
                        InfoCard(
                            title = "内存",
                            value = "${memoryInfo.usagePercent}%",
                            subtitle = "${memoryInfo.used} / ${memoryInfo.total}",
                            icon = Res.drawable.memory
                        )
                        InfoCard(
                            title = "存储",
                            value = "${storageInfo.usagePercent}%",
                            subtitle = "${storageInfo.used} / ${storageInfo.total}",
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
                    DeviceInfoItem("架构", cpuInfo.architecture)
                    DeviceInfoItem("核心数", "${cpuInfo.cores} cores")
                    DeviceInfoItem("缓存", cpuInfo.cache)

                    if (cpuInfo.frequencies.isNotEmpty()) {
                        DeviceInfoItem("当前频率", cpuInfo.frequency)
                        if (cpuInfo.frequencies.size > 1) {
                            DeviceInfoItem("所有核心频率", cpuInfo.frequencies.joinToString(", "))
                        }

                        CpuFrequencyChart(
                            frequencies = cpuInfo.frequencies,
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
                        value = memoryInfo.used,
                        progress = memoryInfo.usagePercent / 100f,
                        progressText = "${memoryInfo.usagePercent.toInt()}%",
                        progressColor = MaterialTheme.colorScheme.primary
                    )

                    if (memoryInfo.swapTotal.isNotEmpty() && memoryInfo.swapTotal != "0 B") {
                        DeviceInfoItemWithProgress(
                            label = "虚拟内存",
                            value = "${memoryInfo.swapUsed} / ${memoryInfo.swapTotal}",
                            progress = memoryInfo.swapUsagePercent / 100f,
                            progressText = "${memoryInfo.swapUsagePercent.toInt()}%",
                            progressColor = MaterialTheme.colorScheme.tertiary
                        )
                        DeviceInfoItem("交换缓存", memoryInfo.swapCached)
                    }

                    DeviceInfoItemWithProgress(
                        label = "存储使用",
                        value = "${storageInfo.used} / ${storageInfo.total}",
                        progress = storageInfo.usagePercent / 100f,
                        progressText = "${storageInfo.usagePercent.toInt()}%",
                        progressColor = MaterialTheme.colorScheme.primary
                    )

                    DeviceInfoItem(
                        "Root状态",
                        if (deviceInfo.isRooted) stringResource(Res.string.enabled) else stringResource(Res.string.disabled),
                        if (deviceInfo.isRooted) Res.drawable.shield else Res.drawable.shieldOff
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 电池信息
                InfoSection(
                    title = "电池信息",
                    icon = Res.drawable.battery
                ) {
                    DeviceInfoItemWithProgress(
                        label = "电池电量",
                        value = "${batteryInfo.level}%",
                        progress = batteryInfo.level / 100f,
                        progressText = "${batteryInfo.level}%",
                        progressColor = when {
                            batteryInfo.level <= 20 -> MaterialTheme.colorScheme.error
                            batteryInfo.level <= 50 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )

                    DeviceInfoItem(
                        "充电状态",
                        when {
                            batteryInfo.isAcPowered -> "交流电充电"
                            batteryInfo.isUsbPowered -> "USB充电"
                            batteryInfo.isWirelessPowered -> "无线充电"
                            else -> getBatteryStatusString(batteryInfo.status)
                        },
                        when {
                            batteryInfo.isAcPowered -> Res.drawable.power
                            batteryInfo.isUsbPowered -> Res.drawable.usb
                            batteryInfo.isWirelessPowered -> Res.drawable.wireless
                            else -> Res.drawable.battery
                        }
                    )

                    DeviceInfoItem("电池健康", getBatteryHealthString(batteryInfo.health))
                    DeviceInfoItem("电池技术", batteryInfo.technology)
                    DeviceInfoItem("电池电压", "${batteryInfo.voltage}mV")
                    DeviceInfoItem("电池温度", String.format("%.1f°C", batteryInfo.temperature))
                    
                    if (batteryInfo.maxChargingCurrent > 0) {
                        DeviceInfoItem("最大充电电流", "${batteryInfo.maxChargingCurrent}mA")
                    }
                    if (batteryInfo.maxChargingVoltage > 0) {
                        DeviceInfoItem("最大充电电压", "${batteryInfo.maxChargingVoltage}mV")
                    }
                    if (batteryInfo.chargeCounter > 0) {
                        DeviceInfoItem("充电计数器", batteryInfo.chargeCounter.toString())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 网络信息
                InfoSection(
                    title = "网络信息",
                    icon = Res.drawable.network
                ) {
                    DeviceInfoItem(
                        label ="WiFi",
                        if (networkInfo.wifiEnabled) stringResource(Res.string.enabled) else stringResource(Res.string.disabled),
                        if (networkInfo.wifiEnabled) Res.drawable.wifi else Res.drawable.wifiOff
                    )
                    DeviceInfoItem(
                        label = stringResource(Res.string.mobile_data),
                        if (networkInfo.mobileDataEnabled) stringResource(Res.string.enabled) else stringResource(Res.string.disabled),
                        if (networkInfo.mobileDataEnabled) Res.drawable.NetworkCell else Res.drawable.NetworkCellOff
                    )
                    DeviceInfoItem(stringResource(Res.string.ip_address), networkInfo.ipAddress)
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


private fun getBatteryHealthString(health: Int): String {
    return when (health) {
        1 -> "未知"
        2 -> "良好"
        3 -> "过热"
        4 -> "已损坏"
        5 -> "过压"
        6 -> "未知错误"
        7 -> "温度过低"
        else -> "未知"
    }
}

private fun getBatteryStatusString(status: Int): String {
    return when (status) {
        1 -> "未知"
        2 -> "充电中"
        3 -> "放电中"
        4 -> "未充电"
        5 -> "已充满"
        else -> "未知"
    }
}
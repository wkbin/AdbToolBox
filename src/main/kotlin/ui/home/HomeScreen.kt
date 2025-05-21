package ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import viewmodel.HomeViewModel
import viewmodel.QuickTool

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel { HomeViewModel() },
    onNavigateToDevice: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToBattery: () -> Unit
) {
    val recentTools by viewModel.recentTools.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 快速工具
        Text(
            text = "快速工具",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(quickTools) { tool ->
                QuickToolCard(
                    title = tool.title,
                    onClick = tool.onClick
                )
            }
        }

        // 最近使用
        if (recentTools.isNotEmpty()) {
            Text(
                text = "最近使用",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(recentTools) { tool ->
                    QuickToolCard(
                        title = tool.title,
                        onClick = tool.onClick
                    )
                }
            }
        }

        // 工具分类
        Text(
            text = "工具分类",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(toolCategories) { category ->
                CategoryCard(
                    title = category.title,
                    description = category.description,
                    onClick = category.onClick
                )
            }
        }
    }
}

@Composable
private fun QuickToolCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class ToolCategory(
    val title: String,
    val description: String,
    val onClick: () -> Unit
)

private val quickTools = listOf(
    QuickTool(
        title = "设备信息",
        onClick = { /* TODO */ }
    ),
    QuickTool(
        title = "网络工具",
        onClick = { /* TODO */ }
    ),
    QuickTool(
        title = "存储分析",
        onClick = { /* TODO */ }
    ),
    QuickTool(
        title = "电池信息",
        onClick = { /* TODO */ }
    )
)

private val toolCategories = listOf(
    ToolCategory(
        title = "系统工具",
        description = "设备信息、系统设置、性能监控",
        onClick = { /* TODO */ }
    ),
    ToolCategory(
        title = "网络工具",
        description = "网络诊断、连接管理、流量监控",
        onClick = { /* TODO */ }
    ),
    ToolCategory(
        title = "存储管理",
        description = "存储分析、文件管理、空间清理",
        onClick = { /* TODO */ }
    ),
    ToolCategory(
        title = "电池工具",
        description = "电池信息、充电管理、耗电分析",
        onClick = { /* TODO */ }
    )
)
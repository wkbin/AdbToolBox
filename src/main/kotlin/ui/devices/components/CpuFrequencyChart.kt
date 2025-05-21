package ui.devices.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

@Composable
fun CpuFrequencyChart(
    frequencies: List<String>,
    modifier: Modifier = Modifier
) {
    val frequencyHistory = remember { mutableStateListOf<List<Float>>() }
    val maxDataPoints = 30 // 显示最近30个数据点
    
    LaunchedEffect(frequencies) {
        val currentFrequencies = frequencies.mapNotNull { freq ->
            when {
                freq.endsWith("GHz") -> freq.removeSuffix("GHz").toFloatOrNull()?.times(1000)
                freq.endsWith("MHz") -> freq.removeSuffix("MHz").toFloatOrNull()
                else -> null
            }
        }
        
        if (currentFrequencies.isNotEmpty()) {
            // 确保所有核心都有数据，如果某个核心没有数据，使用0填充
            val paddedFrequencies = List(4) { index ->
                currentFrequencies.getOrNull(index) ?: 0f
            }
            frequencyHistory.add(paddedFrequencies)
            if (frequencyHistory.size > maxDataPoints) {
                frequencyHistory.removeAt(0)
            }
        }
        delay(2000) // 每2秒更新一次
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val coreColors = listOf(
                Pair("核心0", Color(0xFF2196F3)),
                Pair("核心1", Color(0xFF4CAF50)),
                Pair("核心2", Color(0xFFFFC107)),
                Pair("核心3", Color(0xFFF44336))
            )
            
            coreColors.forEach { (label, color) ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            // 频率刻度
            val maxFreq = 3600f // 固定最大频率为3.6GHz
            val freqStep = maxFreq / 4
            
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(50.dp)
            ) {
                for (i in 0..4) {
                    val freq = (maxFreq - freqStep * i).toInt()
                    val freqText = when {
                        freq >= 1000 -> String.format("%.1f GHz", freq / 1000.0)
                        else -> "${freq} MHz"
                    }
                    Text(
                        text = freqText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 50.dp) // 为刻度留出空间
            ) {
                val width = size.width
                val height = size.height
                
                // 绘制背景网格
                val gridColor = Color.Gray.copy(alpha = 0.1f)
                
                // 水平网格线
                for (i in 0..4) {
                    val y = height * i / 4
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }
                
                // 为每个核心绘制频率曲线
                if (frequencyHistory.isNotEmpty()) {
                    val pointWidth = width / (maxDataPoints - 1)
                    
                    // 始终绘制4个核心的曲线
                    for (coreIndex in 0..3) {
                        val path = Path()
                        val coreColor = when (coreIndex) {
                            0 -> Color(0xFF2196F3) // 蓝色
                            1 -> Color(0xFF4CAF50) // 绿色
                            2 -> Color(0xFFFFC107) // 黄色
                            3 -> Color(0xFFF44336) // 红色
                            else -> Color(0xFF9C27B0) // 紫色
                        }
                        
                        frequencyHistory.forEachIndexed { index, freqs ->
                            val freq = freqs.getOrNull(coreIndex) ?: 0f
                            val x = index * pointWidth
                            val y = height * (1 - freq / maxFreq)
                            
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        
                        drawPath(
                            path = path,
                            color = coreColor,
                            style = Stroke(
                                width = 2f,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }
        }
    }
} 
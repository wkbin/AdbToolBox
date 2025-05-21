package viewmodel

import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuickTool(
    val title: String,
    val onClick: () -> Unit
)

class HomeViewModel() : ViewModel(), KoinComponent {
    private val _recentTools = MutableStateFlow<List<QuickTool>>(emptyList())
    val recentTools: StateFlow<List<QuickTool>> = _recentTools.asStateFlow()

    fun addToRecentTools(tool: QuickTool) {
        val current = _recentTools.value.toMutableList()
        current.remove(tool) // 移除已存在的
        current.add(0, tool) // 添加到开头
        if (current.size > 5) current.removeLast() // 保持最多5个
        _recentTools.value = current
    }
}
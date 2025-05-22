package utils

object FormatUtils {
    fun formatMemory(bytes: Long): String {
        println("Debug - Formatting memory: $bytes bytes")
        return when {
            bytes >= 1024L * 1024 * 1024 * 1024 -> String.format("%.2f TB", bytes / (1024.0 * 1024 * 1024 * 1024))
            bytes >= 1024L * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024L * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            bytes >= 1024L -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /**
     * @param value 单位 kB
     */
    fun formatMemory(value: String?): String {
        return formatMemory((value?.toLongOrNull() ?: 0L) * 1024)
    }

    fun parseMemoryString(memoryStr: String): Long {
        println("Debug - Parsing memory string: '$memoryStr'")

        // 检查输入是否为空
        if (memoryStr.isBlank()) {
            println("Debug - Empty memory string")
            return 0L
        }

        // 分割字符串，处理格式如 "MemTotal: 1024 kB"
        val parts = memoryStr.trim().split(":")
        if (parts.size != 2) {
            println("Debug - Invalid memory string format (missing colon): '$memoryStr'")
            return 0L
        }

        // 获取值和单位部分
        val valueAndUnit = parts[1].trim().split("\\s+".toRegex())
        if (valueAndUnit.size != 2) {
            println("Debug - Invalid memory string format (missing space between value and unit): '${parts[1]}'")
            return 0L
        }

        val value = valueAndUnit[0].toLongOrNull()
        val unit = valueAndUnit[1].uppercase()

        if (value == null) {
            println("Debug - Invalid memory value: '${valueAndUnit[0]}'")
            return 0L
        }

        println("Debug - Parsed memory value: $value $unit")

        // 根据单位转换
        val bytes = when (unit) {
            "TB" -> value * 1024L * 1024 * 1024 * 1024
            "GB" -> value * 1024L * 1024 * 1024
            "MB" -> value * 1024L * 1024
            "KB" -> value * 1024L
            "B" -> value
            else -> {
                println("Debug - Unknown memory unit: '$unit'")
                0L
            }
        }

        println("Debug - Converted to bytes: $bytes")
        return bytes
    }

    fun formatDuration(milliseconds: Long): String {
        println("Debug - Formatting duration: $milliseconds ms")
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> String.format("%dd %02dh %02dm %02ds", days, hours % 24, minutes % 60, seconds % 60)
            hours > 0 -> String.format("%02dh %02dm %02ds", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%02dm %02ds", minutes, seconds % 60)
            else -> String.format("%02ds", seconds)
        }
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        println("Debug - Formatting speed: $bytesPerSecond bytes/s")
        return when {
            bytesPerSecond >= 1024L * 1024 * 1024 -> String.format("%.2f GB/s", bytesPerSecond / (1024.0 * 1024 * 1024))
            bytesPerSecond >= 1024L * 1024 -> String.format("%.2f MB/s", bytesPerSecond / (1024.0 * 1024))
            bytesPerSecond >= 1024L -> String.format("%.2f KB/s", bytesPerSecond / 1024.0)
            else -> "$bytesPerSecond B/s"
        }
    }

    fun formatPercentage(value: Float): String {
        println("Debug - Formatting percentage: $value")
        return String.format("%.1f%%", value)
    }

    fun formatTemperature(celsius: Float): String {
        println("Debug - Formatting temperature: $celsius°C")
        return String.format("%.1f°C", celsius)
    }

    fun formatVoltage(millivolts: Int): String {
        println("Debug - Formatting voltage: $millivolts mV")
        return String.format("%.2f V", millivolts / 1000.0)
    }

    fun formatCurrent(milliamps: Int): String {
        println("Debug - Formatting current: $milliamps mA")
        return String.format("%.2f A", milliamps / 1000.0)
    }

    fun formatPower(milliwatts: Int): String {
        println("Debug - Formatting power: $milliwatts mW")
        return String.format("%.2f W", milliwatts / 1000.0)
    }

    fun formatFrequency(hertz: Long): String {
        println("Debug - Formatting frequency: $hertz Hz")
        return when {
            hertz >= 1000000000L -> String.format("%.2f GHz", hertz / 1000000000.0)
            hertz >= 1000000L -> String.format("%.2f MHz", hertz / 1000000.0)
            hertz >= 1000L -> String.format("%.2f KHz", hertz / 1000.0)
            else -> "$hertz Hz"
        }
    }
} 
package model

data class BatteryInfo(
    val level: Int = 0,                    // 当前电量百分比
    val scale: Int = 100,                  // 最大电量
    val voltage: Int = 0,                  // 电压（毫伏）
    val temperature: Float = 0f,           // 温度（摄氏度）
    val technology: String = "",           // 电池技术
    val health: Int = 0,                   // 电池健康状态
    val status: Int = 0,                   // 充电状态
    val isAcPowered: Boolean = false,      // 是否使用交流电充电
    val isUsbPowered: Boolean = false,     // 是否使用USB充电
    val isWirelessPowered: Boolean = false,// 是否使用无线充电
    val chargeCounter: Int = 0,            // 充电计数器
    val maxChargingCurrent: Int = 0,       // 最大充电电流
    val maxChargingVoltage: Int = 0,       // 最大充电电压
    val isPresent: Boolean = true          // 电池是否存在
) 
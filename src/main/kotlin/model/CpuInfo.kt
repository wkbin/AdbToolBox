package model

data class CpuInfo(
    val architecture: String = "",
    val cores: Int = 0,
    val frequency: String = "",
    val cache: String = "",
    val frequencies: List<String> = emptyList()
)
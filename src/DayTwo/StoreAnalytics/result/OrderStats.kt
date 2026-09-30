package DayTwo.StoreAnalytics.result

data class OrderStats(
    val count: Int,
    val revenue: Long,
    val average: Double?,
    val min: Long?,
    val max: Long?,
    val totalUnits: Int
)
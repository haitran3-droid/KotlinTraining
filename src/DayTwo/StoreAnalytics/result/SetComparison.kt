package DayTwo.StoreAnalytics.result

data class SetComparison(
    val all: Set<String>,
    val common: Set<String>,
    val onlyFirst: Set<String>
)

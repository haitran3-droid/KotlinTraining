package DayTwo.StoreAnalytics.model

data class ProductViewed(
    val productId: String,
    val userId: String
): StoreEvent

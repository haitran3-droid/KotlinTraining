package DayTwo.StoreAnalytics.model

data class CheckoutFailed(
    val orderId : String
): StoreEvent

package DayTwo.StoreAnalytics.model

data class Order(
    val id: String,
    val customerId: String,
    val items: List<OrderItem>,
    val status: OrderStatus
)

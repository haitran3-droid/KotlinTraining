package DayTwo.StoreAnalytics.model

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Long,
    val stock: Int,
    val tags: Set<String>
)

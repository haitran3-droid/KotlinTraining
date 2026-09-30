package DayTwo.StoreAnalytics.result

import DayTwo.StoreAnalytics.model.Product

data class Dashboard(
    val availableProducts: List<Product>,
    val completedOrderCount: Int,
    val totalRevenue: Long,
    val topCustomers: List<CustomerSpend>,
    val revenueByCategory: Map<String, Long>,
    val stockAlerts: List<StockAlert>
)

package DayTwo.StoreAnalytics.exercise

import DayTwo.StoreAnalytics.model.Customer
import DayTwo.StoreAnalytics.model.Order
import DayTwo.StoreAnalytics.model.OrderStatus
import DayTwo.StoreAnalytics.model.Product
import DayTwo.StoreAnalytics.result.CustomerSpend
import DayTwo.StoreAnalytics.result.Dashboard
import DayTwo.StoreAnalytics.result.StockAlert

object DashboardExercise {
    // Câu 17: Pipeline tổng hợp cho màn hình quản trị.
    fun buildDashboard(
        products: List<Product>,
        customers: List<Customer>,
        orders: List<Order>
    ): Dashboard {
        val productById = products.associateBy { it.id }
        val customerById = customers.associateBy { it.id }
        val completed = orders.filter { it.status == OrderStatus.COMPLETED }
        val spending = OrderExercises.spendingByCustomer(completed, productById)
        val inventory = OrderExercises.inventoryAfterCompletedOrders(products, completed)

        val topCustomers = spending.entries
            .mapNotNull { (customerId, amount) ->
                customerById[customerId]?.let { CustomerSpend(it.name, amount) }
            }
            .sortedByDescending { it.amount }
            .take(3)

        val revenueByCategory = completed
            .flatMap { it.items }
            .mapNotNull { item ->
                productById[item.productId]?.let { product ->
                    product.category to product.price * item.quantity
                }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }

        val alerts = products.mapNotNull { product ->
            val remaining = inventory.getOrDefault(product.id, product.stock)
            if (remaining <= 5) StockAlert(product.id, product.name, remaining) else null
        }.sortedBy { it.remaining }

        return Dashboard(
            availableProducts = CatalogExercises.availableProducts(products),
            completedOrderCount = completed.size,
            totalRevenue = completed.sumOf { OrderExercises.orderTotal(it, productById) },
            topCustomers = topCustomers,
            revenueByCategory = revenueByCategory,
            stockAlerts = alerts
        )
    }
}

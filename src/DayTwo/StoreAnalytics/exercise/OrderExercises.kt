package DayTwo.StoreAnalytics.exercise

import DayTwo.StoreAnalytics.model.Order
import DayTwo.StoreAnalytics.model.OrderItem
import DayTwo.StoreAnalytics.model.OrderStatus
import DayTwo.StoreAnalytics.model.Product
import DayTwo.StoreAnalytics.model.ProductViewed
import DayTwo.StoreAnalytics.model.StoreEvent
import DayTwo.StoreAnalytics.result.ValidationResult

object OrderExercises {
    // Câu 06: flatMap, flatten, filterIsInstance, filterNotNull và filterNot.
    fun completedItems(orders: List<Order>): List<OrderItem> = orders
        .filter { it.status == OrderStatus.COMPLETED }
        .flatMap { it.items }

    fun completedItemsWithFlatten(orders: List<Order>): List<OrderItem> = orders
        .filter { it.status == OrderStatus.COMPLETED }
        .map { it.items }
        .flatten()

    fun viewedProductIds(events: List<StoreEvent>): List<String> = events
        .filterIsInstance<ProductViewed>()
        .map { it.productId }

    fun validSearchTerms(terms: List<String?>): List<String> = terms
        .filterNotNull()
        .map { it.trim() }
        .filterNot { it.isEmpty() }

    // Câu 07: sumOf, associate và lọc Map theo value.
    fun orderTotal(order: Order, productsById: Map<String, Product>): Long =
        order.items.sumOf { item ->
            productsById[item.productId]?.price?.times(item.quantity) ?: 0L
        }

    fun orderTotals(orders: List<Order>, productsById: Map<String, Product>): Map<String, Long> =
        orders.associate { it.id to orderTotal(it, productsById) }

    fun ordersFromAmount(totals: Map<String, Long>, minimum: Long): Map<String, Long> =
        totals.filterValues { it >= minimum }

    // Câu 08: groupBy và mapValues.
    fun spendingByCustomer(orders: List<Order>, productsById: Map<String, Product>): Map<String, Long> =
        orders.filter { it.status == OrderStatus.COMPLETED }
            .groupBy { it.customerId }
            .mapValues { (_, customerOrders) ->
                customerOrders.sumOf { orderTotal(it, productsById) }
            }

    // Câu 09: MutableMap, getOrDefault và chuyển về Map chỉ đọc.
    fun inventoryAfterCompletedOrders(
        products: List<Product>,
        orders: List<Order>
    ): Map<String, Int> {
        val inventory = products.associate { it.id to it.stock }.toMutableMap()
        completedItems(orders).forEach { item ->
            val current = inventory.getOrDefault(item.productId, 0)
            inventory[item.productId] = (current - item.quantity).coerceAtLeast(0)
        }
        return inventory.toMap()
    }

    // Câu 10: any, all và none; tránh vacuous truth khi danh sách rỗng.
    fun validateOrders(
        orders: List<Order>,
        productIds: Set<String>,
        customerIds: Set<String>
    ): ValidationResult = ValidationResult(
        hasEmptyOrder = orders.any { it.items.isEmpty() },
        allItemsValid = orders.isNotEmpty() && orders.all { order ->
            order.items.isNotEmpty() && order.items.all { it.quantity > 0 && it.productId in productIds }
        },
        hasNoUnknownCompletedCustomer = orders.none {
            it.status == OrderStatus.COMPLETED && it.customerId !in customerIds
        }
    )
}
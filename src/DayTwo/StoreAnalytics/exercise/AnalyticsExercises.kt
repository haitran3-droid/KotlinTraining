package DayTwo.StoreAnalytics.exercise

import DayTwo.StoreAnalytics.model.Customer
import DayTwo.StoreAnalytics.model.Order
import DayTwo.StoreAnalytics.model.OrderStatus
import DayTwo.StoreAnalytics.model.Product
import DayTwo.StoreAnalytics.result.OrderStats

object AnalyticsExercises {
    // Câu 11: Aggregate, fold và reduceOrNull.
    fun completedOrderStats(orders: List<Order>, productsById: Map<String, Product>): OrderStats {
        val completed = orders.filter { it.status == OrderStatus.COMPLETED }
        val totals = completed.map { OrderExercises.orderTotal(it, productsById) }
        val quantities = OrderExercises.completedItems(completed).map { it.quantity }
        return OrderStats(
            count = completed.count(),
            revenue = totals.fold(0L) { total, value -> total + value },
            average = totals.takeIf { it.isNotEmpty() }?.average(),
            min = totals.minOrNull(),
            max = totals.maxOrNull(),
            totalUnits = quantities.reduceOrNull { total, quantity -> total + quantity } ?: 0
        )
    }

    // Câu 14: chunked, windowed và zipWithNext.
    fun shippingBatches(orders: List<Order>, batchSize: Int): List<List<Order>> {
        require(batchSize > 0)
        return orders.filter { it.status == OrderStatus.COMPLETED }.chunked(batchSize)
    }

    fun movingRevenue(totals: List<Long>, windowSize: Int): List<Long> {
        require(windowSize > 0)
        return totals.windowed(windowSize) { it.sum() }
    }

    fun revenueChanges(totals: List<Long>): List<Long> =
        totals.zipWithNext { previous, current -> current - previous }

    // Câu 15: mapIndexed, joinToString, zip và unzip.
    fun completedOrderLabel(orders: List<Order>): String = orders
        .filter { it.status == OrderStatus.COMPLETED }
        .mapIndexed { index, order -> "${index + 1}. ${order.id}" }
        .joinToString(separator = " | ", prefix = "[", postfix = "]")

    fun customerSpendColumns(
        customers: List<Customer>,
        spending: Map<String, Long>
    ): Pair<List<String>, List<Long>> {
        val names = customers.map { it.name }
        val amounts = customers.map { spending.getOrDefault(it.id, 0L) }
        return names.zip(amounts).unzip()
    }

    // Câu 16: Lazy Sequence và generateSequence.
    fun mostExpensiveAvailable(products: List<Product>, count: Int): List<Product> {
        require(count >= 0)
        return products.asSequence()
            .filter { it.stock > 0 }
            .sortedByDescending { it.price }
            .take(count)
            .toList()
    }

    fun restockPlan(firstBatch: Int, numberOfBatches: Int): List<Int> {
        require(firstBatch > 0 && numberOfBatches >= 0)
        return generateSequence(firstBatch) { previous -> previous * 2 }
            .take(numberOfBatches)
            .toList()
    }
}
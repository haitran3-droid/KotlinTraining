package DayTwo.StoreAnalytics.exercise

import DayTwo.StoreAnalytics.model.Customer
import DayTwo.StoreAnalytics.model.OrderItem
import DayTwo.StoreAnalytics.model.Product
import DayTwo.StoreAnalytics.result.CatalogIndexes
import DayTwo.StoreAnalytics.result.PriceBands
import DayTwo.StoreAnalytics.result.SetComparison

object CatalogExercises {
    // Câu 01: MutableList nhưng không làm thay đổi đầu vào.
    fun editCart(
        initial: List<OrderItem>,
        itemToAdd: OrderItem,
        productIdToRemove: String
    ): List<OrderItem> {
        val cart = initial.toMutableList()
        cart.add(itemToAdd)
        val removeIndex = cart.indexOfFirst { it.productId == productIdToRemove }
        if (removeIndex >= 0) cart.removeAt(removeIndex)
        return cart.toList()
    }

    // Câu 02: Truy cập an toàn và ba cách associate.
    fun productAt(products: List<Product>, index: Int): Product? = products.getOrNull(index)

    fun buildCatalogIndexes(products: List<Product>): CatalogIndexes = CatalogIndexes(
        byId = products.associateBy { it.id },
        nameById = products.associate { it.id to it.name },
        tagCountByProduct = products.associateWith { it.tags.size }
    )

    // Câu 03: Chuyển đổi sang Set và các phép toán tập hợp.
    fun uniqueCategories(products: List<Product>): Set<String> = products.map { it.category }.toSet()

    fun compareTags(first: List<Product>, second: List<Product>): SetComparison {
        val firstTags = first.flatMap { it.tags }.toSet()
        val secondTags = second.flatMap { it.tags }.toSet()
        return SetComparison(
            all = firstTags union secondTags,
            common = firstTags intersect secondTags,
            onlyFirst = firstTags subtract secondTags
        )
    }

    // Câu 04: filter, filterIndexed, sortedWith và partition.
    fun availableProducts(products: List<Product>): List<Product> = products
        .filter { it.stock > 0 }
        .sortedWith(compareBy<Product> { it.price }.thenBy { it.name })

    fun featuredProducts(products: List<Product>): List<Product> =
        availableProducts(products).filterIndexed { index, _ -> index % 2 == 0 }

    fun partitionByStock(products: List<Product>): Pair<List<Product>, List<Product>> =
        products.partition { it.stock > 0 }

    // Câu 05: mapNotNull và distinctBy.
    fun uniqueCustomerEmails(customers: List<Customer>): List<String> = customers
        .mapNotNull { it.email?.trim()?.takeIf(String::isNotEmpty) }
        .distinctBy { it.lowercase() }

    // Câu 12: filter, drop và take.
    fun searchAndPaginate(
        products: List<Product>,
        query: String,
        page: Int,
        pageSize: Int
    ): List<Product> {
        require(page > 0 && pageSize > 0)
        val offset = (page.toLong() - 1L) * pageSize
        val matched = products
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.name }
        if (offset >= matched.size) return emptyList()
        return matched.drop(offset.toInt()).take(pageSize)
    }

    // Câu 13: takeWhile và dropWhile trên cùng một danh sách đã sắp xếp.
    fun splitByBudget(products: List<Product>, maximumPrice: Long): PriceBands {
        val sorted = products.filter { it.stock > 0 }.sortedBy { it.price }
        return PriceBands(
            affordable = sorted.takeWhile { it.price <= maximumPrice },
            expensive = sorted.dropWhile { it.price <= maximumPrice }
        )
    }
}
package DayTwo.StoreAnalytics

import DayTwo.StoreAnalytics.data.StoreSampleData
import DayTwo.StoreAnalytics.exercise.AnalyticsExercises
import DayTwo.StoreAnalytics.exercise.CatalogExercises
import DayTwo.StoreAnalytics.exercise.DashboardExercise
import DayTwo.StoreAnalytics.exercise.OrderExercises
import DayTwo.StoreAnalytics.model.CheckoutFailed
import DayTwo.StoreAnalytics.model.Customer
import DayTwo.StoreAnalytics.model.Order
import DayTwo.StoreAnalytics.model.OrderItem
import DayTwo.StoreAnalytics.model.OrderStatus
import DayTwo.StoreAnalytics.model.ProductViewed
import DayTwo.StoreAnalytics.model.SearchSubmitted
import DayTwo.StoreAnalytics.result.CustomerSpend
import DayTwo.StoreAnalytics.result.OrderStats
import DayTwo.StoreAnalytics.result.SetComparison
import DayTwo.StoreAnalytics.result.StockAlert
import DayTwo.StoreAnalytics.result.ValidationResult

// Chạy main trong IntelliJ. check(...) sẽ báo tên ví dụ và kết quả nếu có lỗi.
fun main() {
    val products = StoreSampleData.products
    val customers = StoreSampleData.customers
    val orders = StoreSampleData.orders
    val originalProducts = products.toList()
    val originalCustomers = customers.toList()
    val originalOrders = orders.map { it.copy(items = it.items.toList()) }
    var passed = 0

    fun <T> expect(label: String, actual: T, expected: T) {
        check(actual == expected) { "$label: mong đợi <$expected>, thực tế <$actual>" }
        passed++
        println("OK: $label")
    }

    fun expectInvalid(label: String, action: () -> Unit) {
        val error = runCatching(action).exceptionOrNull()
        check(error is IllegalArgumentException) { "$label: cần IllegalArgumentException, thực tế <$error>" }
        passed++
        println("OK: $label")
    }

    val cart = listOf(OrderItem("p2", 1), OrderItem("p2", 2))
    expect("Câu 01 - chỉ xóa item đầu tiên", CatalogExercises.editCart(cart, OrderItem("p4", 3), "p2"),
        listOf(OrderItem("p2", 2), OrderItem("p4", 3)))
    expect("Câu 01 - giữ nguyên giỏ ban đầu", cart, listOf(OrderItem("p2", 1), OrderItem("p2", 2)))
    expect("Câu 01 - mã cần xóa không tồn tại", CatalogExercises.editCart(emptyList(), OrderItem("p1", 1), "missing"),
        listOf(OrderItem("p1", 1)))

    val indexes = CatalogExercises.buildCatalogIndexes(products)
    val byId = indexes.byId
    expect("Câu 02 - truy cập hợp lệ", CatalogExercises.productAt(products, 0), products.first())
    expect("Câu 02 - index âm", CatalogExercises.productAt(products, -1), null)
    expect("Câu 02 - index ngoài phạm vi", CatalogExercises.productAt(products, products.size), null)
    expect("Câu 02 - bảng sản phẩm", byId["p1"], products.first())
    expect("Câu 02 - bảng tên", indexes.nameById["p6"], "Webcam")
    expect("Câu 02 - số tag", indexes.tagCountByProduct[products.first()], 2)
    val replacement = products.first().copy(name = "Laptop mới")
    expect("Câu 02 - id trùng giữ bản ghi cuối", CatalogExercises.buildCatalogIndexes(products + replacement).byId["p1"], replacement)

    expect("Câu 03 - category duy nhất", CatalogExercises.uniqueCategories(products), setOf("Điện tử", "Đồ uống"))
    expect("Câu 03 - phép toán tag", CatalogExercises.compareTags(products.take(2), listOf(products[5])),
        SetComparison(setOf("work", "premium", "accessory", "video"), setOf("work"), setOf("premium", "accessory")))
    expect("Câu 04 - còn hàng và tăng dần theo giá", CatalogExercises.availableProducts(products).map { it.id },
        listOf("p5", "p4", "p2", "p6", "p1"))
    expect("Câu 04 - vị trí chẵn", CatalogExercises.featuredProducts(products).map { it.id }, listOf("p5", "p2", "p1"))
    val partition = CatalogExercises.partitionByStock(products)
    expect("Câu 04 - nhóm còn hàng", partition.first.map { it.id }, listOf("p1", "p2", "p4", "p5", "p6"))
    expect("Câu 04 - nhóm hết hàng", partition.second.map { it.id }, listOf("p3"))
    expect("Câu 04 - cùng giá sắp theo tên", CatalogExercises.availableProducts(listOf(
        products.first().copy(name = "B", price = 1), products[1].copy(name = "A", price = 1)
    )).map { it.name }, listOf("A", "B"))
    expect("Câu 05 - trim và loại email trùng", CatalogExercises.uniqueCustomerEmails(customers + Customer("c5", "E", "  ")),
        listOf("An@example.com", "dung@example.com"))

    val completedItems = listOf(OrderItem("p1", 1), OrderItem("p2", 2), OrderItem("p6", 1),
        OrderItem("p2", 1), OrderItem("p4", 2), OrderItem("p5", 1))
    expect("Câu 06 - flatMap", OrderExercises.completedItems(orders), completedItems)
    expect("Câu 06 - flatten", OrderExercises.completedItemsWithFlatten(orders), completedItems)
    expect("Câu 06 - lọc kiểu sự kiện", OrderExercises.viewedProductIds(listOf(
        ProductViewed("p1", "c1"), SearchSubmitted("Laptop"), CheckoutFailed("o2"), ProductViewed("p6", "c3")
    )), listOf("p1", "p6"))
    expect("Câu 06 - làm sạch từ khóa", OrderExercises.validSearchTerms(listOf(null, "", "  ", " laptop ", "trà")),
        listOf("laptop", "trà"))

    val totals = OrderExercises.orderTotals(orders, byId)
    expect("Câu 07 - tổng tiền mọi đơn", totals,
        mapOf("o1" to 21_000_000L, "o2" to 520_000L, "o3" to 2_000_000L, "o4" to 20_000_000L, "o5" to 320_000L))
    expect("Câu 07 - lọc gồm cả ngưỡng", OrderExercises.ordersFromAmount(totals, 2_000_000L),
        mapOf("o1" to 21_000_000L, "o3" to 2_000_000L, "o4" to 20_000_000L))
    val unknownProductOrder = orders.first().copy(items = listOf(OrderItem("missing", 2), OrderItem("p5", 1)))
    expect("Câu 07 - sản phẩm không tồn tại có giá 0", OrderExercises.orderTotal(unknownProductOrder, byId), 80_000L)
    expect("Câu 07 - tổng tiền dùng Long", OrderExercises.orderTotal(orders.first().copy(items = listOf(OrderItem("p1", 200))), byId),
        4_000_000_000L)
    val spending = OrderExercises.spendingByCustomer(orders, byId)
    expect("Câu 08 - chi tiêu từ đơn hoàn tất", spending, mapOf("c1" to 23_000_000L, "c3" to 320_000L))
    expect("Câu 09 - tồn kho sau bán", OrderExercises.inventoryAfterCompletedOrders(products, orders),
        mapOf("p1" to 4, "p2" to 17, "p3" to 0, "p4" to 28, "p5" to 39, "p6" to 7))
    expect("Câu 09 - tồn kho không âm", OrderExercises.inventoryAfterCompletedOrders(products,
        listOf(orders.first().copy(items = listOf(OrderItem("p1", 100)))))["p1"], 0)
    val customerIds = customers.map { it.id }.toSet()
    expect("Câu 10 - dữ liệu mẫu hợp lệ", OrderExercises.validateOrders(orders, byId.keys, customerIds), ValidationResult(false, true, true))
    expect("Câu 10 - đơn rỗng", OrderExercises.validateOrders(listOf(orders.first().copy(items = emptyList())), byId.keys, customerIds),
        ValidationResult(true, false, true))
    expect("Câu 10 - sản phẩm lạ", OrderExercises.validateOrders(listOf(unknownProductOrder), byId.keys, customerIds).allItemsValid, false)
    expect("Câu 10 - số lượng không dương", OrderExercises.validateOrders(
        listOf(orders.first().copy(items = listOf(OrderItem("p1", 0), OrderItem("p2", -1)))), byId.keys, customerIds).allItemsValid, false)
    val unknownCustomerOrder = orders.first().copy(customerId = "missing")
    expect("Câu 10 - khách lạ trong đơn hoàn tất", OrderExercises.validateOrders(listOf(unknownCustomerOrder), byId.keys, customerIds),
        ValidationResult(false, true, false))
    expect("Câu 10 - khách lạ trong đơn chờ", OrderExercises.validateOrders(
        listOf(unknownCustomerOrder.copy(status = OrderStatus.PENDING)), byId.keys, customerIds), ValidationResult(false, true, true))

    val stats = AnalyticsExercises.completedOrderStats(orders, byId)
    expect("Câu 11 - thống kê", stats, OrderStats(3, 23_320_000L, 23_320_000.0 / 3, 320_000L, 21_000_000L, 8))
    expect("Câu 12 - tìm không phân biệt hoa thường", CatalogExercises.searchAndPaginate(products, "LAP", 1, 2).map { it.id }, listOf("p1"))
    expect("Câu 12 - trang thứ hai", CatalogExercises.searchAndPaginate(products, "", 2, 2).map { it.id }, listOf("p4", "p1"))
    expect("Câu 12 - trang ngoài phạm vi", CatalogExercises.searchAndPaginate(products, "", Int.MAX_VALUE, Int.MAX_VALUE), emptyList())
    val bands = CatalogExercises.splitByBudget(products, 500_000L)
    expect("Câu 13 - giá bằng ngân sách", bands.affordable.map { it.id }, listOf("p5", "p4", "p2"))
    expect("Câu 13 - vượt ngân sách", bands.expensive.map { it.id }, listOf("p6", "p1"))
    expect("Câu 14 - lô giao hàng", AnalyticsExercises.shippingBatches(orders, 2).map { batch -> batch.map { it.id } },
        listOf(listOf("o1", "o3"), listOf("o5")))
    val completedTotals = listOf(21_000_000L, 2_000_000L, 320_000L)
    expect("Câu 14 - cửa sổ doanh thu", AnalyticsExercises.movingRevenue(completedTotals, 2), listOf(23_000_000L, 2_320_000L))
    expect("Câu 14 - cửa sổ quá lớn", AnalyticsExercises.movingRevenue(completedTotals, 4), emptyList())
    expect("Câu 14 - thay đổi doanh thu", AnalyticsExercises.revenueChanges(completedTotals), listOf(-19_000_000L, -1_680_000L))
    expect("Câu 15 - đánh số đơn", AnalyticsExercises.completedOrderLabel(orders), "[1. o1 | 2. o3 | 3. o5]")
    expect("Câu 15 - zip và unzip", AnalyticsExercises.customerSpendColumns(customers, spending),
        listOf("An", "Bình", "Cường", "Dũng") to listOf(23_000_000L, 0L, 320_000L, 0L))
    expect("Câu 16 - hai sản phẩm đắt nhất còn hàng", AnalyticsExercises.mostExpensiveAvailable(products, 2).map { it.id }, listOf("p1", "p6"))
    expect("Câu 16 - kế hoạch nhập hàng", AnalyticsExercises.restockPlan(5, 4), listOf(5, 10, 20, 40))
    expect("Câu 16 - lấy 0 sản phẩm", AnalyticsExercises.mostExpensiveAvailable(products, 0), emptyList())
    expect("Câu 16 - 0 đợt nhập", AnalyticsExercises.restockPlan(5, 0), emptyList())

    val dashboard = DashboardExercise.buildDashboard(products, customers, orders)
    expect("Câu 17 - sản phẩm dashboard", dashboard.availableProducts.map { it.id }, listOf("p5", "p4", "p2", "p6", "p1"))
    expect("Câu 17 - số đơn hoàn tất", dashboard.completedOrderCount, 3)
    expect("Câu 17 - doanh thu", dashboard.totalRevenue, 23_320_000L)
    expect("Câu 17 - khách chi tiêu cao nhất", dashboard.topCustomers, listOf(CustomerSpend("An", 23_000_000L), CustomerSpend("Cường", 320_000L)))
    expect("Câu 17 - doanh thu theo category", dashboard.revenueByCategory, mapOf("Điện tử" to 23_000_000L, "Đồ uống" to 320_000L))
    expect("Câu 17 - cảnh báo tồn kho", dashboard.stockAlerts, listOf(StockAlert("p3", "Bàn phím", 0), StockAlert("p1", "Laptop", 4)))

    // Các trường hợp biên để có thể chạy lại khi tự làm các bài tập.
    expect("Rỗng - catalog", CatalogExercises.buildCatalogIndexes(emptyList()).byId, emptyMap())
    expect("Rỗng - category", CatalogExercises.uniqueCategories(emptyList()), emptySet())
    expect("Rỗng - tag", CatalogExercises.compareTags(emptyList(), emptyList()), SetComparison(emptySet(), emptySet(), emptySet()))
    expect("Rỗng - email", CatalogExercises.uniqueCustomerEmails(emptyList()), emptyList())
    expect("Rỗng - item hoàn tất", OrderExercises.completedItems(emptyList()), emptyList())
    expect("Rỗng - tổng tiền", OrderExercises.orderTotals(emptyList(), byId), emptyMap())
    expect("Rỗng - chi tiêu", OrderExercises.spendingByCustomer(emptyList(), byId), emptyMap())
    expect("Rỗng - không có đơn vẫn giữ tồn kho", OrderExercises.inventoryAfterCompletedOrders(products, emptyList()), products.associate { it.id to it.stock })
    expect("Rỗng - validation", OrderExercises.validateOrders(emptyList(), emptySet(), emptySet()), ValidationResult(false, false, true))
    expect("Rỗng - thống kê", AnalyticsExercises.completedOrderStats(emptyList(), emptyMap()), OrderStats(0, 0L, null, null, null, 0))
    expect("Rỗng - tìm kiếm", CatalogExercises.searchAndPaginate(emptyList(), "", 1, 2), emptyList())
    expect("Rỗng - giao hàng", AnalyticsExercises.shippingBatches(emptyList(), 2), emptyList())
    expect("Rỗng - cửa sổ", AnalyticsExercises.movingRevenue(emptyList(), 2), emptyList())
    expect("Rỗng - thay đổi doanh thu", AnalyticsExercises.revenueChanges(emptyList()), emptyList())
    expect("Rỗng - nhãn đơn", AnalyticsExercises.completedOrderLabel(emptyList()), "[]")
    expect("Rỗng - cột chi tiêu", AnalyticsExercises.customerSpendColumns(emptyList(), emptyMap()), emptyList<String>() to emptyList<Long>())
    expect("Rỗng - sản phẩm đắt nhất", AnalyticsExercises.mostExpensiveAvailable(emptyList(), 2), emptyList())
    val emptyDashboard = DashboardExercise.buildDashboard(emptyList(), emptyList(), emptyList())
    expect("Rỗng - dashboard", emptyDashboard, DayTwo.StoreAnalytics.result.Dashboard(emptyList(), 0, 0L, emptyList(), emptyMap(), emptyList()))
    expectInvalid("Tham số - page bằng 0") { CatalogExercises.searchAndPaginate(products, "", 0, 2) }
    expectInvalid("Tham số - pageSize bằng 0") { CatalogExercises.searchAndPaginate(products, "", 1, 0) }
    expectInvalid("Tham số - batchSize bằng 0") { AnalyticsExercises.shippingBatches(orders, 0) }
    expectInvalid("Tham số - windowSize bằng 0") { AnalyticsExercises.movingRevenue(completedTotals, 0) }
    expectInvalid("Tham số - count âm") { AnalyticsExercises.mostExpensiveAvailable(products, -1) }
    expectInvalid("Tham số - firstBatch bằng 0") { AnalyticsExercises.restockPlan(0, 2) }
    expectInvalid("Tham số - số đợt âm") { AnalyticsExercises.restockPlan(5, -1) }
    expect("Giữ nguyên sản phẩm đầu vào", products, originalProducts)
    expect("Giữ nguyên khách hàng đầu vào", customers, originalCustomers)
    expect("Giữ nguyên đơn hàng đầu vào", orders, originalOrders)

    println("\nStore Analytics: đã đạt $passed kiểm tra.")
    println("Thống kê: $stats")
    println("Dashboard: $dashboard")
}

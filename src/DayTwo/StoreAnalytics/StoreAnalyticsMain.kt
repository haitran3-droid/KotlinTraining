package DayTwo.StoreAnalytics

import DayTwo.StoreAnalytics.data.StoreSampleData
import DayTwo.StoreAnalytics.exercise.AnalyticsExercises
import DayTwo.StoreAnalytics.exercise.CatalogExercises
import DayTwo.StoreAnalytics.exercise.DashboardExercise
import DayTwo.StoreAnalytics.exercise.OrderExercises
import DayTwo.StoreAnalytics.model.CheckoutFailed
import DayTwo.StoreAnalytics.model.Customer
import DayTwo.StoreAnalytics.model.OrderItem
import DayTwo.StoreAnalytics.model.OrderStatus
import DayTwo.StoreAnalytics.model.ProductViewed
import DayTwo.StoreAnalytics.model.SearchSubmitted
import DayTwo.StoreAnalytics.result.CustomerSpend
import DayTwo.StoreAnalytics.result.OrderStats
import DayTwo.StoreAnalytics.result.SetComparison
import DayTwo.StoreAnalytics.result.StockAlert
import DayTwo.StoreAnalytics.result.ValidationResult

// =====================================================================
//  Dữ liệu dùng chung
// =====================================================================
private val products = StoreSampleData.products
private val customers = StoreSampleData.customers
private val orders = StoreSampleData.orders
private val indexes = CatalogExercises.buildCatalogIndexes(products)
private val byId = indexes.byId

// =====================================================================
//  Helpers
// =====================================================================
private var passed = 0
private var failed = 0

private fun resetCounters() { passed = 0; failed = 0 }

private fun Long.toCurrency(): String =
    String.format("%,d", this).replace(',', '.') + "d"

private fun check(testName: String, actual: Any?, expected: Any?) {
    if (actual == expected) {
        passed++
        println("   [PASS] $testName")
        println("          -> Ket qua: $actual")
    } else {
        failed++
        println("   [FAIL] $testName")
        println("          -> Mong doi : $expected")
        println("          -> Thuc te  : $actual")
    }
}

private fun checkThrows(testName: String, action: () -> Unit) {
    val error = runCatching(action).exceptionOrNull()
    if (error is IllegalArgumentException) {
        passed++
        println("   [PASS] $testName -> IllegalArgumentException")
    } else {
        failed++
        println("   [FAIL] $testName -> Mong doi IllegalArgumentException, thuc te: $error")
    }
}

private fun printSummary() {
    println()
    if (failed == 0) println("   >> Tat ca $passed kiem tra deu DAT.")
    else println("   >> DAT: $passed  |  LOI: $failed")
    println()
}

private fun header(title: String) {
    println()
    println("=".repeat(60))
    println("  $title")
    println("=".repeat(60))
}

// =====================================================================
//  Câu 01 — Giỏ hàng mutable
// =====================================================================
private fun runCau01() {
    header("Cau 01 -- Gio hang mutable")
    resetCounters()

    val cart = listOf(OrderItem("p2", 1), OrderItem("p2", 2))
    println("   Input:")
    println("     Gio ban dau    : $cart")
    println("     Them vao       : OrderItem(p4, 3)")
    println("     Xoa productId  : p2")
    println()

    val result = CatalogExercises.editCart(cart, OrderItem("p4", 3), "p2")
    check("Chi xoa item dau tien co productId=p2, them p4",
        result, listOf(OrderItem("p2", 2), OrderItem("p4", 3)))
    check("Gio ban dau khong bi thay doi (immutable)",
        cart, listOf(OrderItem("p2", 1), OrderItem("p2", 2)))
    check("Xoa ma khong ton tai -> chi them",
        CatalogExercises.editCart(emptyList(), OrderItem("p1", 1), "missing"),
        listOf(OrderItem("p1", 1)))

    printSummary()
}

// =====================================================================
//  Câu 02 — Truy cập an toàn và lập chỉ mục
// =====================================================================
private fun runCau02() {
    header("Cau 02 -- Truy cap an toan va lap chi muc")
    resetCounters()

    println("   Input: ${products.size} san pham")
    println()

    check("productAt(products, 0) -> san pham dau tien",
        CatalogExercises.productAt(products, 0), products.first())
    check("productAt(products, -1) -> null (index am)",
        CatalogExercises.productAt(products, -1), null)
    check("productAt(products, ${products.size}) -> null (ngoai pham vi)",
        CatalogExercises.productAt(products, products.size), null)
    check("byId[\"p1\"] -> Laptop",
        byId["p1"], products.first())
    check("nameById[\"p6\"] -> \"Webcam\"",
        indexes.nameById["p6"], "Webcam")
    check("tagCountByProduct[Laptop] -> 2 (work, premium)",
        indexes.tagCountByProduct[products.first()], 2)

    val replacement = products.first().copy(name = "Laptop moi")
    check("ID trung -> associateBy giu ban ghi cuoi",
        CatalogExercises.buildCatalogIndexes(products + replacement).byId["p1"], replacement)

    printSummary()
}

// =====================================================================
//  Câu 03 — Set và phép toán tập hợp
// =====================================================================
private fun runCau03() {
    header("Cau 03 -- Set va phep toan tap hop")
    resetCounters()

    check("Tap category duy nhat",
        CatalogExercises.uniqueCategories(products), setOf("Điện tử", "Đồ uống"))

    println()
    println("   Input phep toan tag:")
    println("     Nhom 1: ${products.take(2).map { "${it.name} tags=${it.tags}" }}")
    println("     Nhom 2: ${listOf(products[5]).map { "${it.name} tags=${it.tags}" }}")
    println()

    check("union / intersect / subtract",
        CatalogExercises.compareTags(products.take(2), listOf(products[5])),
        SetComparison(setOf("work", "premium", "accessory", "video"), setOf("work"), setOf("premium", "accessory")))

    printSummary()
}

// =====================================================================
//  Câu 04 — Lọc và sắp xếp catalog
// =====================================================================
private fun runCau04() {
    header("Cau 04 -- Loc va sap xep catalog")
    resetCounters()

    val available = CatalogExercises.availableProducts(products)
    println("   San pham con hang, sap theo gia tang dan:")
    available.forEach { println("     - [${it.id}] ${it.name} -- ${it.price.toCurrency()}") }
    println()

    check("Danh sach ID con hang sap theo gia",
        available.map { it.id }, listOf("p5", "p4", "p2", "p6", "p1"))
    check("Vi tri chan (index 0, 2, 4) -> featured",
        CatalogExercises.featuredProducts(products).map { it.id }, listOf("p5", "p2", "p1"))

    val partition = CatalogExercises.partitionByStock(products)
    check("Nhom con hang", partition.first.map { it.id }, listOf("p1", "p2", "p4", "p5", "p6"))
    check("Nhom het hang", partition.second.map { it.id }, listOf("p3"))
    check("Cung gia -> sap theo ten (A truoc B)",
        CatalogExercises.availableProducts(
            listOf(products.first().copy(name = "B", price = 1), products[1].copy(name = "A", price = 1))
        ).map { it.name }, listOf("A", "B"))

    printSummary()
}

// =====================================================================
//  Câu 05 — Làm sạch dữ liệu nullable và trùng lặp
// =====================================================================
private fun runCau05() {
    header("Cau 05 -- Lam sach du lieu nullable va trung lap")
    resetCounters()

    val customersWithBlank = customers + Customer("c5", "E", "  ")
    println("   Input emails: ${customersWithBlank.map { "${it.name}=${it.email}" }}")
    println()

    check("Trim, bo null/rong, loai trung case-insensitive",
        CatalogExercises.uniqueCustomerEmails(customersWithBlank),
        listOf("An@example.com", "dung@example.com"))

    printSummary()
}

// =====================================================================
//  Câu 06 — Làm phẳng và lọc theo kiểu
// =====================================================================
private fun runCau06() {
    header("Cau 06 -- Lam phang va loc theo kieu")
    resetCounters()

    val completedItems = OrderExercises.completedItems(orders)
    println("   Don hoan tat: ${orders.filter { it.status == OrderStatus.COMPLETED }.map { it.id }}")
    println("   Items sau flatMap:")
    completedItems.forEach { println("     - productId=${it.productId}, qty=${it.quantity}") }
    println()

    val expected = listOf(
        OrderItem("p1", 1), OrderItem("p2", 2), OrderItem("p6", 1),
        OrderItem("p2", 1), OrderItem("p4", 2), OrderItem("p5", 1))
    check("flatMap items don hoan tat", completedItems, expected)
    check("flatten cho ket qua tuong duong", OrderExercises.completedItemsWithFlatten(orders), expected)

    val events = listOf(
        ProductViewed("p1", "c1"), SearchSubmitted("Laptop"),
        CheckoutFailed("o2"), ProductViewed("p6", "c3"))
    println()
    println("   Events: ${events.map { it::class.simpleName }}")
    println()
    check("filterIsInstance<ProductViewed> -> lay productId",
        OrderExercises.viewedProductIds(events), listOf("p1", "p6"))
    check("validSearchTerms: bo null, rong, trim",
        OrderExercises.validSearchTerms(listOf(null, "", "  ", " laptop ", "trà")),
        listOf("laptop", "trà"))

    printSummary()
}

// =====================================================================
//  Câu 07 — Tổng tiền và Map
// =====================================================================
private fun runCau07() {
    header("Cau 07 -- Tong tien va Map")
    resetCounters()

    val totals = OrderExercises.orderTotals(orders, byId)
    println("   Tong tien tung don:")
    totals.forEach { (id, total) ->
        val order = orders.first { it.id == id }
        println("     - [$id] ${order.items} -> ${total.toCurrency()}")
    }
    println()

    check("orderTotals cho moi don", totals,
        mapOf("o1" to 21_000_000L, "o2" to 520_000L, "o3" to 2_000_000L, "o4" to 20_000_000L, "o5" to 320_000L))
    check("Loc don >= 2.000.000d",
        OrderExercises.ordersFromAmount(totals, 2_000_000L),
        mapOf("o1" to 21_000_000L, "o3" to 2_000_000L, "o4" to 20_000_000L))

    val unknownProductOrder = orders.first().copy(items = listOf(OrderItem("missing", 2), OrderItem("p5", 1)))
    check("San pham khong ton tai -> gia = 0",
        OrderExercises.orderTotal(unknownProductOrder, byId), 80_000L)
    check("Tong tien dung Long (200 x 20tr = 4 ty)",
        OrderExercises.orderTotal(orders.first().copy(items = listOf(OrderItem("p1", 200))), byId),
        4_000_000_000L)

    printSummary()
}

// =====================================================================
//  Câu 08 — Nhóm dữ liệu
// =====================================================================
private fun runCau08() {
    header("Cau 08 -- Nhom du lieu -- chi tieu theo khach hang")
    resetCounters()

    val spending = OrderExercises.spendingByCustomer(orders, byId)
    println("   Chi tieu tu don hoan tat:")
    spending.forEach { (customerId, amount) ->
        val name = customers.firstOrNull { it.id == customerId }?.name ?: customerId
        println("     - $name ($customerId): ${amount.toCurrency()}")
    }
    println()

    check("spendingByCustomer", spending, mapOf("c1" to 23_000_000L, "c3" to 320_000L))

    printSummary()
}

// =====================================================================
//  Câu 09 — Cập nhật tồn kho
// =====================================================================
private fun runCau09() {
    header("Cau 09 -- Cap nhat ton kho")
    resetCounters()

    val inventory = OrderExercises.inventoryAfterCompletedOrders(products, orders)
    println("   Ton kho (ban dau -> sau ban):")
    products.forEach {
        val after = inventory[it.id] ?: 0
        val arrow = if (after < it.stock) "giam" else "giu nguyen"
        println("     - [${it.id}] ${it.name}: ${it.stock} -> $after ($arrow)")
    }
    println()

    check("inventoryAfterCompletedOrders", inventory,
        mapOf("p1" to 4, "p2" to 17, "p3" to 0, "p4" to 28, "p5" to 39, "p6" to 7))
    check("Ton kho khong am (mua 100 nhung chi co 5)",
        OrderExercises.inventoryAfterCompletedOrders(
            products, listOf(orders.first().copy(items = listOf(OrderItem("p1", 100))))
        )["p1"], 0)

    printSummary()
}

// =====================================================================
//  Câu 10 — Kiểm tra dữ liệu
// =====================================================================
private fun runCau10() {
    header("Cau 10 -- Kiem tra du lieu")
    resetCounters()

    val customerIds = customers.map { it.id }.toSet()
    val unknownProductOrder = orders.first().copy(items = listOf(OrderItem("missing", 2), OrderItem("p5", 1)))
    val unknownCustomerOrder = orders.first().copy(customerId = "missing")

    check("Du lieu mau: hasEmpty=false, allValid=true, noUnknown=true",
        OrderExercises.validateOrders(orders, byId.keys, customerIds),
        ValidationResult(false, true, true))
    check("Don rong -> hasEmptyOrder = true",
        OrderExercises.validateOrders(listOf(orders.first().copy(items = emptyList())), byId.keys, customerIds),
        ValidationResult(true, false, true))
    check("San pham la -> allItemsValid = false",
        OrderExercises.validateOrders(listOf(unknownProductOrder), byId.keys, customerIds).allItemsValid, false)
    check("So luong <= 0 -> allItemsValid = false",
        OrderExercises.validateOrders(
            listOf(orders.first().copy(items = listOf(OrderItem("p1", 0), OrderItem("p2", -1)))),
            byId.keys, customerIds
        ).allItemsValid, false)
    check("Khach la trong don COMPLETED -> noUnknown = false",
        OrderExercises.validateOrders(listOf(unknownCustomerOrder), byId.keys, customerIds),
        ValidationResult(false, true, false))
    check("Khach la trong don PENDING -> van OK",
        OrderExercises.validateOrders(
            listOf(unknownCustomerOrder.copy(status = OrderStatus.PENDING)), byId.keys, customerIds),
        ValidationResult(false, true, true))

    printSummary()
}

// =====================================================================
//  Câu 11 — Aggregate, fold và reduce
// =====================================================================
private fun runCau11() {
    header("Cau 11 -- Aggregate, fold va reduce")
    resetCounters()

    val stats = AnalyticsExercises.completedOrderStats(orders, byId)
    println("   Thong ke don hoan tat:")
    println("     - So don       : ${stats.count}")
    println("     - Doanh thu    : ${stats.revenue.toCurrency()}")
    println("     - Trung binh   : ${stats.average?.let { String.format("%,.0fd", it) } ?: "N/A"}")
    println("     - Don thap nhat: ${stats.min?.toCurrency() ?: "N/A"}")
    println("     - Don cao nhat : ${stats.max?.toCurrency() ?: "N/A"}")
    println("     - Tong sp      : ${stats.totalUnits}")
    println()

    check("completedOrderStats", stats,
        OrderStats(3, 23_320_000L, 23_320_000.0 / 3, 320_000L, 21_000_000L, 8))

    printSummary()
}

// =====================================================================
//  Câu 12 — Tìm kiếm và phân trang
// =====================================================================
private fun runCau12() {
    header("Cau 12 -- Tim kiem va phan trang")
    resetCounters()

    check("Tim \"LAP\" (case-insensitive) -> Laptop",
        CatalogExercises.searchAndPaginate(products, "LAP", 1, 2).map { it.id }, listOf("p1"))
    check("Trang 2, pageSize=2, query rong",
        CatalogExercises.searchAndPaginate(products, "", 2, 2).map { it.id }, listOf("p4", "p1"))
    check("Trang ngoai pham vi -> []",
        CatalogExercises.searchAndPaginate(products, "", Int.MAX_VALUE, Int.MAX_VALUE), emptyList<Any>())

    printSummary()
}

// =====================================================================
//  Câu 13 — takeWhile và dropWhile
// =====================================================================
private fun runCau13() {
    header("Cau 13 -- takeWhile va dropWhile")
    resetCounters()

    val bands = CatalogExercises.splitByBudget(products, 500_000L)
    println("   Ngan sach: ${500_000L.toCurrency()}")
    println("   Trong ngan sach : ${bands.affordable.map { "${it.name}(${it.price.toCurrency()})" }}")
    println("   Vuot ngan sach  : ${bands.expensive.map { "${it.name}(${it.price.toCurrency()})" }}")
    println()

    check("affordable IDs", bands.affordable.map { it.id }, listOf("p5", "p4", "p2"))
    check("expensive IDs", bands.expensive.map { it.id }, listOf("p6", "p1"))

    printSummary()
}

// =====================================================================
//  Câu 14 — chunked, windowed và zipWithNext
// =====================================================================
private fun runCau14() {
    header("Cau 14 -- chunked, windowed va zipWithNext")
    resetCounters()

    val batches = AnalyticsExercises.shippingBatches(orders, 2)
    println("   Lo giao hang (batchSize=2):")
    batches.forEachIndexed { i, batch -> println("     Lo ${i + 1}: ${batch.map { it.id }}") }
    println()

    check("shippingBatches",
        batches.map { batch -> batch.map { it.id } }, listOf(listOf("o1", "o3"), listOf("o5")))

    val completedTotals = listOf(21_000_000L, 2_000_000L, 320_000L)
    println("   Doanh thu tung don: ${completedTotals.map { it.toCurrency() }}")
    println()

    check("movingRevenue(windowSize=2)",
        AnalyticsExercises.movingRevenue(completedTotals, 2), listOf(23_000_000L, 2_320_000L))
    check("movingRevenue(windowSize=4) -> [] (cua so qua lon)",
        AnalyticsExercises.movingRevenue(completedTotals, 4), emptyList<Any>())
    check("revenueChanges (zipWithNext)",
        AnalyticsExercises.revenueChanges(completedTotals), listOf(-19_000_000L, -1_680_000L))

    printSummary()
}

// =====================================================================
//  Câu 15 — mapIndexed, joinToString, zip và unzip
// =====================================================================
private fun runCau15() {
    header("Cau 15 -- mapIndexed, joinToString, zip va unzip")
    resetCounters()

    val spending = OrderExercises.spendingByCustomer(orders, byId)

    val label = AnalyticsExercises.completedOrderLabel(orders)
    println("   Nhan don hoan tat: $label")
    println()

    check("completedOrderLabel", label, "[1. o1 | 2. o3 | 3. o5]")

    val spendColumns = AnalyticsExercises.customerSpendColumns(customers, spending)
    println("   Cot ten     : ${spendColumns.first}")
    println("   Cot chi tieu: ${spendColumns.second.map { it.toCurrency() }}")
    println()

    check("customerSpendColumns (zip -> unzip)", spendColumns,
        listOf("An", "Bình", "Cường", "Dũng") to listOf(23_000_000L, 0L, 320_000L, 0L))

    printSummary()
}

// =====================================================================
//  Câu 16 — Sequence
// =====================================================================
private fun runCau16() {
    header("Cau 16 -- Sequence")
    resetCounters()

    val topExpensive = AnalyticsExercises.mostExpensiveAvailable(products, 2)
    println("   Top 2 san pham dat nhat con hang:")
    topExpensive.forEach { println("     - ${it.name} -- ${it.price.toCurrency()}") }
    println()

    check("mostExpensiveAvailable(2)", topExpensive.map { it.id }, listOf("p1", "p6"))

    val restock = AnalyticsExercises.restockPlan(5, 4)
    println("   Ke hoach nhap hang (firstBatch=5, 4 dot): $restock")
    println()

    check("restockPlan(5, 4)", restock, listOf(5, 10, 20, 40))
    check("mostExpensiveAvailable(0) -> []",
        AnalyticsExercises.mostExpensiveAvailable(products, 0), emptyList<Any>())
    check("restockPlan(5, 0) -> []",
        AnalyticsExercises.restockPlan(5, 0), emptyList<Any>())

    printSummary()
}

// =====================================================================
//  Câu 17 — Dashboard tổng hợp
// =====================================================================
private fun runCau17() {
    header("Cau 17 -- Dashboard tong hop")
    resetCounters()

    val dashboard = DashboardExercise.buildDashboard(products, customers, orders)
    println("   Dashboard:")
    println("     - San pham con hang : ${dashboard.availableProducts.map { it.name }}")
    println("     - Don hoan tat      : ${dashboard.completedOrderCount}")
    println("     - Tong doanh thu    : ${dashboard.totalRevenue.toCurrency()}")
    println("     - Top khach hang    : ${dashboard.topCustomers.map { "${it.name}=${it.amount.toCurrency()}" }}")
    println("     - Doanh thu/category: ${dashboard.revenueByCategory.map { "${it.key}=${it.value.toCurrency()}" }}")
    println("     - Canh bao ton kho  : ${dashboard.stockAlerts.map { "${it.productName}(${it.remaining})" }}")
    println()

    check("availableProducts", dashboard.availableProducts.map { it.id }, listOf("p5", "p4", "p2", "p6", "p1"))
    check("completedOrderCount", dashboard.completedOrderCount, 3)
    check("totalRevenue", dashboard.totalRevenue, 23_320_000L)
    check("topCustomers", dashboard.topCustomers,
        listOf(CustomerSpend("An", 23_000_000L), CustomerSpend("Cường", 320_000L)))
    check("revenueByCategory", dashboard.revenueByCategory,
        mapOf("Điện tử" to 23_000_000L, "Đồ uống" to 320_000L))
    check("stockAlerts", dashboard.stockAlerts,
        listOf(StockAlert("p3", "Bàn phím", 0), StockAlert("p1", "Laptop", 4)))

    printSummary()
}

// =====================================================================
//  Edge cases + Parameter validation
// =====================================================================
private fun runEdgeCases() {
    header("Edge Cases -- Collection rong")
    resetCounters()

    check("buildCatalogIndexes([])", CatalogExercises.buildCatalogIndexes(emptyList()).byId, emptyMap<String, Any>())
    check("uniqueCategories([])", CatalogExercises.uniqueCategories(emptyList()), emptySet<String>())
    check("compareTags([], [])", CatalogExercises.compareTags(emptyList(), emptyList()), SetComparison(emptySet(), emptySet(), emptySet()))
    check("uniqueCustomerEmails([])", CatalogExercises.uniqueCustomerEmails(emptyList()), emptyList<String>())
    check("completedItems([])", OrderExercises.completedItems(emptyList()), emptyList<OrderItem>())
    check("orderTotals([])", OrderExercises.orderTotals(emptyList(), byId), emptyMap<String, Long>())
    check("spendingByCustomer([])", OrderExercises.spendingByCustomer(emptyList(), byId), emptyMap<String, Long>())
    check("inventoryAfterCompletedOrders(products, [])",
        OrderExercises.inventoryAfterCompletedOrders(products, emptyList()),
        products.associate { it.id to it.stock })
    check("validateOrders([])", OrderExercises.validateOrders(emptyList(), emptySet(), emptySet()), ValidationResult(false, false, true))
    check("completedOrderStats([])", AnalyticsExercises.completedOrderStats(emptyList(), emptyMap()), OrderStats(0, 0L, null, null, null, 0))
    check("searchAndPaginate([])", CatalogExercises.searchAndPaginate(emptyList(), "", 1, 2), emptyList<Any>())
    check("shippingBatches([])", AnalyticsExercises.shippingBatches(emptyList(), 2), emptyList<Any>())
    check("movingRevenue([])", AnalyticsExercises.movingRevenue(emptyList(), 2), emptyList<Any>())
    check("revenueChanges([])", AnalyticsExercises.revenueChanges(emptyList()), emptyList<Any>())
    check("completedOrderLabel([])", AnalyticsExercises.completedOrderLabel(emptyList()), "[]")
    check("customerSpendColumns([])", AnalyticsExercises.customerSpendColumns(emptyList(), emptyMap()), emptyList<String>() to emptyList<Long>())
    check("mostExpensiveAvailable([])", AnalyticsExercises.mostExpensiveAvailable(emptyList(), 2), emptyList<Any>())
    check("buildDashboard([], [], [])",
        DashboardExercise.buildDashboard(emptyList(), emptyList(), emptyList()),
        DayTwo.StoreAnalytics.result.Dashboard(emptyList(), 0, 0L, emptyList(), emptyMap(), emptyList()))

    println()
    println("--- Parameter Validation -- IllegalArgumentException ---")
    println()

    val completedTotals = listOf(21_000_000L, 2_000_000L, 320_000L)
    checkThrows("searchAndPaginate: page = 0") { CatalogExercises.searchAndPaginate(products, "", 0, 2) }
    checkThrows("searchAndPaginate: pageSize = 0") { CatalogExercises.searchAndPaginate(products, "", 1, 0) }
    checkThrows("shippingBatches: batchSize = 0") { AnalyticsExercises.shippingBatches(orders, 0) }
    checkThrows("movingRevenue: windowSize = 0") { AnalyticsExercises.movingRevenue(completedTotals, 0) }
    checkThrows("mostExpensiveAvailable: count = -1") { AnalyticsExercises.mostExpensiveAvailable(products, -1) }
    checkThrows("restockPlan: firstBatch = 0") { AnalyticsExercises.restockPlan(0, 2) }
    checkThrows("restockPlan: numberOfBatches = -1") { AnalyticsExercises.restockPlan(5, -1) }

    println()
    println("--- Immutability Check ---")
    println()

    val originalProducts = StoreSampleData.products
    val originalCustomers = StoreSampleData.customers
    val originalOrders = StoreSampleData.orders
    check("products giu nguyen", products, originalProducts)
    check("customers giu nguyen", customers, originalCustomers)
    check("orders giu nguyen", orders, originalOrders)

    printSummary()
}

// =====================================================================
//  Chạy tất cả
// =====================================================================
private fun runAll() {
    runCau01(); runCau02(); runCau03(); runCau04(); runCau05()
    runCau06(); runCau07(); runCau08(); runCau09(); runCau10()
    runCau11(); runCau12(); runCau13(); runCau14(); runCau15()
    runCau16(); runCau17(); runEdgeCases()
}

// =====================================================================
//  MENU CHÍNH
// =====================================================================
private fun printMenu() {
    println()
    println("============================================================")
    println("  STORE ANALYTICS -- Collections Capstone")
    println("============================================================")
    println()
    println("  Phan A -- Chuan bi du lieu:")
    println("    1.  Cau 01 -- Gio hang mutable")
    println("    2.  Cau 02 -- Truy cap an toan va lap chi muc")
    println("    3.  Cau 03 -- Set va phep toan tap hop")
    println("    4.  Cau 04 -- Loc va sap xep catalog")
    println("    5.  Cau 05 -- Lam sach du lieu nullable")
    println()
    println("  Phan B -- Xu ly don hang:")
    println("    6.  Cau 06 -- Lam phang va loc theo kieu")
    println("    7.  Cau 07 -- Tong tien va Map")
    println("    8.  Cau 08 -- Nhom du lieu (groupBy)")
    println("    9.  Cau 09 -- Cap nhat ton kho")
    println("   10.  Cau 10 -- Kiem tra du lieu (any/all/none)")
    println()
    println("  Phan C -- Phan tich va trinh bay:")
    println("   11.  Cau 11 -- Aggregate, fold va reduce")
    println("   12.  Cau 12 -- Tim kiem va phan trang")
    println("   13.  Cau 13 -- takeWhile va dropWhile")
    println("   14.  Cau 14 -- chunked, windowed, zipWithNext")
    println("   15.  Cau 15 -- mapIndexed, joinToString, zip, unzip")
    println("   16.  Cau 16 -- Sequence")
    println("   17.  Cau 17 -- Dashboard tong hop")
    println()
    println("  Khac:")
    println("   18.  Edge cases + Parameter validation")
    println("    0.  Chay tat ca")
    println("    q.  Thoat")
    println()
    println("------------------------------------------------------------")
}

fun main() {
    while (true) {
        printMenu()
        print("  Nhap lua chon: ")
        val input = readlnOrNull()?.trim()?.lowercase() ?: break

        when (input) {
            "1"  -> runCau01()
            "2"  -> runCau02()
            "3"  -> runCau03()
            "4"  -> runCau04()
            "5"  -> runCau05()
            "6"  -> runCau06()
            "7"  -> runCau07()
            "8"  -> runCau08()
            "9"  -> runCau09()
            "10" -> runCau10()
            "11" -> runCau11()
            "12" -> runCau12()
            "13" -> runCau13()
            "14" -> runCau14()
            "15" -> runCau15()
            "16" -> runCau16()
            "17" -> runCau17()
            "18" -> runEdgeCases()
            "0"  -> runAll()
            "q"  -> { println("  Tam biet!"); break }
            else -> println("  Lua chon khong hop le. Nhap lai.")
        }

        println()
        print("  Nhan Enter de quay lai menu...")
        readlnOrNull()
    }
}

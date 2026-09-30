package DayTwo.StoreAnalytics.data

import DayTwo.StoreAnalytics.model.Customer
import DayTwo.StoreAnalytics.model.Order
import DayTwo.StoreAnalytics.model.OrderItem
import DayTwo.StoreAnalytics.model.OrderStatus
import DayTwo.StoreAnalytics.model.Product

object StoreSampleData {
    val products = listOf(
        Product("p1", "Laptop", "Điện tử", 20_000_000, 5, setOf("work", "premium")),
        Product("p2", "Chuột", "Điện tử", 500_000, 20, setOf("work", "accessory")),
        Product("p3", "Bàn phím", "Điện tử", 1_000_000, 0, setOf("work", "accessory")),
        Product("p4", "Cà phê", "Đồ uống", 120_000, 30, setOf("drink", "local")),
        Product("p5", "Trà", "Đồ uống", 80_000, 40, setOf("drink", "local")),
        Product("p6", "Webcam", "Điện tử", 1_500_000, 8, setOf("work", "video"))
    )

    val customers = listOf(
        Customer("c1", "An", " An@example.com "),
        Customer("c2", "Bình", null),
        Customer("c3", "Cường", "an@EXAMPLE.com"),
        Customer("c4", "Dũng", "dung@example.com")
    )

    val orders = listOf(
        Order("o1", "c1", listOf(OrderItem("p1", 1), OrderItem("p2", 2)), OrderStatus.COMPLETED),
        Order("o2", "c2", listOf(OrderItem("p4", 3), OrderItem("p5", 2)), OrderStatus.PENDING),
        Order("o3", "c1", listOf(OrderItem("p6", 1), OrderItem("p2", 1)), OrderStatus.COMPLETED),
        Order("o4", "c3", listOf(OrderItem("p1", 1)), OrderStatus.CANCELLED),
        Order("o5", "c3", listOf(OrderItem("p4", 2), OrderItem("p5", 1)), OrderStatus.COMPLETED)
    )
}
package DayTwo

fun main() {
    println("===============Aggregate Functions — Hàm tổng hợp===============")
    // Các hàm cơ bản
    val numbers = listOf(5, 2, 8, 1, 9, 3)
    numbers.count()               // 6
    numbers.count { it > 5 }      // 2 (số phần tử > 5)

    numbers.sum()                 // 28
    numbers.sumOf { it * 2 }      // 56

    numbers.average()             // 4.666...

    numbers.min()                 // 1
    numbers.max()                 // 9
    numbers.minOrNull()           // 1 (trả về null nếu rỗng)
    numbers.maxOrNull()           // 9

    //minBy / maxBy - theo tiêu chí
    data class Product(val name: String, val price: Int)

    val products = listOf(
        Product("Laptop", 1500),
        Product("Mouse", 25),
        Product("Keyboard", 75)
    )
    val cheapest = products.minByOrNull { it.price } // Mouse(25)
    val mostExpensive = products.maxByOrNull { it.price } // Laptop(1500)

    println("=============== Reduce & Fold — Tích lũy giá trị===============")
    val numbers1 = listOf(1, 2, 3, 4, 5)

    // reduce - tích lũy, bắt đầu từ phần tử đầu tiên
    val sum = numbers1.reduce { acc, value -> acc + value }
    val product = numbers1.reduce { acc, value -> acc * value }
    println(sum) // 15
    println(product) // 120

    //fold - tích lũy với giá trị khởi tạo
    val sumFrom100 = numbers1.fold(100){acc, value -> acc + value} // 115

    //fold có thể thay đổi kiểu dữ liệu
    val concatenated = numbers1.fold("Numbers: "){acc, value ->
        "$acc$value "
    }
    println(concatenated) // Numbers: 1 2 3 4 5

    // reduceRight / foldRight - duyệt từ phải sang trái
    val reversed = numbers1.foldRight(""){value, acc -> "$acc$value "}
    println(reversed) // 5 4 3 2 1
    // runningFold / runningReduce — trả về danh sách kết quả trung gian
    val runningSums = numbers1.runningFold(0) { acc, value -> acc + value }
    // [0, 1, 3, 6, 10, 15]

    val runningProducts = numbers1.runningReduce { acc, value -> acc * value }
    // [1, 2, 6, 24, 120]
}
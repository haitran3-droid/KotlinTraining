package DayTwo

fun main() {
    println("=============== Sequence — Xử lý lười (Lazy Evaluation) ================")
    // collection: eager - mỗi bước tạo list trung gian
    val eagerResult = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .filter { it % 2 == 0 }    // Tạo list [2, 4, 6, 8, 10]
        .map { it * it }            // Tạo list [4, 16, 36, 64, 100]
        .take(3)                    // Tạo list [4, 16, 36]

// Sequence: lazy — không tạo list trung gian
    val lazyResult = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .asSequence()               // Chuyển thành Sequence
        .filter { it % 2 == 0 }    // Lazy
        .map { it * it }            // Lazy
        .take(3)                    // Lazy
        .toList()                   // Terminal operation — thực thi tất cả

// generateSequence — tạo sequence vô hạn
    val powersOf2 = generateSequence(1) { it * 2 }
        .take(10)
        .toList()
// [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]

// sequence builder
    val fibonacci = sequence {
        var a = 0
        var b = 1
        while (true) {
            yield(a)
            val temp = a + b
            a = b
            b = temp
        }
    }
    fibonacci.take(10).toList()
// [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]
}
package DayOne

// Bài 6: Đa ràng buộc với where
fun <T> filterAndSort(items: List<T>, threshold: T): List<T>
        where T : CharSequence,
              T : Comparable<T> {
    return items
        .filter { it > threshold }
        .sorted()
}

fun main() {
    println("--- Test Bài 6: Multiple Constraint với where ---")

    val words = listOf("Kotlin", "Android", "Java", "Flutter", "Swift")
    val threshold = "Java"

    val result = filterAndSort(words, threshold)
    println("Từ điển lớn hơn '$threshold' và được sắp xếp:")
    println(result)
}

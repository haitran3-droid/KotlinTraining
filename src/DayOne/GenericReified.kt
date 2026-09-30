package DayOne

// Bài 10: Type Erasure với reified
inline fun <reified T> List<*>.filterSpecificType(): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        if (item is T) {
            result.add(item)
        }
    }
    return result
}

fun main() {
    println("--- Test Bài 10: Inline Reified filterSpecificType ---")

    val mixedList: List<*> = listOf(
        "Android",
        100,
        3.14,
        "Kotlin",
        200,
        true,
        "Jetpack Compose"
    )

    val strings: List<String> = mixedList.filterSpecificType<String>()
    println("Chỉ lấy String: $strings")

    val ints: List<Int> = mixedList.filterSpecificType<Int>()
    println("Chỉ lấy Int:    $ints")

    val doubles: List<Double> = mixedList.filterSpecificType<Double>()
    println("Chỉ lấy Double: $doubles")
}

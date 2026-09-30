package DayOne

// Bài 4: Generic Constraint (Upper Bound Number)
fun <T : Number> calculateAverage(numbers: List<T>): Double {
    if (numbers.isEmpty()) return 0.0

    var sum = 0.0
    for (num in numbers) {
        sum += num.toDouble()
    }
    return sum / numbers.size
}

fun main() {
    println("--- Test Bài 4: Generic Number Constraint ---")

    val intList = listOf(1, 2, 3, 4, 5)
    val doubleList = listOf(1.5, 2.5, 3.5)
    val floatList = listOf(10.0f, 20.0f)

    println("Trung bình Int:    ${calculateAverage(intList)}")
    println("Trung bình Double: ${calculateAverage(doubleList)}")
    println("Trung bình Float:  ${calculateAverage(floatList)}")
}

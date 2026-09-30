package DayOne

// Bài 2: Generic Extension Function (swap)
fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    if (index1 in indices && index2 in indices) {
        val temp = this[index1]
        this[index1] = this[index2]
        this[index2] = temp
    }
}

fun main() {
    println("--- Test Bài 2: Generic Extension swap ---")

    val numbers = mutableListOf(1, 2, 3, 4, 5)
    println("Trước khi swap: $numbers")
    numbers.swap(0, 4)
    println("Sau khi swap:   $numbers")

    val languages = mutableListOf("Java", "Kotlin", "Dart")
    println("Trước khi swap: $languages")
    languages.swap(0, 1)
    println("Sau khi swap:   $languages")
}

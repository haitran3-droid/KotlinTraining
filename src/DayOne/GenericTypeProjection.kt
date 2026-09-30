package DayOne

// Bài 9: Type Projection (Use-site Variance)
fun <T> copyData(from: Array<out T>, to: Array<in T>) {
    val count = minOf(from.size, to.size)
    for (i in 0 until count) {
        to[i] = from[i]
    }
}

fun main() {
    println("--- Test Bài 9: Type Projection với Array ---")

    val intSource: Array<Int> = arrayOf(10, 20, 30)

    val numberDest: Array<Number> = Array(3) { 0 }
    val anyDest: Array<Any> = Array(3) { "" }

    copyData(intSource, numberDest)
    println("numberDest sau khi copy: ${numberDest.joinToString()}")

    copyData(intSource, anyDest)
    println("anyDest sau khi copy:    ${anyDest.joinToString()}")
}

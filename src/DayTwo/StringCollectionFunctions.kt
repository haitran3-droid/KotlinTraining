package DayTwo

fun main() {
    val words = listOf("Kotlin", "is", "awesome")

    // joinToString - nối thành chuỗi
    println(words.joinToString()) //Kotlin, is, awesome
    println(words.joinToString(" "))  //Kotlin is awesome

    words.joinToString(
        separator = " | ",
        prefix = "[",
        postfix = "]"
    )  // "[Kotlin | is | awesome]"

    // joinToString với transform
    words.joinToString { it.uppercase() }   // "KOTLIN, IS, AWESOME"

    // joinToString với limit
    val longList = (1..100).toList()
    println(longList.joinToString(limit = 5, truncated = "..."))
    // "1, 2, 3, 4, 5, ..."

}
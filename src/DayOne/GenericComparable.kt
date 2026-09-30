package DayOne

// Bài 5: Generic Constraint với Comparable (findMax)
fun <T : Comparable<T>> findMax(a: T, b: T, c: T): T {
    var max = a
    if (b > max) max = b
    if (c > max) max = c
    return max
}

data class Student(val name: String, val score: Double) : Comparable<Student> {
    override fun compareTo(other: Student): Int {
        return this.score.compareTo(other.score)
    }
}

fun main() {
    println("--- Test Bài 5: Generic Comparable findMax ---")

    val maxInt = findMax(10, 99, 45)
    println("Max Int: $maxInt")

    val maxString = findMax("Apple", "Orange", "Banana")
    println("Max String: $maxString")

    val s1 = Student("An", 7.5)
    val s2 = Student("Bình", 9.0)
    val s3 = Student("Cường", 8.5)

    val topStudent = findMax(s1, s2, s3)
    println("Học sinh điểm cao nhất: ${topStudent.name} (${topStudent.score})")
}

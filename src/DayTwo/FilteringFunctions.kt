package DayTwo

fun main() {
    //filter & filterNot - lọc theo điều kiện
    println("=============== Filter & FilterNot ===============")
    val numbers = listOf(1,2,3,4,5,6,7,8,9,10)

    //filter - giữ lại phần tử thỏa mãn điều kiện
    val evens = numbers.filter{it % 2 == 0}
    println(evens) // [2, 4, 6, 8, 10]
    val greaterThan5 = numbers.filter{it > 5}
    println(greaterThan5) // [6, 7, 8, 9, 10]

    //filterNot - giữ lại phần tử không thỏa điều kiện
    val odds = numbers.filterNot { it % 2 == 0 }
    println(odds) // [1, 3, 5, 7, 9]

    //filterIndexed - lọc có index
    val result = numbers.filterIndexed { index, value ->
        index % 2 == 0 && value > 3
    }
    println(result) //[5, 7, 9]

    //filterIsInstance - lọc theo kiểu dữ liệu
    val mixed: List<Any> = listOf(1,"Hello", 2.5, "world", 3)
    val strings = mixed.filterIsInstance<String>()
    val ints = mixed.filterIsInstance<Int>()
    println(strings) //[Hello, world]
    println(ints) // [1, 3]

    // filterNotNull - loại bỏ null
    val withNulls = listOf(1, null,3,null, 5)
    val nonNulls = withNulls.filterNotNull()
    println(nonNulls) //[1, 3, 5]

    println("=============== Filter trên Map ===============")
    val scores = mapOf("An" to 90, "Bình" to 65, "Cường" to 85, "Dũng" to 50)
    //filterKeys - lọc theo key
    val shortNames = scores.filterKeys{it.length <= 2}
    println(shortNames) //{An=90}
    //filterValue - lọc theo value
    val passed = scores.filterValues{it >= 70}
    println(passed) // {An=90, Cường=85}
    //filter - lọc theo cả key và value
    println(scores.filter{(name, score) ->
        name.length > 2 && score >= 70
    }) // {Cường=85}
    println(scores.filter { it.key.length > 2 && it.value >= 60}) // {Bình=65, Cường=85}

    println("=============== Partition - Chia thành 2 nhóm ===============")
    val (evens1, odds1) = numbers.partition{it % 2 == 0}
    println(evens1) //[2, 4, 6, 8, 10]
    println(odds1) // [1, 3, 5, 7, 9]

    // ví dụ thực tế: Phân loại học sinh đậu/trượt
    data class Student(val name: String, val score: Int)
    val students = listOf(
        Student("An", 90), Student("Bình", 45),
        Student("Cường", 72), Student("Dũng", 38)
    )
    val (passed1, failed) = students.partition { it.score >= 50}
    // passed = [An(90), Cường(72)]
    // failed = [Bình(45), Dũng(38)]

    println("=============== Take & Drop - Lấy/Bỏ phần tử ===============")
    //take - lấy N phần tử đầu
    println(numbers.take(3)) //[1, 2, 3]
    println(numbers.takeLast(3)) // [8, 9, 10]

    //takeWhile - lấy phần tử liên tiếp từ đầu cho đến khi điều kiện sai
    println(numbers.takeWhile { it < 5 }) //[1, 2, 3, 4]
    println(numbers.takeLastWhile { it > 7 }) // [8, 9, 10]

    // drop — bỏ N phần tử đầu
    numbers.drop(3)              // [4, 5, 6, 7, 8, 9, 10]
    numbers.dropLast(3)          // [1, 2, 3, 4, 5, 6, 7]

    // dropWhile — bỏ phần tử liên tiếp từ đầu cho đến khi điều kiện sai
    numbers.dropWhile { it < 5 } // [5, 6, 7, 8, 9, 10]
    numbers.dropLastWhile { it > 7 } // [1, 2, 3, 4, 5, 6, 7]

    println("=============== Distinct — Loại bỏ trùng lặp ===============")
    val numbers1 = listOf(1, 2, 2, 3, 3, 3, 4)
    println(numbers1.distinct()) // [1, 2, 3, 4]

    //distinctBy - loại trùng theo tiêu chí
    data class User(val id: Int, val name: String)
    val users = listOf(
        User(1, "An"), User(2, "Bình"), User(1, "An Duplicate")
    )
    val uniqueById = users.distinctBy { it.id }
    println(uniqueById) // [User(id=1, name=An), User(id=2, name=Bình)]
    //trên String
    val words = listOf("Hello", "hello", "HELLO", "world")
    val uniqueIgnoreCase = words.distinctBy { it.lowercase() }
    println(uniqueIgnoreCase) // [Hello, world]
}
package DayTwo

fun main() {
    val numbers = listOf(5, 2, 8, 1, 9, 3)

    //sorted / sortedDescending - trả về list mới
    println(numbers.sorted()) //[1, 2, 3, 5, 8, 9]
    println(numbers.sortedDescending()) //[9, 8, 5, 3, 2, 1]

    //sortedBy / sortedByDescending - sắp xếp theo tiêu chí
    data class Person(val name: String, val age: Int)
    val people = listOf(Person("An", 30), Person("Bình", 25), Person("Cường", 28))

    println(people.sortedBy { it.age }) //[Person(name=Bình, age=25), Person(name=Cường, age=28), Person(name=An, age=30)]
    people.sortedByDescending { it.name}.forEach { print(it.name + ", ") } //Cường, Bình, An,

    //sortedWith - sắp xếp bằng Comparator tùy chiỉnh
    people.sortedWith(compareBy<Person> {it.age}.thenBy { it.name })
    // reversed
    numbers.reversed()            // [3, 9, 1, 8, 2, 5]

    // shuffled — xáo trộn ngẫu nhiên
    numbers.shuffled()
}
package DayTwo

fun main() {
    //List
    val fruits = listOf<String>("Táo", "Cam", "Xoài", "Táo", "Táo")
    val emptyList = emptyList<String>()
    val nonNullList = listOfNotNull("A", null, "B")

    println(fruits[0] + " " + fruits.getOrNull(99))
    /*println(emptyList)
    println(nonNullList)
    println(fruits.indexOf("Táo"))
    println(fruits.lastIndexOf("Táo"))
    println("Táo" in fruits)*/

    val numbers = mutableListOf(1, 2, 3, 4, 5)
    //Thêm phần tử
    numbers.add(6)
    println(numbers)
    numbers.add(0, 0)
    println(numbers)
    numbers.addAll(listOf(7, 8))
    println(numbers)
    println("-----------------------------------------")
    //Xóa phần tử
    numbers.remove(0)
    println(numbers)
    numbers.removeAt(0)
    println(numbers)
    numbers.removeAll { it > 5 }
    println(numbers)
    //Cập nhập
    numbers[0] = 99
    println(numbers)
    //sắp xếp
    numbers.sort()
    println(numbers)
    numbers.sortDescending()
    println(numbers)
    println("-----------------------------------------")
}
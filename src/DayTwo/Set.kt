package DayTwo

fun main() {
    val setA = setOf(1, 2, 3,4)
    val setB = setOf(3,4,5,6)

    //Hợp (Union) - tất cả các phần tử của 2 set

    println(setA union setB)

    //Giao (intersect) - phần tử chung

    println( setA intersect setB)

    //Hiệu (subtract) - phần tử chỉ có trong set A

    println(setA subtract setB)

    val setC = mutableSetOf(1,2,3,4)
    setC.add(6)
    println(setC)
    setC.remove(5)
    println(setC)
}
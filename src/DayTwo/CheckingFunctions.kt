package DayTwo

fun main() {
    println("===============Checking Functions - Hàm kiểm tra ===============")
    val numbers = listOf(1, 2, 3, 4, 5)
    //any - có Ít Nhất 1 phần tử thỏa mãn điều kiện
    numbers.any{it > 3} //true
    numbers.any{it > 10} // false
    emptyList<Int>().any() //false (list rỗng)

    //all - tất cả các phần tử thỏa mãn điều kiện
    numbers.all{it > 0} // true
    numbers.all{it> 3} //false

    //none - Không có phần tử nào thỏa mãn điều kiện
    numbers.none{it > 10} // true
    numbers.none{it > 3} // false
    emptyList<Int>().none()  // true (list rỗng)
}
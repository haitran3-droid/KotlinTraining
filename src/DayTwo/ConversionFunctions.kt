package DayTwo

fun main() {
    val list = listOf(1,2,3,2,1)
    //chuyển đổi giữa các loại collection
    list.toSet()                  // {1, 2, 3}
    list.toMutableList()          // MutableList [1, 2, 3, 2, 1]
    list.toMutableSet()           // MutableSet {1, 2, 3}

// Set → List
    val set = setOf("A", "B", "C")
    set.toList()                  // ["A", "B", "C"]

// Map chuyển đổi
    val map = mapOf("a" to 1, "b" to 2)
    map.toList()                  // [("a", 1), ("b", 2)] — List<Pair>
    map.toMutableMap()
    map.keys.toList()             // ["a", "b"]
    map.values.toList()           // [1, 2]

// Pair list → Map
    val pairs = listOf("x" to 10, "y" to 20)
    pairs.toMap()                 // {"x"=10, "y"=20}

// Array chuyển đổi
    list.toIntArray()             // IntArray
    list.toTypedArray()           // Array<Int>
}
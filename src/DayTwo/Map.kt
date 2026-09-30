package DayTwo

fun main() {
    val capitails = mapOf(
        "Vn" to "Hà Nội", "JP" to "Tokyo",
        "KR" to "Seoul"
    )
    println(capitails)

    val emptyMap = emptyMap<String, Int>()
    println(emptyMap)

    // Mutable Map
    val scores = mutableMapOf("An" to 90, "Bình" to 85)
    println(scores)

    // HashMap — không đảm bảo thứ tự
    val hashMap = hashMapOf("a" to 1, "b" to 2)
    println(hashMap)
    // LinkedHashMap — giữ thứ tự chèn (mặc định của mutableMapOf)
    val linkedMap = linkedMapOf("x" to 10, "y" to 20)
    println(linkedMap)
    // SortedMap — sắp xếp theo key
    val sortedMap = sortedMapOf("c" to 3, "a" to 1, "b" to 2)
    println(sortedMap)
    // Dùng buildMap
    val built = buildMap {
        put("name", "Kotlin")
        put("version", "1.9")
    }
    println(built)
    println("----------------------------------------------------------")

    val users = mutableMapOf("u1" to "An", "u2" to "Bình", "u3" to "Cường")
    //Truy cập
    println(users["u19"])
    println(users.getValue("u1"))
    println(users.getOrDefault("u99", "N/A"))
    println(users.getOrElse("u99"){"Không tìm thấy"})

    val key = users.entries.firstOrNull{it.value == "Bình"}?.key
    println(key)
    println("----------------------------------------------------------")
    for(entry in users.entries){
        println("Key: ${entry.key}, Value: ${entry.value}")
    }
    println("----------------------------------------------------------")
    for((key, value) in users.entries){
        println("$key -> $value")
    }
    println("----------------------------------------------------------")
    // Kiểm tra
    println(users.containsKey("u99"))
    println(users.containsValue("An"))
    println("u1" in users)
    println("----------------------------------------------------------")
    //Thêm / Cập nhập
    users["u4"] = "Dũng"
    users["u1"] = "An update"
    users.forEach { (key, value) -> println("$key -> $value") }
    println(users.keys) //[u1, u2, u3, u4]
    println(users.values) //[An update, Bình, Cường, Dũng]
    //Xóa
    users.remove("u3") //xóa theo key
    users.remove("u2", "Bình") //xóa chỉ khi key-value khớp
}
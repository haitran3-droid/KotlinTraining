package DayTwo

fun main() {
    //map - biến đổi từng phần
    println("=========MAP===========")
    val numbers = listOf(1, 2, 3, 4, 5)
    val doubled = numbers.map { it * 2 }
    println(doubled) //[2, 4, 6, 8, 10]
    val strings = numbers.map { "Số $it" }
    println(strings) //[Số 1, Số 2, Số 3, Số 4, Số 5]

    //mapIndexed - có thêm index
    val indexed = numbers.mapIndexed { index, value -> "$index: $value" }
    println(indexed) //[0: 1, 1: 2, 2: 3, 3: 4, 4: 5]
    println("-----------------------------------------")
    // mapNotNull - bỏ qua kết quả null
    val parsed = listOf("1", "abc", "3").mapNotNull { it.toIntOrNull() }
    println(parsed) // [1,3]
    // mapIndexedNotNull
    val result = numbers.mapIndexedNotNull { index, value ->
        if (index % 2 == 0) value * 10 else null
    }
    println(result) //[10, 30, 50]
    println("=========flatMAP===========")
    // flatMap - gộp danh sách lồng nhau
    val sentences = listOf("Hello World", "Kotlin is great")
    val words = sentences.flatMap { it.split(" ") }
    println(words)// ["Hello", "World", "Kotlin", "is", "great"]

    // so sánh với map
    val wordsNested = sentences.map { it.split(" ") }
    // [["Hello", "World"], ["Kotlin", "is", "great"]] — List lồng List!
    println(wordsNested)

    // ví dụ thực tế: lấy tất cả các items từ nhiều đơn hàng
    data class Order(val items: List<String>)

    val orders = listOf(
        Order(listOf("Laptop", "Mouse")),
        Order(listOf("Keyboard", "Monitor"))
    )
    val allItems = orders.flatMap { it.items }
    println(allItems) //[Laptop, Mouse, Keyboard, Monitor]
    //flatten - làm phẳng danh sách lồng
    val nested = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    println(nested)  //[[1, 2, 3], [4, 5, 6], [7, 8, 9]]
    val flat = nested.flatten()
    println(flat) //[1, 2, 3, 4, 5, 6, 7, 8, 9]
    println("=========ASSOCIATE===========")
    //associate - chuyển list thành map
    data class User(val id: Int, val name: String)

    val users = listOf(User(1, "An"), User(2, "Bình"), User(3, "Cường"))
    //associateBy - key là thuộc tính, value là object
    val byId = users.associateBy { it.id }
    println(byId) // {1=User(id=1, name=An), 2=User(id=2, name=Bình), 3=User(id=3, name=Cường)}
    //associateWith - key là object, valude là kết quả lamda
    val nameLengths = users.associateWith { it.name.length }
    println(nameLengths) // {User(id=1, name=An)=2, User(id=2, name=Bình)=4, User(id=3, name=Cường)=5}
    //associate - tạo cặp key-value tùy ý
    val idToName = users.associate { it.id to it.name }
    println(idToName)  //{1=An, 2=Bình, 3=Cường}
    println("=========GROUP BY===========")
    // groupBy - nhóm phần từ theo điều kiện
    val words1 = listOf("apple", "banana", "avocado", "blueberry", "cherry")
    val grouped = words1.groupBy { it.first() }
    println(grouped) // {a=[apple, avocado], b=[banana, blueberry], c=[cherry]}
    // groupBy với value transform
    val groupedLengths = words1.groupBy(
        keySelector = {it.first()},
        valueTransform = {it.length}
    )
    println(groupedLengths) //{a=[5, 7], b=[6, 9], c=[6]}
    // ví dụ thực tế: nhóm nhân viên theo phòng ban
    data class Employee(val name: String, val dept: String)
    val employees = listOf(
        Employee("An", "IT"), Employee("Bình", "HR"),
        Employee("Cường", "IT"), Employee("Dũng", "HR")
    )
    val byDept = employees.groupBy(
        keySelector = {it.dept},
        valueTransform = {it.name}
    )
    println(byDept) // {IT=[An, Cường], HR=[Bình, Dũng]}
    println("=========Zip & Unzip===========")
    //zip & unzip — Ghép và tách cặp
    val names = listOf ("An", "Binh", "Cuong")
    val ages = listOf(25, 30, 28)
    //zip - ghép thành List<Pair>
    val pairs = names.zip(ages)
    println(pairs) //[(An, 25), (Binh, 30), (Cuong, 28)]
    //zip với transform
    val info = names.zip(ages){name, age -> "$name ($age tuổi)"}
    println(info) // [An (25 tuổi), Binh (30 tuổi), Cuong (28 tuổi)]
    //unzip - tách List<Pair> thành Pair<List, List>
    val (nameList, ageList) = pairs.unzip()
    println(nameList) //[An, Binh, Cuong]
    println(ageList)  //[25, 30, 28]

    println("=========Chunked & Windowed===========")
    // chunked & windowed — Chia nhóm
    val numbers1 = (1..10).toList()
    //chunked - chia thành các nhóm c kích thước cố định
    val numbers2 = numbers1.chunked(3)
    println(numbers2) //[[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]]

    //chunked với transform
    println(numbers1.chunked(3){chunk -> chunk.sum()}) //[6, 15, 24, 10]

    //windowed - tạo cửa sổ trượt
    println(numbers1.windowed(3))// [[1,2,3], [2,3,4], [3,4,5], ..., [8,9,10]]

    // windowed với step
    println(numbers1.windowed(size = 3, step = 2)) // [[1,2,3], [3,4,5], [5,6,7], [7,8,9]]

    // zipWithNext — ghép mỗi phần tử với phần tử kế tiếp
    println(numbers1.zipWithNext()) // [(1,2), (2,3), (3,4), ..., (9,10)]
}
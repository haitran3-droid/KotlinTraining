# Kotlin Collections: List, Set, Map — Functions & Extensions

> [!NOTE]
> Tài liệu tham khảo: [Kotlin Official Docs - Collections](https://kotlinlang.org/docs/collections-overview.html)

**Cách đọc ví dụ:** mỗi đoạn code minh họa riêng một ý; không ghép mọi biến cùng tên thành một file. `sortedSetOf`, `sortedMapOf` và một số thao tác Java Map trong bài dùng cho Kotlin/JVM. Xem thêm [40 câu hỏi và trả lời](kotlin_collections_qa.md).

---

## 1. Tổng quan về Collections trong Kotlin

Kotlin cung cấp một hệ thống Collection phong phú, được chia thành **2 nhóm chính**:

| Loại | Mutable (Thay đổi được) | Read-only (Chỉ đọc) |
|------|--------------------------|----------------------|
| **List** | `MutableList<T>` | `List<T>` |
| **Set** | `MutableSet<T>` | `Set<T>` |
| **Map** | `MutableMap<K, V>` | `Map<K, V>` |

> [!IMPORTANT]
> `List`, `Set`, `Map` là interface **read-only**, không cung cấp API thêm/xóa/cập nhật phần tử. Đây không phải cam kết dữ liệu bất biến: nơi khác giữ tham chiếu mutable vẫn có thể sửa cùng đối tượng. Dùng `mutableListOf()`, `mutableSetOf()`, `mutableMapOf()` khi cần API thay đổi nội dung.

```kotlin
// Read-only — không thể gọi add/remove qua interface List
val names: List<String> = listOf("An", "Bình", "Cường")

// Mutable — có thể add/remove
val mutableNames: MutableList<String> = mutableListOf("An", "Bình")
mutableNames.add("Cường") // ✅ OK
```

---

## 2. List — Danh sách có thứ tự

`val` chỉ ngăn gán lại biến, không cấm sửa một collection mutable. Ví dụ hai biến có thể tham chiếu cùng danh sách:

```kotlin
val editable = mutableListOf("An")
val readable: List<String> = editable
editable.add("Bình")
println(readable) // [An, Bình]
```

### 2.1. Khởi tạo List

```kotlin
// Read-only List
val fruits = listOf("Táo", "Cam", "Xoài")
val emptyList = emptyList<String>()
val nonNullList = listOfNotNull("A", null, "B") // → ["A", "B"]

// Mutable List
val mutableFruits = mutableListOf("Táo", "Cam")
val arrayList = arrayListOf("X", "Y", "Z") // Backed by ArrayList

// Từ Array
val fromArray = intArrayOf(1, 2, 3).toList()

// Dùng buildList (Kotlin 1.6+)
val built = buildList {
    add("Một")
    add("Hai")
    addAll(listOf("Ba", "Bốn"))
}
```

### 2.2. Truy cập phần tử

```kotlin
val colors = listOf("Đỏ", "Xanh", "Vàng", "Tím")

colors[0]                  // "Đỏ" — có thể ném IndexOutOfBoundsException
colors.get(1)              // "Xanh"
colors.first()             // "Đỏ"
colors.last()              // "Tím"
colors.firstOrNull()       // "Đỏ" — null nếu list rỗng
colors.lastOrNull()        // "Tím" — null nếu list rỗng
colors.getOrNull(99)       // null — an toàn, không ném exception
colors.getOrElse(99) { "Mặc định" } // "Mặc định"

colors.indexOf("Vàng")    // 2
colors.lastIndexOf("Đỏ")  // 0
colors.contains("Tím")    // true
"Xanh" in colors           // true
```

### 2.3. Các hàm thao tác List

```kotlin
val numbers = mutableListOf(1, 2, 3, 4, 5)

// Thêm phần tử
numbers.add(6)               // [1, 2, 3, 4, 5, 6]
numbers.add(0, 0)            // [0, 1, 2, 3, 4, 5, 6] — thêm tại index 0
numbers.addAll(listOf(7, 8)) // [0, 1, 2, 3, 4, 5, 6, 7, 8]

// Xóa phần tử
numbers.remove(0)            // Xóa phần tử có giá trị 0
numbers.removeAt(0)          // Xóa phần tử tại index 0
numbers.removeAll { it > 5 } // Xóa tất cả phần tử > 5

// Cập nhật
numbers[0] = 99              // Gán giá trị tại index 0

// Sắp xếp
numbers.sort()               // Sắp xếp tại chỗ (mutable)
numbers.sortDescending()     // Sắp xếp giảm dần tại chỗ
```

---

## 3. Set — Tập hợp không trùng lặp

### 3.1. Khởi tạo Set

```kotlin
// Read-only Set
val uniqueNumbers = setOf(1, 2, 3, 2, 1)  // → {1, 2, 3}
val emptySet = emptySet<Int>()

// Mutable Set
val mutableSet = mutableSetOf("A", "B", "C")

// LinkedHashSet — giữ thứ tự chèn
val linkedSet = linkedSetOf("X", "Y", "Z")

// HashSet — không đảm bảo thứ tự; tra cứu trung bình O(1)
val hashSet = hashSetOf(3, 1, 2)

// TreeSet (SortedSet) — tự động sắp xếp
val sortedSet = sortedSetOf(5, 3, 1, 4, 2) // → {1, 2, 3, 4, 5}

// Dùng buildSet
val built = buildSet {
    add("Kotlin")
    addAll(listOf("Java", "Kotlin")) // "Kotlin" chỉ xuất hiện 1 lần
}
```

### 3.2. Các phép toán trên Set

```kotlin
val setA = setOf(1, 2, 3, 4)
val setB = setOf(3, 4, 5, 6)

// Hợp (Union) — tất cả phần tử của cả 2 set
setA union setB          // {1, 2, 3, 4, 5, 6}

// Giao (Intersect) — phần tử chung
setA intersect setB      // {3, 4}

// Hiệu (Subtract) — phần tử chỉ có trong setA
setA subtract setB       // {1, 2}
```

> [!TIP]
> **Khi nào dùng Set thay vì List?**
> - Khi cần **loại bỏ phần tử trùng lặp**
> - Khi cần kiểm tra **phần tử tồn tại** thường xuyên (`contains()` trung bình O(1) với HashSet; không phải bảo đảm cho mọi Set)
> - Khi cần thực hiện **phép toán tập hợp** (union, intersect, subtract)

Với HashSet/LinkedHashSet, phần tử trùng dựa trên `equals()` và `hashCode()`. Hai data class có cùng ID nhưng thuộc tính khác vẫn có thể là hai phần tử khác nhau. SortedSet xác định trùng theo comparator/thứ tự so sánh. Đừng sửa thuộc tính tham gia `equals/hashCode` của một object khi nó đang được dùng trong hash set hoặc làm key của hash map.

---

## 4. Map — Cặp Key-Value

### 4.1. Khởi tạo Map

```kotlin
// Read-only Map
val capitals = mapOf(
    "VN" to "Hà Nội",
    "JP" to "Tokyo",
    "KR" to "Seoul"
)
val emptyMap = emptyMap<String, Int>()

// Mutable Map
val scores = mutableMapOf("An" to 90, "Bình" to 85)

// HashMap — không đảm bảo thứ tự
val hashMap = hashMapOf("a" to 1, "b" to 2)

// LinkedHashMap — giữ thứ tự chèn (mặc định của mutableMapOf)
val linkedMap = linkedMapOf("x" to 10, "y" to 20)

// SortedMap — sắp xếp theo key
val sortedMap = sortedMapOf("c" to 3, "a" to 1, "b" to 2)

// Dùng buildMap
val built = buildMap {
    put("name", "Kotlin")
    put("version", "1.9")
}
```

### 4.2. Truy cập và thao tác Map

```kotlin
val users = mutableMapOf("u1" to "An", "u2" to "Bình", "u3" to "Cường")

// Truy cập
users["u1"]                      // "An" — trả về null nếu không tìm thấy
users.getValue("u1")             // "An" — ném NoSuchElementException nếu không có
users.getOrDefault("u99", "N/A") // "N/A"
users.getOrElse("u99") { "Không tìm thấy" }

// Kiểm tra
users.containsKey("u1")          // true
users.containsValue("An")       // true
"u1" in users                    // true

// Thêm / Cập nhật
users["u4"] = "Dũng"            // Thêm mới
users["u1"] = "An Updated"      // Cập nhật
users.putIfAbsent("u1", "X")    // Thêm nếu key chưa có hoặc đang ánh xạ tới null

// Xóa
users.remove("u3")              // Xóa theo key
users.remove("u2", "Bình")      // Xóa chỉ khi key-value khớp

// Duyệt
users.forEach { (key, value) ->
    println("$key → $value")
}

// Lấy keys và values
users.keys    // Set<String>
users.values  // Collection<String>
users.entries // Set<Map.Entry<String, String>>
```

---

**Map và null:** key là duy nhất, value có thể trùng. Với `Map<K, V?>`, `map[key] == null` có thể là key không tồn tại hoặc value đang là null; dùng `containsKey` để phân biệt. `getOrElse` dùng fallback khi kết quả đọc là null, còn `getOrDefault` dùng mặc định khi key không tồn tại.

## 5. Transformation Functions — Hàm biến đổi

### 5.1. `map` — Biến đổi từng phần tử

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// Biến đổi mỗi phần tử
val doubled = numbers.map { it * 2 }          // [2, 4, 6, 8, 10]
val strings = numbers.map { "Số $it" }        // ["Số 1", "Số 2", ...]

// mapIndexed — có thêm index
val indexed = numbers.mapIndexed { index, value ->
    "$index: $value"
}  // ["0: 1", "1: 2", "2: 3", ...]

// mapNotNull — bỏ qua kết quả null
val parsed = listOf("1", "abc", "3").mapNotNull { it.toIntOrNull() }
// [1, 3]

// mapIndexedNotNull
val result = numbers.mapIndexedNotNull { index, value ->
    if (index % 2 == 0) value * 10 else null
}  // [10, 30, 50]
```

> [!TIP]
> **Khi nào dùng `map`?**
> - Khi cần **chuyển đổi kiểu dữ liệu** (ví dụ: `Entity` → `DTO`, `Model` → `UIState`)
> - Khi cần **tính toán trên từng phần tử** mà không thay đổi collection gốc
> - Dùng `mapNotNull` khi kết quả biến đổi **có thể null** và muốn bỏ qua null

`map` tạo danh sách kết quả nhưng không sao chép sâu phần tử, cũng không ngăn lambda sửa object nguồn. Muốn giữ dữ liệu cũ để so sánh, tạo object mới thay vì sửa trực tiếp object cũ.

### 5.2. `flatMap` — Gộp danh sách lồng nhau

```kotlin
val sentences = listOf("Hello World", "Kotlin is great")

val words = sentences.flatMap { it.split(" ") }
// ["Hello", "World", "Kotlin", "is", "great"]

// So sánh với map
val wordsNested = sentences.map { it.split(" ") }
// [["Hello", "World"], ["Kotlin", "is", "great"]] — List lồng List!

// Ví dụ thực tế: Lấy tất cả items từ nhiều đơn hàng
data class Order(val items: List<String>)
val orders = listOf(
    Order(listOf("Laptop", "Mouse")),
    Order(listOf("Keyboard", "Monitor"))
)
val allItems = orders.flatMap { it.items }
// ["Laptop", "Mouse", "Keyboard", "Monitor"]
```

> [!TIP]
> **Khi nào dùng `flatMap`?**
> - Khi mỗi phần tử **tạo ra một danh sách** và bạn muốn **gộp tất cả** thành 1 danh sách phẳng
> - Thay thế pattern `map { ... }.flatten()`

### 5.3. `flatten` — Làm phẳng danh sách lồng

```kotlin
val nested = listOf(
    listOf(1, 2, 3),
    listOf(4, 5),
    listOf(6)
)
val flat = nested.flatten() // [1, 2, 3, 4, 5, 6]
```

### 5.4. `associate` — Chuyển List thành Map

```kotlin
data class User(val id: Int, val name: String)
val users = listOf(User(1, "An"), User(2, "Bình"), User(3, "Cường"))

// associateBy — key là thuộc tính, value là object
val byId = users.associateBy { it.id }
// {1=User(1, "An"), 2=User(2, "Bình"), 3=User(3, "Cường")}

// associateWith — key là object, value là kết quả lambda
val nameLengths = users.associateWith { it.name.length }
// {User(1, "An")=2, User(2, "Bình")=4, User(3, "Cường")=5}

// associate — tạo cặp key-value tùy ý
val idToName = users.associate { it.id to it.name }
// {1="An", 2="Bình", 3="Cường"}
```

**Key trùng:** các hàm `associate*` giữ value của phần tử cuối cho mỗi key. Nếu cần giữ tất cả phần tử trong cùng nhóm, dùng `groupBy`.

### 5.5. `groupBy` — Nhóm phần tử theo điều kiện

```kotlin
val words = listOf("apple", "banana", "avocado", "blueberry", "cherry")

val grouped = words.groupBy { it.first() }
// {'a'=["apple", "avocado"], 'b'=["banana", "blueberry"], 'c'=["cherry"]}

// groupBy với value transform
val groupedLengths = words.groupBy(
    keySelector = { it.first() },
    valueTransform = { it.length }
)
// {'a'=[5, 7], 'b'=[6, 9], 'c'=[6]}

// Ví dụ thực tế: Nhóm nhân viên theo phòng ban
data class Employee(val name: String, val dept: String)
val employees = listOf(
    Employee("An", "IT"), Employee("Bình", "HR"),
    Employee("Cường", "IT"), Employee("Dũng", "HR")
)
val byDept = employees.groupBy { it.dept }
// {"IT"=[An, Cường], "HR"=[Bình, Dũng]}
```

### 5.6. `zip` & `unzip` — Ghép và tách cặp

```kotlin
val names = listOf("An", "Bình", "Cường")
val ages = listOf(25, 30, 28)

// zip — ghép thành List<Pair>
val pairs = names.zip(ages)
// [("An", 25), ("Bình", 30), ("Cường", 28)]

// zip với transform
val info = names.zip(ages) { name, age -> "$name ($age tuổi)" }
// ["An (25 tuổi)", "Bình (30 tuổi)", "Cường (28 tuổi)"]

// unzip — tách List<Pair> thành Pair<List, List>
val (nameList, ageList) = pairs.unzip()
// nameList = ["An", "Bình", "Cường"]
// ageList = [25, 30, 28]
```

Nếu hai nguồn khác độ dài, `zip` chỉ tạo số cặp bằng nguồn ngắn hơn; phần dư bị bỏ khỏi kết quả.

### 5.7. `chunked` & `windowed` — Chia nhóm

```kotlin
val numbers = (1..10).toList()

// chunked — chia thành các nhóm có kích thước cố định
numbers.chunked(3)
// [[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]]

// chunked với transform
numbers.chunked(3) { chunk -> chunk.sum() }
// [6, 15, 24, 10]

// windowed — tạo cửa sổ trượt
numbers.windowed(3)
// [[1,2,3], [2,3,4], [3,4,5], ..., [8,9,10]]

// windowed với step
numbers.windowed(size = 3, step = 2)
// [[1,2,3], [3,4,5], [5,6,7], [7,8,9]]

// zipWithNext — ghép mỗi phần tử với phần tử kế tiếp
numbers.zipWithNext()
// [(1,2), (2,3), (3,4), ..., (9,10)]
```

---

`chunked` giữ nhóm cuối dù thiếu phần tử. `windowed` mặc định bỏ cửa sổ thiếu phần tử; dùng `partialWindows = true` nếu muốn giữ chúng.

## 6. Filtering Functions — Hàm lọc

### 6.1. `filter` & `filterNot` — Lọc theo điều kiện

```kotlin
val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

// filter — giữ lại phần tử thỏa điều kiện
val evens = numbers.filter { it % 2 == 0 }      // [2, 4, 6, 8, 10]
val greaterThan5 = numbers.filter { it > 5 }     // [6, 7, 8, 9, 10]

// filterNot — giữ lại phần tử KHÔNG thỏa điều kiện
val odds = numbers.filterNot { it % 2 == 0 }     // [1, 3, 5, 7, 9]

// filterIndexed — lọc có index
val result = numbers.filterIndexed { index, value ->
    index % 2 == 0 && value > 3
}  // [5, 7, 9]

// filterIsInstance — lọc theo kiểu dữ liệu
val mixed: List<Any> = listOf(1, "hello", 2.5, "world", 3)
val strings = mixed.filterIsInstance<String>()    // ["hello", "world"]
val ints = mixed.filterIsInstance<Int>()           // [1, 3]

// filterNotNull — loại bỏ null
val withNulls = listOf(1, null, 3, null, 5)
val nonNulls = withNulls.filterNotNull()          // [1, 3, 5]
```

### 6.2. `filter` trên Map

```kotlin
val scores = mapOf("An" to 90, "Bình" to 65, "Cường" to 85, "Dũng" to 50)

// filterKeys — lọc theo key
val shortNames = scores.filterKeys { it.length <= 2 }
// {"An"=90}

// filterValues — lọc theo value
val passed = scores.filterValues { it >= 70 }
// {"An"=90, "Cường"=85}

// filter — lọc theo cả key và value
val result = scores.filter { (name, score) ->
    name.length > 2 && score >= 70
}
// {"Cường"=85}
```

### 6.3. `partition` — Chia thành 2 nhóm

```kotlin
val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

val (evens, odds) = numbers.partition { it % 2 == 0 }
// evens = [2, 4, 6, 8, 10]
// odds  = [1, 3, 5, 7, 9]

// Ví dụ thực tế: Phân loại học sinh đậu/rớt
data class Student(val name: String, val score: Int)
val students = listOf(
    Student("An", 90), Student("Bình", 45),
    Student("Cường", 72), Student("Dũng", 38)
)
val (passed, failed) = students.partition { it.score >= 50 }
// passed = [An(90), Cường(72)]
// failed = [Bình(45), Dũng(38)]
```

> [!TIP]
> **Khi nào dùng `partition` thay vì `filter`?**
> - Khi cần **cả 2 nhóm** (thỏa và không thỏa điều kiện)
> - Tránh phải gọi `filter` + `filterNot` hai lần → hiệu suất tốt hơn

### 6.4. `take` & `drop` — Lấy/Bỏ phần tử

```kotlin
val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

// take — lấy N phần tử đầu
numbers.take(3)              // [1, 2, 3]
numbers.takeLast(3)          // [8, 9, 10]

// takeWhile — lấy phần tử liên tiếp từ đầu cho đến khi điều kiện sai
numbers.takeWhile { it < 5 } // [1, 2, 3, 4]
numbers.takeLastWhile { it > 7 } // [8, 9, 10]

// drop — bỏ N phần tử đầu
numbers.drop(3)              // [4, 5, 6, 7, 8, 9, 10]
numbers.dropLast(3)          // [1, 2, 3, 4, 5, 6, 7]

// dropWhile — bỏ phần tử liên tiếp từ đầu cho đến khi điều kiện sai
numbers.dropWhile { it < 5 } // [5, 6, 7, 8, 9, 10]
numbers.dropLastWhile { it > 7 } // [1, 2, 3, 4, 5, 6, 7]
```

### 6.5. `distinct` — Loại bỏ trùng lặp

```kotlin
val numbers = listOf(1, 2, 2, 3, 3, 3, 4)

numbers.distinct()            // [1, 2, 3, 4]

// distinctBy — loại trùng theo tiêu chí
data class User(val id: Int, val name: String)
val users = listOf(
    User(1, "An"), User(2, "Bình"), User(1, "An Duplicate")
)
val uniqueById = users.distinctBy { it.id }
// [User(1, "An"), User(2, "Bình")]

// Trên String
val words = listOf("Hello", "hello", "HELLO", "world")
val uniqueIgnoreCase = words.distinctBy { it.lowercase() }
// ["Hello", "world"]
```

---

## 7. Checking Functions — Hàm kiểm tra

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// any — có ÍT NHẤT 1 phần tử thỏa điều kiện?
numbers.any { it > 3 }        // true
numbers.any { it > 10 }       // false
emptyList<Int>().any()         // false (list rỗng)

// all — TẤT CẢ phần tử thỏa điều kiện?
numbers.all { it > 0 }        // true
numbers.all { it > 3 }        // false

// none — KHÔNG CÓ phần tử nào thỏa điều kiện?
numbers.none { it > 10 }      // true
numbers.none { it > 3 }       // false
emptyList<Int>().none()        // true (list rỗng)
```

> [!WARNING]
> **Chú ý:** `all` trả về `true` cho collection rỗng. Chỉ thêm `isNotEmpty()` nếu yêu cầu nghiệp vụ cần ít nhất một phần tử, ví dụ “có học sinh và tất cả đều đậu”. Đây là hành vi đúng của API, không phải lỗi.
> ```kotlin
> emptyList<Int>().all { it > 100 } // true! (vacuous truth)
> ```

---

## 8. Aggregate Functions — Hàm tổng hợp

### 8.1. Các hàm cơ bản

```kotlin
val numbers = listOf(5, 2, 8, 1, 9, 3)

numbers.count()               // 6
numbers.count { it > 5 }      // 2 (số phần tử > 5)

numbers.sum()                 // 28
numbers.sumOf { it * 2 }      // 56

numbers.average()             // 4.666...

numbers.min()                 // 1
numbers.max()                 // 9
numbers.minOrNull()           // 1 (trả về null nếu rỗng — khuyên dùng)
numbers.maxOrNull()           // 9

// minBy / maxBy — theo tiêu chí
data class Product(val name: String, val price: Int)
val products = listOf(
    Product("Laptop", 1500),
    Product("Mouse", 25),
    Product("Keyboard", 75)
)
val cheapest = products.minByOrNull { it.price } // Mouse(25)
val mostExpensive = products.maxByOrNull { it.price } // Laptop(1500)
```

Với collection số rỗng, `sum()` trả 0, `average()` trả `NaN`; `min()/max()` ném exception, còn `minOrNull()/maxOrNull()` trả null. Các hàm `minByOrNull/maxByOrNull` trả object phần tử, không phải riêng giá trị tiêu chí.

### 8.2. `reduce` & `fold` — Tích lũy giá trị

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// reduce — tích lũy, bắt đầu từ phần tử đầu tiên
val sum = numbers.reduce { acc, value -> acc + value }     // 15
val product = numbers.reduce { acc, value -> acc * value } // 120

// fold — tích lũy với giá trị khởi tạo
val sumFrom100 = numbers.fold(100) { acc, value -> acc + value } // 115

// fold có thể thay đổi kiểu dữ liệu
val concatenated = numbers.fold("Numbers: ") { acc, value ->
    "$acc$value "
}  // "Numbers: 1 2 3 4 5 "

// reduceRight / foldRight — duyệt từ phải sang trái
val reversed = numbers.foldRight("") { value, acc ->
    "$acc$value "
}  // "5 4 3 2 1 "

// runningFold / runningReduce — trả về danh sách kết quả trung gian
val runningSums = numbers.runningFold(0) { acc, value -> acc + value }
// [0, 1, 3, 6, 10, 15]

val runningProducts = numbers.runningReduce { acc, value -> acc * value }
// [1, 2, 6, 24, 120]
```

> [!TIP]
> **`reduce` vs `fold`:**
> - `reduce`: Bắt đầu tích lũy từ phần tử đầu; thường dùng kết quả cùng kiểu phần tử, cũng có thể tích lũy ở một kiểu cha phù hợp. Ném exception nếu list rỗng.
> - `fold`: Dùng khi cần **giá trị khởi tạo** hoặc kiểu kết quả **khác kiểu phần tử**. An toàn với list rỗng.

---

## 9. Sorting Functions — Hàm sắp xếp

```kotlin
val numbers = listOf(5, 2, 8, 1, 9, 3)

// sorted / sortedDescending — trả về list MỚI
numbers.sorted()              // [1, 2, 3, 5, 8, 9]
numbers.sortedDescending()    // [9, 8, 5, 3, 2, 1]

// sortedBy / sortedByDescending — sắp xếp theo tiêu chí
data class Person(val name: String, val age: Int)
val people = listOf(Person("An", 30), Person("Bình", 25), Person("Cường", 28))

people.sortedBy { it.age }
// [Bình(25), Cường(28), An(30)]

people.sortedByDescending { it.name }
// [Cường, Bình, An]

// sortedWith — sắp xếp bằng Comparator tùy chỉnh
people.sortedWith(compareBy<Person> { it.age }.thenBy { it.name })

// reversed
numbers.reversed()            // [3, 9, 1, 8, 2, 5]

// shuffled — xáo trộn ngẫu nhiên
numbers.shuffled()
```

---

## 10. Conversion Functions — Hàm chuyển đổi

```kotlin
val list = listOf(1, 2, 3, 2, 1)

// Chuyển đổi giữa các loại Collection
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
```

---

`toList()/toMutableList()` không sao chép sâu các object bên trong. Thay đổi cấu trúc list kết quả không sửa cấu trúc list nguồn, nhưng các object mutable bên trong có thể vẫn được chia sẻ. `toSet()` loại trùng; `pairs.toMap()` giữ value cuối khi key trùng.

## 11. String Collection Functions — Hàm xử lý chuỗi

```kotlin
val words = listOf("Kotlin", "is", "awesome")

// joinToString — nối thành chuỗi
words.joinToString()                    // "Kotlin, is, awesome"
words.joinToString(separator = " ")     // "Kotlin is awesome"
words.joinToString(
    separator = " | ",
    prefix = "[",
    postfix = "]"
)  // "[Kotlin | is | awesome]"

// joinToString với transform
words.joinToString { it.uppercase() }   // "KOTLIN, IS, AWESOME"

// joinToString với limit
val longList = (1..100).toList()
longList.joinToString(limit = 5, truncated = "...")
// "1, 2, 3, 4, 5, ..."
```

---

## 12. Sequence — Xử lý lười (Lazy Evaluation)

```kotlin
// Pipeline trên List: eager — filter/map/take ở đây tạo các list kết quả
val eagerResult = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    .filter { it % 2 == 0 }    // Tạo list [2, 4, 6, 8, 10]
    .map { it * it }            // Tạo list [4, 16, 36, 64, 100]
    .take(3)                    // Tạo list [4, 16, 36]

// Pipeline Sequence này: filter/map/take lazy, tránh các list trung gian
val lazyResult = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    .asSequence()               // Chuyển thành Sequence
    .filter { it % 2 == 0 }    // Lazy
    .map { it * it }            // Lazy
    .take(3)                    // Lazy
    .toList()                   // Terminal: chạy pipeline đến khi có đủ 3 kết quả

// generateSequence — tạo sequence vô hạn
val powersOf2 = generateSequence(1) { it * 2 }
    .take(10)
    .toList()
// [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]

// sequence builder
val fibonacci = sequence {
    var a = 0
    var b = 1
    while (true) {
        yield(a)
        val temp = a + b
        a = b
        b = temp
    }
}
fibonacci.take(10).toList()
// [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]
```

> [!IMPORTANT]
> **Khi nào dùng Sequence thay vì Collection?**
> - Khi cần giảm các danh sách trung gian trong pipeline nhiều bước; nguồn lớn có thể hưởng lợi nhưng không có ngưỡng cố định bảo đảm nhanh hơn
> - Khi có **chuỗi nhiều phép biến đổi** (filter → map → take → ...)
> - Khi chỉ cần **một phần kết quả** (ví dụ: `first()`, `take(n)`)
> - Khi muốn tránh **tạo nhiều list trung gian** → tiết kiệm bộ nhớ

Sequence có chi phí xử lý lazy, nên không luôn nhanh hơn List. Một số thao tác như `sorted()` vẫn cần giữ dữ liệu trước khi phát kết quả. Sequence không tự chạy background; phép biến đổi nặng trên Main vẫn có thể làm đứng UI. Với nguồn vô hạn, giới hạn số phần tử trước khi gom vào `toList()`.

---

## 13. Bảng tổng hợp — Chọn đúng function

| Mục đích | Function | Ví dụ |
|----------|----------|-------|
| Biến đổi từng phần tử | `map` | `list.map { it * 2 }` |
| Biến đổi + bỏ null | `mapNotNull` | `list.mapNotNull { it.toIntOrNull() }` |
| Gộp list lồng | `flatMap` / `flatten` | `orders.flatMap { it.items }` |
| Lọc theo điều kiện | `filter` | `list.filter { it > 5 }` |
| Lọc theo kiểu | `filterIsInstance` | `mixed.filterIsInstance<String>()` |
| Loại bỏ null | `filterNotNull` | `list.filterNotNull()` |
| Chia 2 nhóm | `partition` | `list.partition { it % 2 == 0 }` |
| Lấy N đầu/cuối | `take` / `takeLast` | `list.take(3)` |
| Bỏ N đầu/cuối | `drop` / `dropLast` | `list.drop(3)` |
| Loại trùng | `distinct` / `distinctBy` | `list.distinctBy { it.id }` |
| Kiểm tra bất kỳ | `any` | `list.any { it > 10 }` |
| Kiểm tra tất cả | `all` | `list.all { it > 0 }` |
| Kiểm tra không có | `none` | `list.none { it < 0 }` |
| Tổng / Trung bình | `sum` / `average` | `list.sum()` |
| Min / Max | `minOrNull` / `maxOrNull` | `list.minOrNull()` |
| Tích lũy (cùng kiểu) | `reduce` | `list.reduce { a, b -> a + b }` |
| Tích lũy (khác kiểu) | `fold` | `list.fold(0) { acc, v -> acc + v }` |
| Nhóm phần tử | `groupBy` | `list.groupBy { it.dept }` |
| List → Map | `associate` / `associateBy` | `users.associateBy { it.id }` |
| List → String | `joinToString` | `list.joinToString(", ")` |
| Sắp xếp | `sorted` / `sortedBy` | `list.sortedBy { it.name }` |
| Ghép cặp | `zip` | `names.zip(ages)` |
| Chia nhóm cố định | `chunked` | `list.chunked(3)` |
| Cửa sổ trượt | `windowed` | `list.windowed(3)` |

---

## 14. Ví dụ thực tế trong Android

### 14.1. Xử lý danh sách từ API

```kotlin
data class ApiUser(val id: Int, val name: String, val email: String?, val isActive: Boolean)
data class UserUiModel(val id: Int, val displayName: String, val email: String)

fun mapApiToUi(apiUsers: List<ApiUser>): List<UserUiModel> {
    return apiUsers
        .filter { it.isActive }                      // Chỉ lấy user active
        .filter { it.email != null }                 // Bỏ user không có email
        .distinctBy { it.email }                     // Giữ phần tử đầu theo email
        .sortedBy { it.name }                        // Sắp xếp theo tên gốc
        .mapNotNull { user ->                         // Bỏ email null và tạo UI model
            val email = user.email ?: return@mapNotNull null
            UserUiModel(
                id = user.id,
                displayName = user.name.uppercase(),
                email = email
            )
        }
}
```

### 14.2. Nhóm và hiển thị dữ liệu

```kotlin
data class Transaction(val category: String, val amount: Double, val date: String)

fun summarize(transactions: List<Transaction>): Map<String, Double> {
    return transactions
        .groupBy { it.category }                      // Nhóm theo danh mục
        .mapValues { (_, txns) -> txns.sumOf { it.amount } } // Tính tổng mỗi nhóm
}

// Kết quả: {"Ăn uống"=500.0, "Di chuyển"=200.0, "Giải trí"=150.0}
```

### 14.3. Tìm kiếm và phân trang

```kotlin
fun searchAndPaginate(
    items: List<String>,
    query: String,
    page: Int,
    pageSize: Int = 20
): List<String> {
    require(page >= 1 && pageSize > 0)
    val start = (page.toLong() - 1) * pageSize // Tránh tràn phép nhân Int
    val filtered = items.filter { it.contains(query, ignoreCase = true) }
    if (start >= filtered.size) return emptyList()
    return filtered.drop(start.toInt()).take(pageSize) // Lấy tối đa pageSize phần tử
}
```

### 14.4. Xử lý RecyclerView DiffUtil

```kotlin
data class Item(val id: Int, val title: String, val isSelected: Boolean)

fun toggleSelection(items: List<Item>, targetId: Int): List<Item> {
    return items.map { item ->
        if (item.id == targetId) item.copy(isSelected = !item.isSelected)
        else item
    }
}

fun getSelectedIds(items: List<Item>): List<Int> {
    return items
        .filter { it.isSelected }
        .map { it.id }
}
```

---

> [!NOTE]
> **Tóm tắt nguyên tắc chọn Collection:**
> - **List** → Khi cần **thứ tự** và cho phép **trùng lặp**
> - **Set** → Khi cần **loại bỏ trùng lặp** và kiểm tra tồn tại nhanh
> - **Map** → Khi cần **tra cứu nhanh** theo key
> - **Sequence** → Khi pipeline có thể hưởng lợi từ lazy/dừng sớm; đo hiệu năng nếu cần chọn giữa Sequence và List

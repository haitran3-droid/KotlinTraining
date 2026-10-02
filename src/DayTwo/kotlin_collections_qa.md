# 40 câu hỏi và trả lời về Kotlin Collections — Mức intern

Tài liệu đi kèm [bài giảng Collections](kotlin_collections.md). Mục tiêu: chọn đúng List/Set/Map, đoán được kết quả biến đổi và viết được xử lý danh sách cho ứng dụng.

**Cách học:** tự trả lời trước, đọc đáp án sau. Mỗi đoạn code là một ví dụ độc lập. Học A–D trước; học tổng hợp, Sequence và tình huống Android sau.

**Điểm cần nhớ:** `List`, `Set`, `Map` là interface **chỉ đọc** (read-only), không phải cam kết dữ liệu bất biến tuyệt đối. Sequence cũng không có mốc “hơn 10.000 phần tử là chắc chắn nhanh hơn”; hiệu quả phụ thuộc pipeline và dữ liệu.

## A. Chọn collection và hiểu mutability — Câu 1–7

### 1. Khi nào dùng List, Set hoặc Map?

**Trả lời:** chọn theo ý nghĩa dữ liệu và thao tác cần thực hiện.

| Nhu cầu | Chọn | Ví dụ |
|---|---|---|
| Có vị trí, có thứ tự, cho phép trùng | List | Các sản phẩm trên màn hình |
| Mỗi giá trị chỉ xuất hiện một lần | Set | ID đang được chọn |
| Tra cứu bằng một khóa | Map | User theo ID |

Không chọn Set nếu số lần xuất hiện mang ý nghĩa: giỏ hàng có hai món cùng mã không thể tự ý gộp thành một phần tử mà mất số lượng.

### 2. val có làm MutableList không thay đổi được không?

**Trả lời:** không. `val` ngăn gán biến sang tham chiếu khác; nó không cấm sửa đối tượng được tham chiếu.

```kotlin
val names = mutableListOf("An")
names.add("Bình")             // Được
// names = mutableListOf("Chi") // Không biên dịch
```

Muốn giới hạn API sửa danh sách, cung cấp kiểu `List` cho nơi chỉ cần đọc.

### 3. List chỉ đọc có đảm bảo dữ liệu bên trong không đổi không?

**Trả lời:** không. Một nơi khác có thể giữ tham chiếu mutable tới cùng đối tượng.

```kotlin
val editable = mutableListOf("An")
val readable: List<String> = editable
editable.add("Bình")
println(readable) // [An, Bình]
```

`readable` không có hàm `add`, nhưng vẫn nhìn thấy thay đổi. Đây là khác biệt giữa read-only và bất biến.

### 4. toList và toMutableList có sao chép mọi object bên trong không?

**Trả lời:** không. Chúng tạo kết quả tách về cấu trúc danh sách khi cần, nhưng không sao chép sâu các phần tử.

```kotlin
data class User(var name: String)
val original = mutableListOf(User("An"))
val copied = original.toList()

original.add(User("Bình"))
println(copied.size) // 1
original[0].name = "Chi"
println(copied[0].name) // Chi
```

Hai danh sách vẫn tham chiếu cùng `User` đầu tiên. Muốn tách phần tử, cần tạo object mới theo cấu trúc dữ liệu; `data class.copy()` cũng là sao chép nông nếu có thuộc tính chứa object mutable khác.

### 5. Truy cập list bằng index, first hay firstOrNull khác nhau thế nào?

**Trả lời:** `list[index]` cần index hợp lệ; `first()` cần có phần tử. Nếu không thỏa, chúng ném exception. `getOrNull(index)` và `firstOrNull()` trả null khi không tìm được phần tử.

```kotlin
val items = emptyList<String>()
println(items.firstOrNull()) // null
println(items.getOrNull(9))  // null
// items.first()            // Ném exception
```

Chọn hàm nullable khi dữ liệu rỗng hoặc thiếu là tình huống bình thường; xử lý fallback thay vì thêm `!!`.

### 6. remove(1) và removeAt(1) có xóa cùng phần tử không?

**Trả lời:** không nhất thiết. `remove(1)` xóa lần xuất hiện đầu của **giá trị 1**; `removeAt(1)` xóa phần tử ở **index 1**.

```kotlin
val a = mutableListOf(1, 7, 1)
a.remove(1)
println(a) // [7, 1]

val b = mutableListOf(1, 7, 1)
b.removeAt(1)
println(b) // [1, 1]
```

Index bắt đầu từ 0. Với danh sách số nguyên, hai lời gọi đặc biệt dễ bị hiểu nhầm.

### 7. sorted và sort có gì khác? reversed có phải sắp xếp giảm dần không?

**Trả lời:** `sorted()` trả danh sách kết quả mà không sắp xếp lại nguồn; `sort()` sửa thứ tự của MutableList hiện tại. `reversed()` chỉ đảo thứ tự hiện có.

```kotlin
val values = mutableListOf(3, 1, 2)
println(values.sorted())   // [1, 2, 3]
println(values)            // [3, 1, 2]
println(values.reversed()) // [2, 1, 3]
values.sort()
println(values)            // [1, 2, 3]
```

## B. Set và Map — Câu 8–13

### 8. Set xác định phần tử trùng như thế nào?

**Trả lời:** với các set dựa trên hash như HashSet, tính trùng dựa trên `equals` và `hashCode`. Data class sinh hai hàm này từ các thuộc tính trong constructor chính.

```kotlin
data class User(val id: Int, val name: String)
val users = setOf(User(1, "An"), User(1, "Bình"))
println(users.size) // 2: cùng ID nhưng name khác
```

Muốn loại trùng theo ID, dùng `distinctBy { it.id }` hoặc lưu ID trong Set. Với sorted set dùng comparator, tính trùng theo kết quả so sánh nên cần đọc quy tắc của implementation.

### 9. Set có đảm bảo thứ tự và contains luôn O(1) không?

**Trả lời:** không cho mọi implementation. LinkedHashSet giữ thứ tự chèn; HashSet không cam kết thứ tự; sorted set sắp theo quy tắc so sánh.

HashSet thường có chi phí `contains` trung bình O(1), không phải bảo đảm tuyệt đối cho mọi trường hợp hay mọi loại Set. Nếu UI cần thứ tự rõ ràng, chọn cấu trúc giữ thứ tự hoặc sắp xếp kết quả.

### 10. union, intersect và subtract dùng để làm gì?

```kotlin
val a = setOf(1, 2, 3)
val b = setOf(3, 4)
```

**Trả lời:** `a union b` gồm 1, 2, 3, 4; `a intersect b` gồm 3; `a subtract b` gồm 1, 2. Chúng trả kết quả, không xóa phần tử khỏi `a`.

Ví dụ: tập ID đã chọn và ID được phép dùng. Giao lấy ID vừa được chọn vừa hợp lệ; hiệu tìm ID đã chọn nhưng không còn hợp lệ. `a subtract b` khác `b subtract a`.

### 11. Map có cho trùng key hoặc trùng value không?

**Trả lời:** mỗi key có một value hiện tại; nhiều key có thể có value giống nhau. Gán lại cùng key cập nhật value của key đó.

```kotlin
val scores = mutableMapOf("An" to 8, "Bình" to 8)
scores["An"] = 9
println(scores.size)  // 2
println(scores["An"]) // 9
```

Nếu cần giữ nhiều giá trị cho một key, dùng `Map<Key, List<Value>>`, thường được tạo bằng `groupBy`.

### 12. map[key] trả null nghĩa là key chắc chắn không tồn tại?

**Trả lời:** chỉ kết luận như vậy nếu value của map không nullable. Với value nullable, null còn có thể là giá trị đã lưu.

```kotlin
val notes: Map<String, String?> = mapOf("An" to null)
println(notes["An"])             // null
println(notes["Bình"])           // null
println(notes.containsKey("An")) // true
```

Dùng `containsKey` để phân biệt hai trường hợp. `getValue(key)` ném exception khi key không có trong map thông thường; không nên dùng nếu thiếu key là tình huống bình thường.

### 13. keys, values và entries của Map khác nhau thế nào?

**Trả lời:** `keys` là tập khóa, `values` là các giá trị có thể trùng, `entries` chứa từng cặp key/value.

```kotlin
val scores = mapOf("An" to 8, "Bình" to 8)
println(scores.keys.size)   // 2
println(scores.values.size) // 2, không loại trùng điểm 8
scores.forEach { (name, score) -> println("$name: $score") }
```

`"An" in scores` kiểm tra key. Các collection từ `keys/values/entries` không nên mặc định là snapshot độc lập với map mutable nguồn.

## C. Biến đổi và nhóm dữ liệu — Câu 14–22

### 14. map và forEach khác nhau thế nào?

**Trả lời:** `map` tạo danh sách kết quả từ phép biến đổi từng phần tử; `forEach` thực hiện hành động và trả `Unit`.

```kotlin
val numbers = listOf(1, 2, 3)
val doubled = numbers.map { it * 2 } // [2, 4, 6]
numbers.forEach { println(it) }     // In từng số
```

Dùng `map` để đổi Entity sang UI model. Dùng `forEach` để log hoặc làm hành động khác. `map` không tự sao chép sâu hay ngăn lambda của bạn sửa object nguồn.

### 15. map và filter khác nhau thế nào?

**Trả lời:** `map` biến đổi mỗi phần tử; `filter` quyết định phần tử nào được giữ, không tự đổi kiểu phần tử.

```kotlin
val numbers = listOf(1, 2, 3, 4)
println(numbers.map { it > 2 })    // [false, false, true, true]
println(numbers.filter { it > 2 }) // [3, 4]
```

Muốn chỉ lấy tên user đang active: lọc user bằng `filter`, rồi lấy tên bằng `map`.

### 16. mapNotNull khác filterNotNull như thế nào?

**Trả lời:** `filterNotNull` loại null đã có trong dữ liệu. `mapNotNull` biến đổi phần tử rồi loại những kết quả null.

```kotlin
println(listOf(1, null, 2).filterNotNull()) // [1, 2]
println(listOf("1", "x", "2").mapNotNull { it.toIntOrNull() }) // [1, 2]
```

`mapNotNull` phù hợp với parse dữ liệu có thể thất bại. Nó loại dữ liệu lỗi khỏi kết quả; nếu cần báo lỗi từng dòng, nên giữ thông tin lỗi thay vì âm thầm bỏ hết.

### 17. map, flatMap và flatten khác nhau thế nào?

```kotlin
val sentences = listOf("a b", "c")
```

**Trả lời:** `sentences.map { it.split(" ") }` tạo `[[a, b], [c]]`. `flatMap` với cùng lambda tạo `[a, b, c]`. `flatten` dùng khi nguồn đã là danh sách lồng: `listOf(listOf("a", "b"), listOf("c")).flatten()`.

Ví dụ nhiều đơn hàng, mỗi đơn chứa nhiều sản phẩm: `orders.flatMap { it.items }` lấy sản phẩm từ mọi đơn thành một danh sách.

### 18. associateBy và groupBy khác nhau ở điểm nào quan trọng nhất?

**Trả lời:** `associateBy` giữ một phần tử cho mỗi key; nếu key trùng, phần tử cuối thắng. `groupBy` giữ danh sách mọi phần tử thuộc key đó.

```kotlin
val names = listOf("An", "Bình", "Chi")
println(names.associateBy { it.length }) // {2=An, 4=Bình, 3=Chi}
val repeated = listOf("An", "Vy")
println(repeated.associateBy { it.length }) // {2=Vy}
println(repeated.groupBy { it.length })     // {2=[An, Vy]}
```

Không dùng `associateBy` để nhóm đơn hàng theo khách nếu cần giữ tất cả đơn hàng.

### 19. associate, associateBy và associateWith chọn thế nào?

**Trả lời:** xem key và value bạn muốn tạo từ đâu.

```kotlin
val names = listOf("An", "Bình")
val a = names.associate { it.first() to it.length } // Key/value tùy chọn
val b = names.associateBy { it.first() }            // Value là phần tử gốc
val c = names.associateWith { it.length }           // Key là phần tử gốc
```

`a` là `Map<Char, Int>`, `b` là `Map<Char, String>`, `c` là `Map<String, Int>`. Mỗi kết quả vẫn chỉ giữ một value trên một key; chú ý dữ liệu key trùng.

### 20. zip hai danh sách khác độ dài có bị lỗi không?

**Trả lời:** không; kết quả có độ dài bằng danh sách ngắn hơn. Phần dư không được ghép.

```kotlin
val names = listOf("An", "Bình", "Chi")
val ages = listOf(20, 21)
println(names.zip(ages)) // [(An, 20), (Bình, 21)]
```

`unzip()` tách danh sách Pair thành hai danh sách. Nếu tên và tuổi bắt buộc đủ cặp, hãy kiểm tra độ dài trước; không coi `zip` là bước xác nhận dữ liệu hợp lệ.

### 21. chunked và windowed khác nhau thế nào?

**Trả lời:** `chunked` chia nhóm liên tiếp không chồng nhau; `windowed` tạo cửa sổ có thể chồng nhau.

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)
println(numbers.chunked(3))  // [[1, 2, 3], [4, 5]]
println(numbers.windowed(3)) // [[1, 2, 3], [2, 3, 4], [3, 4, 5]]
```

Mặc định `windowed(3)` không giữ cửa sổ cuối thiếu phần tử; có thể dùng `partialWindows = true` nếu cần. `chunked` phù hợp chia batch; `windowed` phù hợp tính trung bình trên cửa sổ trượt.

### 22. mapIndexed và zipWithNext thêm thông tin gì?

**Trả lời:** `mapIndexed` cho cả index và giá trị; `zipWithNext` ghép mỗi phần tử với phần tử liền sau.

```kotlin
println(listOf("An", "Bình").mapIndexed { index, name -> "${index + 1}. $name" })
// [1. An, 2. Bình]
println(listOf(10, 15, 12).zipWithNext { a, b -> b - a })
// [5, -3]
```

List có 0 hoặc 1 phần tử thì `zipWithNext` không tạo cặp nào. Index chỉ là vị trí hiện tại; không dùng thay ID ổn định của một item có thể đổi thứ tự.

## D. Lọc và kiểm tra — Câu 23–29

### 23. partition có khác gọi filter hai lần không?

**Trả lời:** nó trả cả nhóm thỏa điều kiện và nhóm không thỏa trong một lần duyệt nguồn.

```kotlin
val (passed, failed) = listOf(8, 4, 6).partition { it >= 5 }
println(passed) // [8, 6]
println(failed) // [4]
```

Dùng `filter` nếu chỉ cần một nhóm. Dùng `partition` nếu cần cả hai; thứ tự phần tử trong từng nhóm được giữ theo nguồn.

### 24. filter và takeWhile có luôn cho cùng kết quả không?

**Trả lời:** không. `filter` xét tất cả phần tử; `takeWhile` lấy liên tiếp từ đầu và dừng ngay khi điều kiện sai.

```kotlin
val numbers = listOf(1, 2, 7, 3)
println(numbers.filter { it < 5 })    // [1, 2, 3]
println(numbers.takeWhile { it < 5 }) // [1, 2]
println(numbers.dropWhile { it < 5 }) // [7, 3]
```

`dropWhile` bỏ phần đầu thỏa điều kiện, rồi giữ cả phần còn lại, kể cả phần tử sau đó lại thỏa điều kiện.

### 25. distinctBy và associateBy xử lý key trùng giống nhau không?

**Trả lời:** không. `distinctBy` giữ phần tử đầu tiên cho mỗi key và trả List; `associateBy` giữ phần tử cuối cho mỗi key và trả Map.

```kotlin
data class User(val id: Int, val name: String)
val users = listOf(User(1, "Cũ"), User(1, "Mới"))
println(users.distinctBy { it.id })    // [User(id=1, name=Cũ)]
println(users.associateBy { it.id }[1]) // User(id=1, name=Mới)
```

Muốn giữ bản mới nhất theo thời gian, cần quy tắc thời gian rõ ràng; “phần tử cuối trong list” không tự có nghĩa là “mới nhất”.

### 26. any, all và none trả gì với list rỗng?

**Trả lời:** `any { ... }` là false, `all { ... }` là true, `none { ... }` là true. Không có phần tử nào chứng minh điều kiện của `all` bị sai.

Nếu yêu cầu là “có ít nhất một học sinh và tất cả đều đậu”, viết `students.isNotEmpty() && students.all { it.score >= 5 }`. Không cần thêm `isNotEmpty` vào mọi trường hợp; thêm khi yêu cầu nghiệp vụ cần có dữ liệu.

### 27. Chỉ kiểm tra có phần tử thỏa điều kiện thì dùng filter hay any?

**Trả lời:** dùng `any { ... }`. Nó trả Boolean và có thể dừng ở phần tử đầu tiên thỏa điều kiện, không tạo danh sách kết quả như `filter`.

```kotlin
val hasNegative = listOf(2, -1, 3).any { it < 0 } // true
```

Nếu cần số lượng, dùng `count { ... }`; nếu cần các phần tử, dùng `filter`. Chọn theo kết quả thực sự cần.

### 28. filterIsInstance có giống ép kiểu cả list không?

**Trả lời:** không. Nó kiểm tra và giữ các phần tử phù hợp, bỏ các phần tử khác.

```kotlin
val mixed: List<Any?> = listOf("An", 1, null, "Bình")
val names: List<String> = mixed.filterIsInstance<String>()
println(names) // [An, Bình]
```

Nó không báo lỗi vì có số 1. Nếu yêu cầu dữ liệu phải toàn chuỗi, việc lọc bỏ phần tử không phải là xác nhận dữ liệu đầu vào hợp lệ. Với generic lồng như `List<String>`, cũng cần chú ý type erasure.

### 29. filterKeys, filterValues và filter trên Map chọn thế nào?

**Trả lời:** dùng `filterKeys` khi chỉ cần key, `filterValues` khi chỉ cần value, `filter` khi điều kiện liên quan cả hai.

```kotlin
val scores = mapOf("An" to 9, "Bình" to 4, "Chi" to 8)
val passed = scores.filterValues { it >= 5 } // {An=9, Chi=8}
val chosen = scores.filter { (name, score) -> name.length > 2 && score >= 5 }
// {Chi=8}
```

Chúng tạo kết quả lọc; map gốc không bị xóa key.

## E. Tổng hợp, sắp xếp và chuyển đổi — Câu 30–34

### 30. sumOf, count và average dùng khi nào? List rỗng thì sao?

**Trả lời:** `sumOf` tính tổng theo thuộc tính; `count` đếm phần tử; `average` tính trung bình dãy số.

```kotlin
data class Item(val quantity: Int, val price: Long)
val items = listOf(Item(2, 10), Item(1, 30))
val total = items.sumOf { it.quantity * it.price } // 50L
val count = items.count { it.quantity > 1 }        // 1
```

List số rỗng có tổng bằng 0 và count bằng 0, nhưng `average()` trả `NaN`. Khi hiển thị trung bình, xác định rõ UI cần gì nếu chưa có dữ liệu.

### 31. reduce và fold khác nhau thế nào với list rỗng?

**Trả lời:** `reduce` bắt đầu từ phần tử đầu, nên ném exception nếu rỗng. `fold` bắt đầu từ giá trị bạn cung cấp, nên trả giá trị đó nếu nguồn rỗng.

```kotlin
println(listOf(1, 2, 3).reduce { acc, value -> acc + value }) // 6
println(listOf(1, 2, 3).fold(10) { acc, value -> acc + value }) // 16
println(emptyList<Int>().fold(10) { acc, value -> acc + value }) // 10
```

`fold` cũng cho kết quả khác kiểu phần tử, ví dụ tạo chuỗi từ list số. Nếu chỉ tính tổng đơn giản, `sum()` thường dễ đọc hơn.

### 32. minOrNull, minByOrNull và runningFold trả gì?

**Trả lời:** `minOrNull` trả phần tử nhỏ nhất theo thứ tự tự nhiên; `minByOrNull` trả **object gốc** có tiêu chí nhỏ nhất; cả hai trả null khi rỗng. `runningFold` trả các kết quả tích lũy trung gian, gồm giá trị ban đầu.

```kotlin
println(listOf(3, 1, 2).minOrNull()) // 1
println(listOf(1, 2, 3).runningFold(0) { acc, value -> acc + value })
// [0, 1, 3, 6]
```

Với `products.minByOrNull { it.price }`, kết quả là một product, không phải riêng giá tiền. `runningReduce` tương tự nhưng bắt đầu từ phần tử đầu, không có giá trị ban đầu do caller truyền.

### 33. Sắp theo nhiều tiêu chí bằng cách nào?

**Trả lời:** dùng comparator với `compareBy` rồi `thenBy`; tiêu chí sau xử lý trường hợp bằng nhau ở tiêu chí trước.

```kotlin
data class Person(val name: String, val age: Int)
val people = listOf(Person("Bình", 20), Person("An", 20), Person("Chi", 19))
val sorted = people.sortedWith(compareBy<Person> { it.age }.thenBy { it.name })
// Chi(19), An(20), Bình(20)
```

`sortedBy { it.age }` chỉ biểu đạt tuổi. Đừng dựa vào thứ tự nguồn để ngầm đại diện quy tắc phụ như “cùng tuổi thì theo tên”.

### 34. toSet, toMap và joinToString có làm mất thông tin không?

**Trả lời:** có thể. `toSet()` loại trùng; `toMap()` từ danh sách Pair giữ value cuối nếu key trùng. `joinToString()` tạo chuỗi hiển thị, không phải định dạng lưu dữ liệu cấu trúc an toàn cho mọi trường hợp.

```kotlin
println(listOf(1, 1, 2).toSet()) // [1, 2]
println(listOf("a" to 1, "a" to 2).toMap()) // {a=2}
println(listOf("An", "Bình").joinToString(" | ")) // An | Bình
```

`joinToString(limit = 2)` chỉ giới hạn phần hiển thị, không sửa list. Nếu cần gửi dữ liệu cấu trúc tới API, dùng định dạng và serializer phù hợp như JSON.

## F. Sequence — Câu 35–37

### 35. Sequence lazy nghĩa là gì? Có tự chạy background không?

**Trả lời:** các bước như `filter/map` được trì hoãn đến khi kết quả được tiêu thụ bởi terminal operation như `toList`, `first`, `sum`. Sequence không tự chọn thread background.

```kotlin
val sequence = listOf(1, 2, 3).asSequence().map {
    println("Đổi $it")
    it * 2
}
// Chưa in gì ở trên
val result = sequence.toList() // In Đổi 1, Đổi 2, Đổi 3
```

Nếu gọi `toList()` trên Main và phép biến đổi rất nặng, Main vẫn có thể bị chặn. Lazy khác với bất đồng bộ.

### 36. Vì sao Sequence kết hợp take có thể làm ít việc hơn?

**Trả lời:** nó có thể xử lý từng phần tử qua pipeline và dừng khi lấy đủ kết quả, thay vì tạo đầy đủ danh sách ở mỗi bước.

```kotlin
val result = (1..10).asSequence()
    .filter { it % 2 == 0 }
    .map { it * it }
    .take(2)
    .toList()
// [4, 16]
```

Pipeline này chỉ cần xét đến số 4. Với phiên bản List eager, `filter` xét toàn nguồn trước khi `map` và `take` chạy. Nhưng thao tác như `sorted()` vẫn cần đọc toàn bộ nguồn trước khi đưa ra thứ tự sắp xếp.

### 37. Sequence có luôn nhanh hơn và ít tốn bộ nhớ hơn List không?

**Trả lời:** không. Sequence có chi phí điều phối từng bước; với nguồn nhỏ hoặc phép biến đổi đơn giản, List có thể phù hợp hơn. Lợi ích thường rõ hơn khi tránh nhiều danh sách trung gian hoặc có thể dừng sớm.

Không có mốc số phần tử bảo đảm Sequence thắng. Chọn cách rõ ràng trước, đo khi hiệu năng thật sự là vấn đề. Với nguồn vô hạn như `generateSequence`, cần giới hạn hợp lý trước `toList()`; nếu không, việc gom toàn bộ có thể không kết thúc.

## G. Áp dụng trong Android — Câu 38–40

### 38. Xử lý danh sách API thành UI model mà không dùng !! thế nào?

**Trả lời:** loại dữ liệu không hợp lệ và lấy một giá trị local không null trước khi tạo UI model.

```kotlin
data class ApiUser(val id: Int, val name: String, val email: String?, val active: Boolean)
data class UserUi(val id: Int, val name: String, val email: String)

fun toUi(users: List<ApiUser>): List<UserUi> = users
    .filter { it.active }
    .mapNotNull { user ->
        val email = user.email ?: return@mapNotNull null
        UserUi(user.id, user.name, email)
    }
    .distinctBy { it.email }
    .sortedBy { it.name }
```

Mẫu này loại email null, không tự loại email rỗng hay kiểm tra định dạng. `distinctBy` giữ bản đầu theo email; quy tắc coi email nào là trùng phải dựa vào yêu cầu sản phẩm.

### 39. Vì sao cập nhật item bằng map và copy hữu ích cho danh sách UI?

**Trả lời:** tạo danh sách mới và object mới cho item thay đổi giúp việc so sánh dữ liệu cũ/mới rõ ràng, phù hợp với ListAdapter/DiffUtil nếu comparator được cấu hình đúng.

```kotlin
data class Item(val id: Int, val selected: Boolean)
fun toggle(items: List<Item>, id: Int): List<Item> = items.map {
    if (it.id == id) it.copy(selected = !it.selected) else it
}
```

Tránh sửa cùng object mà cả danh sách cũ và mới đang giữ: nơi so sánh có thể không còn thấy trạng thái cũ. List mới không tự bảo đảm mọi object bên trong đều bất biến; bạn vẫn cần thiết kế model phù hợp.

### 40. Tìm kiếm, phân trang và tổng hợp theo nhóm nên viết thế nào?

**Trả lời:** xác định thứ tự các bước và điều kiện đầu vào trước.

```kotlin
fun page(items: List<String>, query: String, page: Int, pageSize: Int): List<String> {
    require(page >= 1 && pageSize > 0)
    val start = (page.toLong() - 1) * pageSize
    val filtered = items.filter { it.contains(query, ignoreCase = true) }
    if (start >= filtered.size) return emptyList()
    return filtered.drop(start.toInt()).take(pageSize)
}
```

Lọc trước rồi phân trang để trang được tính trong kết quả tìm kiếm. Dùng Long cho offset để tránh phép nhân Int tràn trong ví dụ. Nếu cần thứ tự ổn định, sắp xếp trước khi lấy trang.

Với tổng tiền theo danh mục, dùng `transactions.groupBy { it.category }.mapValues { (_, rows) -> rows.sumOf { it.amount } }`. `groupBy` gom hàng, `mapValues` tính kết quả từng nhóm. Đây là phân trang/tổng hợp dữ liệu đã ở bộ nhớ; dữ liệu rất lớn thường cần xử lý ở database hoặc server.

## Ôn tập và nguồn đối chiếu

Ưu tiên câu **2–4, 7, 12, 18, 24–26, 31 và 35–37**. Với mỗi pipeline, tự hỏi: kết quả là List, Set, Map hay Boolean; nguồn có bị sửa không; dữ liệu rỗng/trùng/null được xử lý thế nào?

Các chủ đề và ví dụ bám theo bài giảng gốc. Có thể đối chiếu API và các khác biệt nêu đầu bài tại:

- [Kotlin — Tổng quan collections, read-only và mutable](https://kotlinlang.org/docs/collections-overview.html).
- [Kotlin — Biến đổi collections](https://kotlinlang.org/docs/collection-transformations.html).
- [Kotlin — Lọc collections](https://kotlinlang.org/docs/collection-filtering.html).
- [Kotlin — Tổng hợp collections](https://kotlinlang.org/docs/collection-aggregate.html).
- [Kotlin — Sequence và đánh giá lazy](https://kotlinlang.org/docs/sequences.html).

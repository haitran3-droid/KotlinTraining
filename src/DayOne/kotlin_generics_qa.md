# 30 câu hỏi và trả lời về Kotlin Generics — Mức intern

Tài liệu đi kèm [bài giảng Generics](kotlin_generics.md). Học theo thứ tự: hiểu `T` → phân biệt `in/out` → projection và giới hạn kiểu → nhận biết các trường hợp runtime.

**Cách học:** tự trả lời trước khi đọc đáp án. Với code, đoán “biên dịch được không, vì sao?” trước. Mỗi ví dụ độc lập; dòng bị chú thích là dòng cố ý không biên dịch. Câu 26–29 là phần mở rộng, chỉ cần nhận biết trước.

**Điểm cần nhớ:** mặc định một class tự khai báo như `Box<T>` là invariant, nhưng `List` chỉ đọc của Kotlin đã khai báo `out T`. Vì vậy, `List<String>` **gán được** cho `List<Any>`; `MutableList<String>` không gán được cho `MutableList<Any>`.

## A. Hiểu generic và tham số kiểu — Câu 1–6

### 1. Generic giải quyết vấn đề gì?

**Trả lời:** cho phép dùng cùng một cấu trúc hoặc thuật toán với nhiều kiểu dữ liệu mà vẫn giữ thông tin kiểu để compiler kiểm tra.

```kotlin
class Box<T>(val value: T)

val numberBox = Box(10)
val nameBox = Box("An")
```

Không cần viết riêng `IntBox` và `StringBox`. Khi lấy `numberBox.value`, compiler biết đó là `Int`; không cần ép kiểu từ `Any`.

### 2. T là một biến chứa dữ liệu hay một kiểu dữ liệu?

**Trả lời:** `T` là tham số kiểu, đại diện cho kiểu được chọn khi sử dụng. Nó không phải biến chứa giá trị.

Trong `Box<String>`, `T` là `String`. Trong `Box<Int>`, `T` là `Int`. Tên `T` chỉ là quy ước; `Element`, `Key`, `Value` cũng dùng được. Chọn tên rõ hơn khi có nhiều tham số kiểu.

### 3. Vì sao không dùng Any thay cho T trong mọi trường hợp?

**Trả lời:** `Any` cho phép nhận nhiều kiểu nhưng không giữ mối liên hệ cụ thể giữa kiểu đầu vào và đầu ra.

```kotlin
fun identityAny(value: Any): Any = value
fun <T> identity(value: T): T = value

val a = identityAny("An") // Kiểu tĩnh: Any
val b = identity("An")    // Kiểu tĩnh: String
println(b.length)
// println(a.length)      // Không biên dịch
```

Dùng `Any` khi thực sự chỉ cần xử lý ở mức `Any`. Dùng generic khi cần bảo toàn hoặc ràng buộc mối quan hệ kiểu.

### 4. Khi nào cần viết Box<Int>, khi nào compiler tự hiểu?

**Trả lời:** compiler suy luận kiểu từ giá trị và kiểu mà ngữ cảnh yêu cầu.

```kotlin
class Box<T>(val value: T)
val a = Box(1)                  // Box<Int>
val b: Box<String> = Box("An")  // T được suy luận là String
val c = emptyList<String>()     // Cần cung cấp kiểu khi thiếu ngữ cảnh
val d: List<String> = emptyList()
```

Nếu không có phần tử và không có kiểu yêu cầu, compiler có thể không đủ thông tin để suy luận. Chỉ định kiểu để giải quyết sự thiếu thông tin đó.

### 5. Generic class và generic function khác nhau thế nào?

**Trả lời:** tham số kiểu của class dùng cho một đối tượng; tham số kiểu của hàm được chọn cho mỗi lần gọi hàm.

```kotlin
class Box<T>(val value: T)
fun <T> wrap(value: T): List<T> = listOf(value)

val numbers = wrap(1)     // List<Int>
val names = wrap("An")   // List<String>
```

Một `Box<Int>` không đổi thành `Box<String>` sau khi tạo. Cùng hàm `wrap` có thể được gọi với nhiều kiểu khác nhau.

### 6. Generic interface hữu ích trong Repository như thế nào?

**Trả lời:** nó định nghĩa hợp đồng chung nhưng mỗi implementation giữ một kiểu dữ liệu cụ thể.

```kotlin
interface Repository<T> {
    fun getById(id: Int): T?
    fun save(item: T)
}

data class User(val id: Int, val name: String)

class UserRepository : Repository<User> {
    private val users = mutableMapOf<Int, User>()
    override fun getById(id: Int): User? = users[id]
    override fun save(item: User) { users[item.id] = item }
}
```

`save` trong `UserRepository` nhận `User`; không thể truyền một `String` chỉ vì interface ban đầu dùng `T`.

## B. Invariant, out và in — Câu 7–15

### 7. String là Any, vậy Box<String> có phải Box<Any> không?

**Trả lời:** không mặc định. `Box<T>` tự khai báo không có variance modifier là invariant.

```kotlin
class Box<T>(var value: T)
val strings = Box("An")
// val objects: Box<Any> = strings // Không biên dịch
```

Nếu được gán, `objects.value = 123` sẽ đưa `Int` vào hộp vốn chứa `String`. Compiler ngăn việc đó để bảo vệ tính an toàn kiểu.

### 8. Vì sao List<String> gán được cho List<Any>?

**Trả lời:** `List` chỉ đọc của Kotlin khai báo tham số kiểu là `out T`, nên có covariance.

```kotlin
val names: List<String> = listOf("An")
val objects: List<Any> = names // Biên dịch được
println(objects[0])           // An
```

Mọi `String` đều dùng được như `Any`, và interface `List` không cho thêm `Int` vào danh sách. Đây là điểm cần phân biệt với generic invariant trong câu 7.

### 9. Vì sao MutableList<String> không gán được cho MutableList<Any>?

**Trả lời:** `MutableList` vừa nhận phần tử mới vừa trả phần tử, nên không thể cho phép chuyển kiểu theo cách đó.

Nếu một `MutableList<String>` được nhìn như `MutableList<Any>`, nơi nhận có thể thêm số 1. Sau đó, nơi ban đầu đọc phần tử và tin rằng đó là `String` sẽ gặp lỗi. Compiler chặn phép gán ngay từ đầu.

### 10. out nghĩa là gì? Chiều gán ra sao?

**Trả lời:** ở trường hợp cơ bản, `out T` phù hợp với nguồn cung cấp giá trị `T`. Cho phép nhìn nguồn của kiểu con như nguồn của kiểu cha.

```kotlin
interface Source<out T> {
    fun get(): T
}

fun read(source: Source<String>) {
    val general: Source<Any> = source
    val value: Any = general.get()
    println(value)
}
```

Nguồn tạo `String` chắc chắn cũng tạo giá trị dùng được như `Any`. Chiều gán: `Source<String>` → `Source<Any>`.

### 11. Vì sao không khai báo fun save(value: T) trong Source<out T>?

**Trả lời:** tham số trực tiếp `value: T` là vị trí nhận giá trị. Nếu cho phép, người dùng nhìn `Source<String>` như `Source<Any>` rồi truyền một `Int` vào `save` sẽ phá an toàn kiểu.

```kotlin
interface Source<out T> {
    fun get(): T
    // fun save(value: T) // Không biên dịch
}
```

Ở mức intern, dùng ví dụ đầu vào/đầu ra trực tiếp để hiểu. Với kiểu hàm lồng nhau, vị trí variance có quy tắc chi tiết hơn; không đoán chỉ từ tên method.

### 12. in nghĩa là gì? Vì sao chiều gán ngược với out?

**Trả lời:** `in T` phù hợp với đối tượng nhận giá trị `T`. Một đối tượng nhận được mọi `Any` chắc chắn nhận được `String`.

```kotlin
interface Sink<in T> {
    fun accept(value: T)
}

fun use(sink: Sink<Any>) {
    val stringSink: Sink<String> = sink
    stringSink.accept("An")
}
```

Chiều gán: `Sink<Any>` → `Sink<String>`. Không đi chiều ngược lại: nơi chỉ biết nhận `String` không thể nhận mọi `Any`.

### 13. Sink<in T> có được trả về T không?

**Trả lời:** không được trả trực tiếp `T` ở một method công khai như `fun get(): T`.

Nếu nhìn `Sink<Any>` như `Sink<String>`, caller sẽ nghĩ `get()` trả `String` trong khi thực tế có thể trả `Int`. Nhưng trả `Unit`, `Boolean`, `Int` để báo trạng thái vẫn được, vì chúng không phải tham số kiểu `T`.

### 14. Class vừa đọc vừa ghi thì chọn in hay out?

**Trả lời:** thường giữ `T` invariant. Đừng thêm `in/out` chỉ để phép gán mong muốn biên dịch được.

```kotlin
class Storage<T>(private var value: T) {
    fun get(): T = value
    fun set(newValue: T) { value = newValue }
}
```

Nếu API cần linh hoạt hơn, có thể tách interface chỉ đọc và chỉ ghi, hoặc dùng projection ở nơi nhận như câu 16–18.

### 15. out có làm đối tượng bất biến không?

**Trả lời:** không. Variance quy định an toàn khi dùng các kiểu khác nhau; nó không đảm bảo đối tượng không thay đổi.

Một `MutableList<String>` có thể được đưa ra ngoài dưới dạng `List<String>`, nhưng nơi giữ tham chiếu mutable vẫn sửa được danh sách. `out` không đóng băng dữ liệu và không tự sao chép đối tượng.

## C. Projection, dấu * và constraints — Câu 16–23

### 16. Declaration-site variance và use-site projection khác nhau thế nào?

**Trả lời:** declaration-site đặt quy tắc trên kiểu khi khai báo, như `Source<out T>`. Use-site giới hạn cách sử dụng ở một nơi cụ thể, như tham số `Array<out Number>`.

`Array<T>` vốn đọc và ghi được. Hàm chỉ cần đọc có thể nhận `Array<out Number>` để chấp nhận `Array<Int>` mà không được ghi một giá trị `Number` tùy ý vào đó.

### 17. Array<out Number> cho đọc và ghi như thế nào?

```kotlin
fun total(values: Array<out Number>): Double {
    // values[0] = 3.14 // Không biên dịch
    return values.sumOf { it.toDouble() }
}
```

**Trả lời:** đọc phần tử như `Number` được; gán phần tử không được. Mảng thực tế có thể là `Array<Int>`, nên ghi `Double` vào đó không an toàn. `total(arrayOf(1, 2))` trả `3.0`.

### 18. Array<in String> có thật sự chỉ ghi, hoàn toàn không đọc được không?

**Trả lời:** vẫn đọc được, nhưng chỉ biết kết quả là `Any?`, không thể mặc định là `String`.

```kotlin
fun putName(values: Array<in String>) {
    values[0] = "An"        // Ghi String được
    val value: Any? = values[0]
    println(value)
}
```

Giả sử mảng không rỗng. Mảng được truyền vào có thể là `Array<Any>` vốn chứa cả số. Vì vậy, các phần tử khác chưa chắc là chuỗi.

### 19. List<*> khác List<Any?> thế nào?

**Trả lời:** `List<*>` diễn đạt “không biết kiểu phần tử”; `List<Any?>` diễn đạt phần tử được nhìn ở mức `Any?`. Với `List` chỉ đọc, cả hai đều đọc ra `Any?`, và `List<String>` dùng được ở cả hai nơi.

Khác biệt rõ hơn với mutable generic: `MutableList<Any?>` cho thêm số, chuỗi hoặc null; `MutableList<*>` không cho thêm phần tử vì không biết kiểu thực tế.

### 20. Có thêm null vào MutableList<*> được không?

**Trả lời:** không. Kiểu thực tế có thể là `MutableList<String>` không cho null, hoặc một kiểu khác bạn không biết.

```kotlin
fun inspect(values: MutableList<*>) {
    println(values.firstOrNull()) // Đọc như Any?
    // values.add(null)           // Không biên dịch
    values.clear()               // Được: không đưa giá trị T mới vào
}
```

Projection không có nghĩa mọi thao tác thay đổi đều bị cấm. `clear()` không nhận một giá trị `T`, nên vẫn dùng được.

### 21. Vì sao cần upper bound như T : Number?

**Trả lời:** để chỉ nhận các kiểu phù hợp và cho phép code dùng khả năng của kiểu giới hạn đó.

```kotlin
fun <T : Number> twice(value: T): Double = value.toDouble() * 2

println(twice(3))    // 6.0
println(twice(2.5))  // 5.0
// twice("3")       // Không biên dịch
```

Bound này không khiến kết quả tự mang kiểu `T`; hàm đã khai báo kết quả là `Double`. Cũng không thể mặc định dùng mọi toán tử số cho một `T : Number`.

### 22. T không có bound có nhận null được không? T : Any thì sao?

**Trả lời:** bound mặc định là `Any?`, nên có thể chọn một kiểu nullable. `T : Any` yêu cầu kiểu không null.

```kotlin
fun <T> keep(value: T): T = value
fun <T : Any> keepNonNull(value: T): T = value

val name = keep<String?>(null) // Được
// keepNonNull<String?>(null) // Không biên dịch
```

Không thể dùng một thành viên chỉ có ở `String` như `.length` trên `T` không có bound phù hợp.

### 23. where dùng khi nào? Có cần dùng cho mọi hàm generic không?

**Trả lời:** không. `where` hữu ích khi một tham số kiểu phải thỏa nhiều giới hạn cùng lúc.

```kotlin
fun <T> longerAndGreater(value: T, other: T): Boolean
    where T : CharSequence, T : Comparable<T> {
    return value.length > other.length && value > other
}
```

`CharSequence` cho dùng `.length`; `Comparable<T>` cho so sánh `>`. Một `String` thỏa cả hai. Không có giới hạn cần thiết thì compiler không thể biết `T` có các khả năng đó.

## D. Runtime và phần mở rộng — Câu 24–29

### 24. Type erasure nghĩa là generic không còn tác dụng sao?

**Trả lời:** không. Compiler vẫn kiểm tra generic khi biên dịch. Trên JVM, runtime thường không biết đầy đủ type argument của một đối tượng generic để kiểm tra như `is List<String>`.

Một đối tượng danh sách vẫn là danh sách, và từng phần tử vẫn có kiểu runtime riêng. Điều bị thiếu là sự đảm bảo rằng toàn bộ danh sách có type argument `String` chỉ từ kiểm tra kiểu danh sách.

### 25. Vì sao is List<*> được nhưng is List<String> thường không được?

**Trả lời:** từ một giá trị `Any`, runtime có thể kiểm tra nó có phải danh sách không, nhưng không thể dùng kiểm tra đó để chứng minh mọi phần tử là `String`.

```kotlin
fun inspect(value: Any) {
    if (value is List<*>) println(value.size)
    // if (value is List<String>) { ... } // Không biên dịch
}
```

Muốn xác nhận phần tử, kiểm tra từng phần tử. `filterIsInstance<String>()` chỉ lấy các chuỗi và bỏ phần tử khác; nó không chứng minh danh sách đầu vào toàn chuỗi.

### 26. reified giải quyết điều gì và vì sao đi cùng inline?

**Trả lời:** trong hàm inline, `reified T` cho phép dùng kiểu mà caller chọn trong các thao tác như `value is T`.

```kotlin
inline fun <reified T> isType(value: Any?): Boolean = value is T

println(isType<String>("An")) // true
println(isType<Int>("An"))    // false
```

Compiler triển khai hàm inline tại nơi gọi và có thể dùng kiểu cụ thể ở đó. Một hàm generic thông thường không có `reified` không được kiểm tra `value is T` theo cách này.

### 27. reified có kiểm tra hết kiểu phần tử của List<String> không?

**Trả lời:** không tự động. Nó không khôi phục toàn bộ generic lồng bên trong một danh sách runtime.

Với hàm `isType` ở câu 26, `isType<List<String>>(listOf(1, 2))` vẫn có thể trả `true` vì kiểm tra phần danh sách, không xác nhận từng phần tử là chuỗi. Cần kiểm tra dữ liệu bên trong nếu đó là yêu cầu.

Vì vậy, không dùng `reified` như lời đảm bảo rằng một phép cast generic bất kỳ đều an toàn.

### 28. T & Any khác T : Any thế nào?

**Trả lời:** `T : Any` yêu cầu toàn bộ type argument `T` là không null. `T & Any` thể hiện một giá trị của `T` chắc chắn không null tại vị trí đó, ngay cả khi `T` có thể có bound nullable.

Nó thường xuất hiện khi override API Java có annotation không null, như ví dụ `Game<T>` trong bài gốc. Ở mức intern, chỉ cần nhận diện khi đọc code interop; không cần thêm nó vào mọi hàm generic.

### 29. Dấu _ trong type argument có giống dấu * không?

**Trả lời:** không. `_` ở lời gọi hàm bảo compiler suy luận một type argument cụ thể. `*` trong kiểu generic diễn đạt một type argument không biết.

```kotlin
fun <A, B> pairOf(first: A, second: B): Pair<A, B> = first to second

val result = pairOf<String, _>("An", 22) // B được suy luận là Int
val unknown: List<*> = listOf("An")
```

`result.second` có kiểu `Int`; phần tử lấy từ `unknown` chỉ biết là `Any?`.

## E. Áp dụng — Câu 30

### 30. Gặp một API generic mới thì đọc theo thứ tự nào?

**Trả lời:** tìm mối quan hệ giữa các kiểu trước khi nhìn tên modifier.

1. `T` đại diện dữ liệu gì? Ví dụ user, item hay kết quả.
2. API nhận `T`, trả `T`, hay cả hai? Đó là gợi ý chọn `in`, `out` hoặc invariant.
3. Có bound nào không? Bound cho biết code được phép dùng khả năng nào của `T`.
4. Kiểu đã được xác định hay đang là `*`? Điều này ảnh hưởng giá trị đọc/ghi được.
5. Code đang kiểm tra lúc compile-time hay runtime? Cast generic không thay thế việc kiểm tra dữ liệu.

**Tự thử:** viết một `Source<out T>` chỉ có `get()`, một `Sink<in T>` chỉ có `accept()`, rồi giải thích hai chiều gán ở câu 10 và 12 bằng ví dụ `String`/`Any` của bạn.

## Ôn tập và đối chiếu

Ưu tiên câu **3, 7–12, 18–22, 25 và 27**. Phần `T & Any` và `_` có thể học sau khi bạn đã chắc `T`, `in/out` và bounds.

Khi làm bài, luôn phân biệt **kiểu mà compiler nhìn thấy** với **đối tượng thực tế đang được tham chiếu**. Phép gán đổi cách nhìn đối tượng, không tự tạo bản sao hay biến đổi các phần tử bên trong.

Đối chiếu thêm các giới hạn và cú pháp tại [Kotlin — Generics](https://kotlinlang.org/docs/generics.html).

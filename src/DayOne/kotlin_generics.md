# Kotlin Generics: `in`, `out`, `where`

> [!NOTE]
> Tài liệu tham khảo: [Kotlin Official Docs - Generics](https://kotlinlang.org/docs/generics.html)

---

## 1. Khái niệm Generic là gì?

**Generic** (kiểu tổng quát) cho phép bạn viết class, interface, hoặc function mà **không cần xác định kiểu dữ liệu cụ thể** ngay lúc khai báo. Kiểu dữ liệu sẽ được chỉ định sau khi sử dụng.

**Tại sao cần Generic?**
- ✅ **Type Safety** — Compiler kiểm tra kiểu tại compile-time, tránh `ClassCastException` lúc runtime
- ✅ **Tái sử dụng code** — Viết 1 lần, dùng cho nhiều kiểu dữ liệu
- ✅ **Loại bỏ ép kiểu thủ công** — Không cần cast khi lấy phần tử ra

**Ví dụ đơn giản — Không dùng Generic:**
```kotlin
class IntBox(val value: Int)
class StringBox(val value: String)
// Phải viết riêng cho mỗi kiểu → lặp code!
```

**Dùng Generic:**
```kotlin
class Box<T>(val value: T)

val intBox = Box(42)          // Box<Int>
val strBox = Box("Hello")     // Box<String>
```

> `T` là **type parameter** (tham số kiểu) — đại diện cho một kiểu dữ liệu bất kỳ.

---

## 2. Cách khai báo

### 2.1 Generic Class

```kotlin
class Box<T>(t: T) {
    var value = t
}

// Tạo instance — chỉ định kiểu rõ ràng
val box: Box<Int> = Box<Int>(1)

// Hoặc để compiler tự suy luận (type inference)
val box = Box(1)  // Compiler hiểu đây là Box<Int>
```

### 2.2 Generic Interface

```kotlin
interface Repository<T> {
    fun getById(id: Int): T
    fun getAll(): List<T>
    fun save(item: T)
}

class UserRepository : Repository<User> {
    override fun getById(id: Int): User { ... }
    override fun getAll(): List<User> { ... }
    override fun save(item: User) { ... }
}
```

### 2.3 Generic Function

Type parameter đặt **trước tên hàm**:

```kotlin
fun <T> singletonList(item: T): List<T> {
    return listOf(item)
}

// Extension function cũng có thể generic
fun <T> T.basicToString(): String {
    return this.toString()
}
```

Gọi hàm generic:
```kotlin
val l = singletonList<Int>(1)   // Chỉ định kiểu rõ ràng
val l = singletonList(1)        // Type inference — compiler tự hiểu T = Int
```

---

## 3. Variance — `out` và `in` (Phần quan trọng nhất!)

### 3.1 Vấn đề: Invariance (Bất biến)

Mặc định, generic trong Kotlin (và Java) là **invariant** — nghĩa là `List<String>` **KHÔNG phải** subtype của `List<Any>`, dù `String` là subtype của `Any`.

```kotlin
val strs: List<String> = listOf("a", "b")
val objs: List<Any> = strs  // ❌ Lỗi nếu List là invariant (mutable)
```

Tại sao? Vì nếu cho phép, ta có thể:
```kotlin
val strs: MutableList<String> = mutableListOf("a")
val objs: MutableList<Any> = strs  // Giả sử được phép
objs.add(42)                        // Thêm Int vào list String!
val s: String = strs[1]             // 💥 ClassCastException!
```

### 3.2 `out` — Covariance (Hiệp biến) — **Producer**

> **Quy tắc:** `T` chỉ được dùng ở vị trí **output** (trả về), KHÔNG được dùng làm input.

```kotlin
interface Source<out T> {
    fun nextT(): T       // ✅ T ở vị trí trả về — OK
    // fun consume(t: T) // ❌ Compile error — T không được ở vị trí tham số
}
```

**Khi dùng `out`:** `Source<String>` trở thành subtype của `Source<Any>`

```kotlin
fun demo(strs: Source<String>) {
    val objects: Source<Any> = strs  // ✅ OK vì T là out-parameter
}
```

**Cách nhớ:** `out` = chỉ **lấy ra** (produce/read) → an toàn để gán subtype cho supertype

**Ví dụ thực tế:** `List<out T>` trong Kotlin standard library là covariant → `List<String>` gán được cho `List<Any>`.

### 3.3 `in` — Contravariance (Nghịch biến) — **Consumer**

> **Quy tắc:** `T` chỉ được dùng ở vị trí **input** (tham số), KHÔNG được dùng làm output.

```kotlin
interface Comparable<in T> {
    operator fun compareTo(other: T): Int  // ✅ T ở vị trí tham số — OK
    // fun get(): T                         // ❌ Compile error — T không được ở vị trí trả về
}
```

**Khi dùng `in`:** `Comparable<Number>` trở thành subtype của `Comparable<Double>`

```kotlin
fun demo(x: Comparable<Number>) {
    x.compareTo(1.0)  // OK — Double là subtype của Number
    val y: Comparable<Double> = x  // ✅ OK!
}
```

**Cách nhớ:** `in` = chỉ **đưa vào** (consume/write) → chiều subtype **đảo ngược**

### 3.4 Bảng tóm tắt `in` / `out`

| Modifier | Tên gọi | Vai trò | Chiều subtype | Java tương đương |
|----------|---------|---------|---------------|-----------------|
| `out T` | Covariant | **Producer** — chỉ trả về `T` | `C<Sub>` → `C<Super>` | `? extends T` |
| `in T` | Contravariant | **Consumer** — chỉ nhận `T` | `C<Super>` → `C<Sub>` | `? super T` |
| _(không có)_ | Invariant | Cả đọc và ghi | Không chuyển đổi | `T` |

> [!TIP]
> Quy tắc vàng: **Consumer `in`, Producer `out`!** (CIPO)
> Tương đương với PECS trong Java: Producer-Extends, Consumer-Super.

---

## 4. Type Projection (Use-site Variance)

Khi một class **không thể** khai báo `out` hay `in` ở declaration-site (vì dùng `T` ở cả 2 vị trí), ta dùng **type projection** tại nơi sử dụng:

```kotlin
// Array<T> vừa đọc vừa ghi → không thể dùng out/in ở declaration-site
class Array<T>(val size: Int) {
    operator fun get(index: Int): T { ... }
    operator fun set(index: Int, value: T) { ... }
}
```

### `out` projection — Chỉ cho phép đọc:
```kotlin
fun copy(from: Array<out Any>, to: Array<Any>) {
    for (i in from.indices)
        to[i] = from[i]  // Đọc từ 'from' OK, ghi vào 'to' OK
}

val ints: Array<Int> = arrayOf(1, 2, 3)
val any = Array<Any>(3) { "" }
copy(ints, any)  // ✅ OK! Array<Int> match được Array<out Any>
```

### `in` projection — Chỉ cho phép ghi:
```kotlin
fun fill(dest: Array<in String>, value: String) {
    // Có thể ghi String vào dest
    // Nhưng đọc ra chỉ được Any?
}
```

---

## 5. Star Projection (`*`)

Khi bạn **không biết** hoặc **không quan tâm** type argument cụ thể:

```kotlin
val list: List<*>  // Tương tự Java raw type, nhưng an toàn hơn
```

| Khai báo | `Foo<*>` tương đương |
|----------|---------------------|
| `Foo<out T : TUpper>` | `Foo<out TUpper>` — đọc được `TUpper` |
| `Foo<in T>` | `Foo<in Nothing>` — không ghi được gì |
| `Foo<T : TUpper>` | Đọc: `Foo<out TUpper>`, Ghi: `Foo<in Nothing>` |

```kotlin
// Ví dụ: Kiểm tra kiểu tại runtime
if (something is List<*>) {
    something.forEach { println(it) }  // it có kiểu Any?
}
```

---

## 6. Generic Constraints — Giới hạn kiểu

### 6.1 Upper Bound (Giới hạn trên)

Dùng dấu `:` để chỉ định `T` phải là subtype của một kiểu nào đó:

```kotlin
fun <T : Comparable<T>> sort(list: List<T>) { ... }

sort(listOf(1, 2, 3))          // ✅ OK — Int implements Comparable<Int>
sort(listOf(HashMap<Int, String>())) // ❌ Error — HashMap không implement Comparable
```

> Nếu không chỉ định upper bound, mặc định là `Any?`

### 6.2 Nhiều Upper Bound — `where` clause

Khi `T` cần thỏa mãn **nhiều điều kiện** cùng lúc:

```kotlin
fun <T> copyWhenGreater(list: List<T>, threshold: T): List<String>
    where T : CharSequence,
          T : Comparable<T> {
    return list.filter { it > threshold }.map { it.toString() }
}
// T phải vừa là CharSequence, vừa implement Comparable
```

---

## 7. Type Erasure — Xóa kiểu tại Runtime

> [!WARNING]
> Giống Java, Kotlin sử dụng **type erasure** — thông tin generic bị xóa tại runtime.
> `Foo<Bar>` và `Foo<Baz?>` đều trở thành `Foo<*>` lúc runtime.

### Hệ quả:
```kotlin
// ❌ Không thể kiểm tra type argument tại runtime
if (list is List<String>) { ... }  // Compile error!

// ✅ Chỉ kiểm tra được star-projection
if (list is List<*>) { ... }       // OK
```

### Giải pháp: `reified` type parameter (chỉ dùng với `inline` function)

```kotlin
inline fun <reified T> isOfType(value: Any): Boolean {
    return value is T  // ✅ OK nhờ reified — T được giữ lại tại runtime
}

println(isOfType<String>("hello"))  // true
println(isOfType<Int>("hello"))     // false
```

---

## 8. Definitely Non-Nullable Types — `T & Any`

Dùng khi interop với Java và cần đảm bảo `T` **không null**:

```kotlin
// Java interface
public interface Game<T> {
    @NotNull T load(@NotNull T x);
}

// Kotlin override
interface ArcadeGame<T1> : Game<T1> {
    override fun load(x: T1 & Any): T1 & Any  // Đảm bảo non-null
}
```

---

## 9. Underscore Operator `_` cho Type Arguments

Khi có thể suy luận một phần type arguments:

```kotlin
abstract class SomeClass<T> {
    abstract fun execute(): T
}
class SomeImplementation : SomeClass<String>() {
    override fun execute(): String = "Test"
}

object Runner {
    inline fun <reified S : SomeClass<T>, T> run(): T {
        return S::class.java.getDeclaredConstructor().newInstance().execute()
    }
}

fun main() {
    // T tự suy luận là String từ SomeImplementation
    val s = Runner.run<SomeImplementation, _>()
}
```

---

## 10. Tổng kết các trường hợp sử dụng

```mermaid
graph TD
    A["Kotlin Generic"] --> B["Generic Class/Interface"]
    A --> C["Generic Function"]
    A --> D["Variance"]
    A --> E["Constraints"]
    A --> F["Type Erasure"]
    
    B --> B1["class Box&lt;T&gt;"]
    B --> B2["interface Repo&lt;T&gt;"]
    
    C --> C1["fun &lt;T&gt; sort(list)"]
    C --> C2["fun &lt;T&gt; T.extend()"]
    
    D --> D1["out T — Covariant — Producer"]
    D --> D2["in T — Contravariant — Consumer"]
    D --> D3["Type Projection — use-site"]
    D --> D4["Star Projection — *"]
    
    E --> E1["Upper Bound — T : Type"]
    E --> E2["where clause — multiple bounds"]
    
    F --> F1["reified — giữ kiểu tại runtime"]
    F --> F2["Star projection check"]
```

### Bảng Quick Reference

| Tình huống | Sử dụng | Ví dụ |
|------------|---------|-------|
| Class dùng cho nhiều kiểu | Generic class | `class Box<T>(val value: T)` |
| Hàm dùng cho nhiều kiểu | Generic function | `fun <T> listOf(item: T): List<T>` |
| Chỉ đọc/produce giá trị | `out` (covariant) | `interface Source<out T>` |
| Chỉ ghi/consume giá trị | `in` (contravariant) | `interface Sink<in T>` |
| Vừa đọc vừa ghi, nhưng muốn linh hoạt tại nơi dùng | Type projection | `Array<out Any>`, `Array<in String>` |
| Không biết/quan tâm kiểu | Star projection | `List<*>` |
| Giới hạn kiểu được phép | Upper bound | `<T : Comparable<T>>` |
| Nhiều giới hạn cùng lúc | `where` | `where T : A, T : B` |
| Cần kiểm tra kiểu tại runtime | `reified` + `inline` | `inline fun <reified T> check()` |

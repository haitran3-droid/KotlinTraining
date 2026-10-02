# Kotlin Coroutines cơ bản — Dành cho intern Android

Mục tiêu: hiểu coroutine dùng để làm gì, chọn đúng scope và viết được các tác vụ thường gặp trong Android. Chỉ cần biết Kotlin cơ bản, lambda, Activity/Fragment và ViewModel.

**Cách học:** đọc mục 1–4 trước, sau đó học Flow ở mục 5. Mục 6 dùng khi gặp thư viện callback; mục 7 ghép Retrofit và Room vào một ví dụ thực tế. Chưa cần học sâu về CoroutineContext, buffer hay kiểm thử thời gian giả lập.

**Quy ước:** mỗi đoạn code minh họa một ý, không ghép toàn bộ thành một file. Ví dụ Android cần project Android, thư viện Coroutines Android, AndroidX Lifecycle, Retrofit với JSON converter và Room với bộ xử lý annotation của Room. Dùng phiên bản tương thích của project; IDE có thể tự thêm import. Các hàm như `render()` và `showToast()` đại diện cho code UI bạn tự viết.

## 1. Coroutine và suspend là gì?

Khi gọi API, ứng dụng phải chờ server trả dữ liệu. Coroutine giúp viết việc chờ này theo thứ tự dễ đọc mà không giữ thread đứng chờ.

```kotlin
suspend fun getName(): String {
    delay(1_000) // Giả lập chờ 1 giây, không chặn thread
    return "An"
}

// Trong một ViewModel:
fun loadName() {
    viewModelScope.launch {
        val name = getName() // Chờ kết quả tại đây
        println(name)       // Sau đó mới in An
    }
}
```

- **Coroutine:** tác vụ có thể tạm dừng rồi tiếp tục; không phải một thread riêng.
- **`suspend`:** cho phép hàm tạm dừng. Gọi hàm này từ một hàm `suspend` khác hoặc trong coroutine như `launch`.
- **`delay()`:** tạm dừng coroutine, thread có thể làm việc khác.
- **`Thread.sleep()`:** chặn thread; nếu chạy trên Main thì UI bị đứng.

**Cần nhớ:** thêm `suspend` không tự đưa code sang background. Coroutine cũng không tự biến mọi thao tác nặng thành thao tác không chặn thread.

Nguồn: [Kotlin — Coroutines basics](https://kotlinlang.org/docs/coroutines-basics.html).

## 2. Scope — Ai quản lý tác vụ?

Hãy xem scope như một nhóm tác vụ có người quản lý. Bạn gọi `scope.launch { ... }` để thêm tác vụ vào nhóm. Khi scope bị hủy, các coroutine con nhận yêu cầu hủy theo.

| Scope | Dùng khi nào? | Kết thúc khi nào? |
|---|---|---|
| `CoroutineScope` | Kiểu chung để quản lý coroutine | Theo owner và cấu hình của scope |
| `viewModelScope` | Tải và xử lý dữ liệu cho ViewModel | ViewModel bị clear |
| `lifecycleScope` | Tác vụ gắn với Activity/Fragment | Lifecycle tương ứng bị destroy |
| Custom scope | Một đối tượng cần tự quản lý tác vụ | Bạn gọi `cancel()` |

### 2.1. viewModelScope

```kotlin
class NameViewModel : ViewModel() {
    fun loadName() {
        viewModelScope.launch {
            println(getName()) // Hàm ở mục 1
        }
    }
}
```

ViewModel thường được giữ lại khi xoay màn hình. Vì vậy, `viewModelScope` phù hợp để tải dữ liệu mà không gắn tác vụ vào View cũ. Nó mặc định chạy trên Main; công việc nặng vẫn cần dispatcher phù hợp.

### 2.2. lifecycleScope

Ví dụ đặt trong `onCreate()` của Activity:

```kotlin
lifecycleScope.launch {
    delay(1_000)
    textView.text = "Xin chào"
}
```

Activity bị destroy thì tác vụ được hủy. **Activity chỉ xuống background chưa làm `lifecycleScope.launch` bị hủy.** Với việc nghe Flow để cập nhật UI, dùng `repeatOnLifecycle` ở mục 5.4.

Trong Fragment, tác vụ cập nhật View dùng `viewLifecycleOwner.lifecycleScope`, vì View của Fragment có thể bị hủy trước Fragment.

Nguồn: [Android — Scope theo lifecycle](https://developer.android.com/topic/libraries/architecture/coroutines).

### 2.3. Custom scope

Chỉ tạo khi đối tượng của bạn có vòng đời riêng và biết rõ nơi cần hủy tác vụ:

```kotlin
class CounterManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            repeat(3) {
                delay(1_000)
                println(it + 1)
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
```

Owner của `CounterManager` phải gọi `close()` khi không dùng nữa. Scope đã hủy không dùng lại được. Ở mức cơ bản, chỉ cần biết `SupervisorJob()` giúp một tác vụ con thất bại không tự hủy các tác vụ con khác; nó không tự xử lý lỗi.

**Cách chọn nhanh:** tải dữ liệu trong ViewModel → `viewModelScope`; cập nhật View → scope của lifecycle tương ứng; tự quản lý một đối tượng → custom scope có hàm hủy. Tránh dùng `GlobalScope` cho công việc của màn hình.

## 3. launch, async, withContext — Chạy công việc như thế nào?

| Hàm | Mục đích | Giá trị trả về |
|---|---|---|
| `launch` | Bắt đầu tác vụ, ví dụ tải dữ liệu rồi cập nhật state | `Job`, dùng để quản lý/hủy tác vụ |
| `async` | Bắt đầu tác vụ cần lấy kết quả, hữu ích cho các việc độc lập | `Deferred<T>`, lấy kết quả bằng `await()` |
| `withContext` | Chạy một khối với dispatcher khác rồi chờ kết quả | Kết quả của khối code |

### 3.1. launch

```kotlin
val job = viewModelScope.launch {
    delay(1_000)
    println("Đã tải xong")
}

// Khi không cần tác vụ nữa:
// job.cancel()
```

`launch` vẫn có thể lấy dữ liệu bên trong khối code. Nó chỉ không trả dữ liệu đó trực tiếp cho nơi gọi.

### 3.2. async và await

Hai tác vụ độc lập có thể cùng tiến hành:

```kotlin
viewModelScope.launch {
    val nameTask = async {
        delay(1_000)
        "An"
    }
    val ageTask = async {
        delay(1_000)
        22
    }

    val name = nameTask.await()
    val age = ageTask.await()
    println("$name — $age tuổi")
}
```

Ví dụ mất khoảng 1 giây vì cả hai tác vụ được tạo trước khi `await()`. Nếu tạo tác vụ thứ nhất rồi `await()` ngay trước khi tạo tác vụ thứ hai, chúng sẽ chạy tuần tự. `async` không tự chọn background thread.

### 3.3. withContext

```kotlin
suspend fun readText(file: java.io.File): String {
    return withContext(Dispatchers.IO) {
        file.readText() // Đọc file là thao tác chặn thread
    }
}
```

Gọi `readText()` từ `viewModelScope.launch` được: phần đọc file chạy trên IO. `withContext` chờ khối hoàn tất, rồi code phía sau tiếp tục trong context trước đó.

Nguồn: [Kotlin — Builders](https://kotlinlang.org/docs/coroutines-basics.html), [Kotlin — Context và dispatchers](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html).

## 4. Dispatcher — Công việc chạy ở đâu?

Scope quản lý vòng đời; dispatcher quyết định thread dùng để thực thi công việc.

| Dispatcher | Dùng cho | Ví dụ |
|---|---|---|
| `Dispatchers.Main` | Công việc UI ngắn | Gán text, hiển thị loading |
| `Dispatchers.IO` | Thao tác I/O chặn thread | Đọc file, API đồng bộ cũ |
| `Dispatchers.Default` | Tính toán tốn CPU | Xử lý một danh sách rất lớn |
| `Dispatchers.Unconfined` | Trường hợp đặc biệt, chưa cần dùng ở mức intern | Không đảm bảo tiếp tục trên Main |

```kotlin
viewModelScope.launch {
    val total = withContext(Dispatchers.Default) {
        (1L..1_000_000L).sum()
    }
    // Quay lại Main để cập nhật UI/state
    println(total)
}
```

**Dễ nhầm:** gọi Retrofit `suspend` hoặc Room DAO bất đồng bộ không bắt buộc phải bọc thêm `withContext(IO)`: chúng đã hỗ trợ thực thi phù hợp. Nhưng code đọc file đồng bộ hoặc tính toán nặng do bạn viết vẫn cần chọn dispatcher.

Nguồn: [Kotlin — Dispatchers](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html), [Android — Room bất đồng bộ](https://developer.android.com/training/data-storage/room/async-queries).

## 5. Flow, SharedFlow, StateFlow — Dữ liệu theo thời gian

Hàm `suspend` thường trả một kết quả cho mỗi lần gọi. `Flow` biểu diễn một luồng có thể phát nhiều giá trị theo thời gian.

### 5.1. Flow: phát và nhận nhiều giá trị

```kotlin
fun countdown(): Flow<Int> = flow {
    for (number in 3 downTo 0) {
        emit(number) // Phát giá trị
        delay(1_000)
    }
}

// Trong coroutine:
viewModelScope.launch {
    countdown().collect { number -> // Nhận giá trị
        println(number)             // 3, 2, 1, 0
    }
}
```

Flow tạo bằng `flow {}` là **cold**: khối code chỉ chạy khi có `collect`; mỗi lần collect chạy lại khối đó. Đừng kết luận mọi `Flow` đều cold: `StateFlow` và `SharedFlow` cũng là các loại Flow.

Nguồn: [Kotlin — Flow](https://kotlinlang.org/docs/coroutines-flow.html).

### 5.2. StateFlow: trạng thái hiện tại

```kotlin
class CounterViewModel : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun increase() {
        _count.value = _count.value + 1
    }
}
```

`_count` là phần ViewModel được phép sửa. UI chỉ đọc `count`.

StateFlow luôn có giá trị ban đầu và đọc giá trị hiện tại bằng `.value`. Collector mới nhận trạng thái mới nhất. Ví dụ: số đếm, danh sách người dùng, trạng thái loading. Giá trị bằng giá trị cũ không phát lại; collector chậm có thể bỏ qua các trạng thái trung gian.

Nguồn: [Kotlin — StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/).

### 5.3. SharedFlow: chia sẻ các lần phát

```kotlin
class MessageViewModel : ViewModel() {
    private val _messages = MutableSharedFlow<String>(replay = 0)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun notifySaved() {
        viewModelScope.launch {
            _messages.emit("Đã lưu")
        }
    }
}
```

SharedFlow có thể gửi cùng một lần phát tới nhiều collector. Ví dụ trên dùng cho thông báo Toast khi UI đang nghe. Với `replay = 0`, thông báo phát lúc không có collector sẽ mất; collector mới không nhận thông báo cũ. Nếu thông tin cần hiển thị khi quay lại màn hình, nên lưu nó trong state.

Nguồn: [Kotlin — SharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/).

| Loại | Cần giá trị ban đầu? | Collector mới nhận gì? | Ví dụ |
|---|---|---|---|
| `flow {}` | Không | Chạy lại nguồn từ đầu | Đếm ngược |
| `StateFlow` | Có | Trạng thái mới nhất | Số đếm trên UI |
| `SharedFlow` | Không | Các giá trị được giữ theo `replay` | Thông báo tới nhiều nơi |

StateFlow và SharedFlow là **hot**: chia sẻ cùng state/luồng phát giữa các collector, không chạy lại một khối tạo dữ liệu riêng như `flow {}`.

### 5.4. Nhận Flow trong Fragment

Đặt trong `onViewCreated()`, với `viewModel` là `CounterViewModel`:

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.count.collect { count ->
            binding.countText.text = count.toString()
        }
    }
}
```

Khi View xuống dưới STARTED, block collect bị hủy; khi STARTED lại, block được chạy lại. StateFlow gửi trạng thái mới nhất để UI hiển thị. Nếu nghe thêm một Flow dài hạn, đặt mỗi `collect` trong một `launch` riêng bên trong block này.

Nguồn: [Android — Thu thập Flow theo lifecycle](https://developer.android.com/topic/libraries/architecture/coroutines).

## 6. Chuyển callback thành suspend hoặc Flow

Callback là hàm thư viện gọi lại cho bạn khi có kết quả. Bạn chỉ cần chuyển đổi khi API đang dùng callback; Retrofit và Room đã có hỗ trợ coroutine nên thường không cần tự bọc.

| API callback | Chọn cách nào? |
|---|---|
| Trả một kết quả, không tích hợp việc hủy | `suspendCoroutine` |
| Trả một kết quả, muốn phản ứng khi coroutine bị hủy | `suspendCancellableCoroutine` |
| Gửi nhiều lần, như listener thay đổi dữ liệu | `callbackFlow` |

### 6.1. suspendCoroutine: một kết quả

Giả sử thư viện có API sau. `Result<String>` mang kết quả thành công hoặc lỗi; thư viện cam kết gọi callback **đúng một lần**:

```kotlin
interface LegacyNameApi {
    fun getName(callback: (Result<String>) -> Unit)
}

suspend fun LegacyNameApi.awaitName(): String = suspendCoroutine { cont ->
    getName { result ->
        result.fold(
            onSuccess = { cont.resume(it) },
            onFailure = { cont.resumeWithException(it) }
        )
    }
}
```

`cont` là continuation: cách báo cho coroutine đang chờ biết rằng nó có thể tiếp tục. `resume(value)` trả kết quả; `resumeWithException(error)` báo lỗi. Hàm trên không tự ngừng chờ hoặc hủy thao tác callback khi Job bị hủy.

Nguồn: [Kotlin — suspendCoroutine](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.coroutines/suspend-coroutine.html).

### 6.2. suspendCancellableCoroutine: một kết quả, có hủy

Giả sử API trả về một request có thể hủy, callback chỉ hoàn tất tối đa một lần, và `cancel()` an toàn khi gọi từ thread khác:

```kotlin
interface NameRequest {
    fun cancel()
}

interface CancellableNameApi {
    fun getName(callback: (Result<String>) -> Unit): NameRequest
}

suspend fun CancellableNameApi.awaitName(): String =
    suspendCancellableCoroutine { cont ->
        val request = getName { result ->
            result.fold(
                onSuccess = { cont.resume(it) },
                onFailure = { cont.resumeWithException(it) }
            )
        }
        cont.invokeOnCancellation {
            request.cancel()
        }
    }
```

Caller bị hủy → coroutine ngừng chờ và hook gọi `request.cancel()`. Chỉ dùng `suspendCancellableCoroutine` không tự hủy API bên dưới: bạn phải nối hành động hủy như trên.

Hai ví dụ một kết quả đều yêu cầu API không báo hoàn tất lặp lại. Nếu API gọi callback nhiều lần, hãy kiểm tra hợp đồng của thư viện trước khi viết adapter.

Nguồn: [Kotlin — suspendCancellableCoroutine](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html).

### 6.3. callbackFlow: nhiều kết quả

Giả sử nguồn phát số đếm có hàm đăng ký và gỡ listener an toàn khi gọi từ thread khác:

```kotlin
interface CountSource {
    fun addListener(listener: (Int) -> Unit)
    fun removeListener(listener: (Int) -> Unit)
}

fun CountSource.asFlow(): Flow<Int> = callbackFlow {
    val listener: (Int) -> Unit = { count ->
        trySend(count) // Gửi giá trị từ callback vào Flow
    }
    addListener(listener)
    awaitClose {
        removeListener(listener) // Gỡ khi collection kết thúc/bị hủy
    }
}.conflate()
```

Mỗi lần collect đăng ký listener; `awaitClose` giữ việc nghe và dọn listener khi kết thúc. Ví dụ chỉ quan tâm số đếm mới nhất nên dùng `conflate()` để bỏ giá trị cũ nếu người nhận chậm. `trySend` có thể thất bại khi luồng đã đóng; đây không phải mẫu đảm bảo giữ mọi sự kiện.

Các import cho mục này:

```kotlin
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
```

Nguồn: [Kotlin — callbackFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html).

## 7. Sử dụng với Retrofit và Room Database

Ví dụ: lấy danh sách người dùng từ server, lưu vào Room, rồi hiển thị dữ liệu trong Room. Phần này minh họa các lớp chính; bạn cần tự cấu hình Retrofit, RoomDatabase và cách tạo ViewModel trong project Android.

### 7.1. Retrofit: dùng suspend để gọi API

```kotlin
data class UserDto(val id: Int, val name: String)

interface UserApi {
    @GET("users")
    suspend fun getUsers(): List<UserDto>
}
```

Khi gọi `api.getUsers()`, coroutine chờ kết quả rồi tiếp tục. Không cần `enqueue()` hay tự chuyển callback. Với kiểu trả về body như trên, HTTP lỗi hoặc lỗi mạng được báo bằng exception; xử lý ở nơi gọi. Nếu dùng `Response<T>` thay thế, cần kiểm tra `isSuccessful`.

### 7.2. Room: suspend để ghi, Flow để theo dõi

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name")
    fun observeUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
}
```

`insertUsers()` là một thao tác ghi nên dùng `suspend`. `observeUsers()` trả Flow để nhận dữ liệu khi bảng thay đổi, không cần thêm `suspend` vào hàm này. Room tự xử lý các truy vấn bất đồng bộ ngoài Main.

### 7.3. Repository: nối API và database

```kotlin
class UserRepository(private val api: UserApi, private val dao: UserDao) {
    fun observeUsers(): Flow<List<UserEntity>> = dao.observeUsers()

    suspend fun refresh() {
        val users = api.getUsers()
        dao.insertUsers(users.map { UserEntity(it.id, it.name) })
    }
}
```

Ví dụ chỉ thêm/cập nhật user; user đã bị xóa trên server chưa được xóa khỏi Room. Đây là mẫu đơn giản để học luồng gọi hàm.

### 7.4. ViewModel: khởi chạy và xử lý lỗi

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {
    val users: Flow<List<UserEntity>> = repository.observeUsers()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _error.value = null
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e // Giữ hành vi hủy coroutine
            } catch (e: Exception) {
                _error.value = "Không tải được dữ liệu. Hãy thử lại."
            }
        }
    }
}
```

Trong `onViewCreated()` của Fragment, nghe `users` bằng mẫu ở mục 5.4 và thay phần cập nhật số đếm bằng `render(users)`. Nếu nghe cả `users` và `error`, dùng hai `launch` riêng trong `repeatOnLifecycle`. Nút tải lại gọi `viewModel.refresh()`.

**Luồng dữ liệu:** UI gọi refresh → ViewModel khởi chạy → Repository gọi Retrofit → lưu Room → Flow của Room phát dữ liệu → UI hiển thị. Nếu gọi API lỗi, bước ghi Room chưa chạy nên dữ liệu đã lưu vẫn còn.

Nguồn: [Retrofit — Coroutine adapters trong mã nguồn](https://github.com/square/retrofit/blob/trunk/retrofit/src/main/java/retrofit2/KotlinExtensions.kt), [Android — Room async queries](https://developer.android.com/training/data-storage/room/async-queries).

## 8. Bài tập nhỏ và tự kiểm tra

Làm lần lượt, chưa cần thêm kiến trúc phức tạp:

1. **Đếm số:** dùng `CounterViewModel` ở mục 5.2. Nút bấm tăng số; Fragment nghe `count`. Xoay màn hình và kiểm tra số vẫn được giữ trong cùng ViewModel.
2. **Hai tác vụ:** chạy hai hàm có `delay(1_000)` bằng `async`; tạo cả hai trước khi `await`. So sánh với gọi tuần tự.
3. **Callback:** dùng một API giả theo hợp đồng mục 6.2; hủy Job trước khi trả kết quả và kiểm tra `cancel()` được gọi. Với mục 6.3, kiểm tra listener được gỡ sau khi hủy collection.
4. **Danh sách user:** nối API giả hoặc Retrofit với Room theo mục 7. Sau refresh, UI nhận dữ liệu từ Room; thử lỗi mạng và kiểm tra dữ liệu cũ vẫn còn.

| Câu hỏi | Đáp án ngắn |
|---|---|
| `suspend` có nghĩa là chạy background? | Không, còn phụ thuộc dispatcher và code bên trong. |
| Scope và dispatcher khác nhau thế nào? | Scope quản lý vòng đời; dispatcher quyết định thread thực thi. |
| Khi nào dùng `async`? | Khi cần kết quả từ các tác vụ, thường là tác vụ độc lập chạy đồng thời. |
| StateFlow hay SharedFlow cho danh sách đang hiển thị? | StateFlow, vì UI cần trạng thái mới nhất. |
| Callback một lần hay nhiều lần? | Một lần dùng adapter suspend; nhiều lần dùng callbackFlow. |
| Room DAO trả Flow có cần thêm suspend? | Không. |

**Đạt mức cơ bản khi:** bạn giải thích được các câu hỏi trên và tự sửa ví dụ để tải, lưu, hiển thị dữ liệu mà không chặn Main thread.

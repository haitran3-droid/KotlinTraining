# 40 câu hỏi và trả lời về Kotlin Coroutines — Mức intern

Tài liệu đi kèm [bài giảng cơ bản](kotlin_coroutines_basic.md). Mục tiêu là hiểu **vì sao code hoạt động như vậy** và biết chọn cách làm khi gặp tình huống thực tế.

**Cách học:** mỗi lượt học một nhóm. Đọc câu hỏi, tự trả lời trước, sau đó đối chiếu đáp án. Với câu có code, hãy đoán kết quả trước khi đọc phần giải thích. Không cần thuộc lòng tên API nếu chưa giải thích được tác dụng của nó.

Ví dụ độc lập, cần imports và dependency tương ứng như bài giảng. Các đoạn `viewModelScope.launch` đặt trong ViewModel. `api`, `repository`, `binding` và các hàm UI đại diện cho đối tượng/hàm của ứng dụng; không phải ví dụ Android chạy ngay khi copy cả file.

## A. Coroutine và suspend — Câu 1–6

### 1. Coroutine giải quyết vấn đề gì khi gọi API?

**Trả lời:** giúp viết code chờ kết quả theo thứ tự dễ đọc mà không chặn thread trong lúc chờ API bất đồng bộ.

Ví dụ `val users = api.getUsers()` rồi `render(users)` dễ đọc hơn nhiều callback lồng nhau. Trong lúc request chờ mạng, Main thread vẫn có thể xử lý thao tác người dùng. Điều này phụ thuộc API hỗ trợ bất đồng bộ; coroutine không tự sửa mọi đoạn code blocking.

### 2. Coroutine có phải một thread mới không?

**Trả lời:** không. Coroutine chạy trên thread do dispatcher quyết định. Nhiều coroutine có thể dùng chung thread và nhường thread khi suspend.

Hình dung: thread là người làm việc; coroutine là các công việc. Khi một công việc chờ kết quả, người làm việc có thể chuyển sang công việc khác. Tạo 100 coroutine không đồng nghĩa tạo 100 thread.

### 3. Thêm suspend vào hàm có làm hàm chạy background không?

**Trả lời:** không. `suspend` cho phép hàm tạm dừng và gọi các hàm suspend khác; nó không tự đổi dispatcher.

```kotlin
suspend fun badWait() {
    Thread.sleep(2_000)
}

viewModelScope.launch {
    badWait() // Vẫn chặn Main khoảng 2 giây
}
```

Nếu chỉ muốn chờ, dùng `delay(2_000)`. Nếu là thao tác đồng bộ chặn thread như đọc file, dùng `withContext(Dispatchers.IO)` ở nơi thực hiện thao tác đó.

### 4. delay và Thread.sleep khác nhau thế nào?

**Trả lời:** `delay` tạm dừng coroutine; `Thread.sleep` chặn thread hiện tại.

Khi `delay` chạy trong coroutine trên Main, Main vẫn xử lý UI. Khi `Thread.sleep` chạy trên Main, UI bị đứng. Cả hai làm đoạn code phía sau phải chờ, nhưng cách chờ khác nhau.

### 5. Hai hàm suspend gọi liên tiếp có tự chạy đồng thời không?

**Trả lời:** không. Code trong cùng một coroutine mặc định chạy tuần tự.

```kotlin
suspend fun stepA() { delay(1_000) }
suspend fun stepB() { delay(1_000) }

viewModelScope.launch {
    stepA()
    stepB()
    println("Xong")
}
```

`Xong` xuất hiện sau khoảng 2 giây: A hoàn tất rồi B mới bắt đầu. Dùng `async` khi muốn các tác vụ độc lập cùng tiến hành.

### 6. Vì sao không gọi hàm suspend trực tiếp trong onCreate?

**Trả lời:** `onCreate` là hàm thông thường, không có môi trường suspend để chờ kết quả. Bạn khởi chạy coroutine trong scope phù hợp.

Trong Activity, có thể dùng `lifecycleScope.launch { ... }`. Với tải dữ liệu cho màn hình, thường gọi `viewModel.loadData()`, để ViewModel dùng `viewModelScope.launch`. Tránh dùng `runBlocking` trên Main để ép gọi hàm suspend vì nó chặn thread.

Tham khảo: [Kotlin — Coroutine cơ bản](https://kotlinlang.org/docs/coroutines-basics.html), [Kotlin — Gọi tuần tự và đồng thời](https://kotlinlang.org/docs/composing-suspending-functions.html).

## B. Scope và vòng đời — Câu 7–12

### 7. Scope và dispatcher khác nhau thế nào?

**Trả lời:** scope quản lý tác vụ sống bao lâu và thuộc về ai; dispatcher quyết định thread dùng để chạy code.

Ví dụ `viewModelScope.launch(Dispatchers.IO)` vẫn gắn với ViewModel, nhưng khối code chạy trên IO. Khi ViewModel bị clear, tác vụ nhận yêu cầu hủy dù đang chạy trên IO.

### 8. Khi nào chọn viewModelScope, lifecycleScope hoặc custom scope?

**Trả lời:** chọn theo đối tượng sở hữu công việc.

| Công việc | Scope phù hợp |
|---|---|
| Tải dữ liệu và cập nhật state của ViewModel | `viewModelScope` |
| Nghe state rồi cập nhật View của Fragment | `viewLifecycleOwner.lifecycleScope` |
| Tác vụ chỉ thuộc Activity | `lifecycleScope` của Activity |
| Manager có vòng đời riêng | Custom scope, owner phải hủy khi kết thúc |

Custom scope không có nghĩa là “chạy background”. Bạn vẫn cần chọn dispatcher theo loại công việc.

### 9. Xoay màn hình có hủy tác vụ trong viewModelScope không?

**Trả lời:** thông thường không, nếu ViewModel được lấy qua cơ chế quản lý ViewModel của Android. ViewModel được giữ lại qua thay đổi cấu hình; Activity/View cũ có thể bị tạo lại.

Vì vậy, ViewModel giữ dữ liệu và UI mới đăng ký nghe lại. Không giữ `binding`, View hoặc Activity cũ trong ViewModel. ViewModel cũng không đảm bảo giữ dữ liệu khi process bị hệ điều hành dừng.

### 10. App xuống background thì lifecycleScope có bị hủy không?

**Trả lời:** không chỉ vì xuống background. Scope bị hủy khi lifecycle tương ứng đi đến DESTROYED.

Muốn ngừng nghe Flow khi UI không còn STARTED, dùng `repeatOnLifecycle(STARTED)`. Nó hủy block bên trong khi xuống dưới STARTED và chạy block mới khi quay lại.

### 11. Vì sao Fragment cập nhật UI bằng viewLifecycleOwner.lifecycleScope?

**Trả lời:** Fragment và View của Fragment có vòng đời khác nhau. View có thể đã bị hủy trong khi Fragment vẫn còn tồn tại.

Nếu coroutine giữ binding cũ và tiếp tục cập nhật, nó có thể dùng View không còn hợp lệ. Scope của `viewLifecycleOwner` kết thúc cùng View, phù hợp với code cập nhật UI.

### 12. Custom scope có tự hủy không? SupervisorJob có tự xử lý lỗi không?

**Trả lời:** custom scope cần owner quản lý và gọi `cancel()`. Nếu dùng `SupervisorJob()`, lỗi ở một coroutine con trực tiếp không tự hủy các coroutine con trực tiếp khác, nhưng lỗi vẫn phải được xử lý.

Ví dụ manager có `close()` gọi `scope.cancel()`; nơi tạo manager gọi `close()` khi kết thúc sử dụng. Scope đã bị hủy không thể khởi chạy lại tác vụ hoạt động; muốn dùng một vòng đời mới phải tạo scope mới.

Tham khảo: [Android — Lifecycle và coroutine scopes](https://developer.android.com/topic/libraries/architecture/coroutines), [Kotlin — Supervision](https://kotlinlang.org/docs/exception-handling.html#supervision).

## C. launch, async, withContext và dispatcher — Câu 13–18

### 13. launch không trả kết quả thì có dùng để gọi API được không?

**Trả lời:** có. Bạn lấy kết quả API bên trong khối `launch` rồi cập nhật state.

```kotlin
viewModelScope.launch {
    val users = repository.getUsers()
    // Cập nhật state bằng users tại đây
}
```

Nơi gọi `launch` nhận `Job`, không nhận trực tiếp danh sách users. `Job` dùng để quản lý tác vụ, ví dụ hủy hoặc chờ hoàn tất bằng `join()`.

### 14. Khi nào cần async thay vì gọi suspend trực tiếp?

**Trả lời:** thường dùng khi cần kết quả của nhiều tác vụ độc lập và muốn chúng cùng tiến hành. Một request đơn lẻ thường chỉ cần gọi hàm suspend trực tiếp.

Nếu cần lấy user trước để biết `user.id` rồi mới tải bài viết, hai bước phụ thuộc nhau nên chạy tuần tự. Nếu tải danh sách danh mục và banner độc lập, có thể dùng hai `async`.

### 15. Hai đoạn này mất khoảng bao lâu?

Giả sử `loadA()` và `loadB()` mỗi hàm dùng `delay(1_000)` rồi trả một kết quả; code đặt trong coroutine.

```kotlin
// Đoạn A
val a = async { loadA() }.await()
val b = async { loadB() }.await()

// Đoạn B — xét riêng với đoạn A
val taskA = async { loadA() }
val taskB = async { loadB() }
val resultA = taskA.await()
val resultB = taskB.await()
```

**Trả lời:** A khoảng 2 giây; B khoảng 1 giây. Ở A, phải chờ A xong mới tạo B. Ở B, cả hai được tạo trước khi chờ kết quả. `await()` không chặn thread như `Thread.sleep()`.

### 16. withContext(IO) có làm code phía sau chạy ngay không?

**Trả lời:** không. Coroutine gọi nó chờ khối hoàn tất, rồi mới thực hiện dòng tiếp theo.

```kotlin
viewModelScope.launch {
    val text = withContext(Dispatchers.IO) {
        file.readText()
    }
    // Khối IO hoàn tất; trở lại Main
    println(text)
}
```

Nó hữu ích khi cần chuyển phần đọc file sang IO nhưng vẫn giữ trình tự “đọc xong rồi sử dụng kết quả”.

### 17. IO và Default đều background, vì sao cần phân biệt?

**Trả lời:** chúng được thiết kế cho loại công việc khác nhau. IO phù hợp thao tác chặn thread trong lúc chờ I/O; Default phù hợp công việc dùng CPU để tính toán.

Đọc file đồng bộ → IO. Tính toán hoặc biến đổi một tập dữ liệu rất lớn → Default. Cập nhật View → Main. Không cần chuyển mọi phép tính nhỏ sang Default; đổi context cũng có chi phí.

### 18. launch và async có tự chạy ở background không? Dùng Unconfined được không?

**Trả lời:** không tự chạy background; chúng kế thừa dispatcher của scope nếu không chỉ định. Trong `viewModelScope`, mặc định là Main.

`Unconfined` không đảm bảo tiếp tục trên Main sau khi suspend, nên không phải lựa chọn để cập nhật UI. Ở mức intern, tập trung Main, IO và Default; chỉ dùng Unconfined khi có lý do cụ thể và hiểu hành vi của nó.

Tham khảo: [Kotlin — async và await](https://kotlinlang.org/docs/composing-suspending-functions.html), [Kotlin — Context và dispatchers](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html).

## D. Flow, StateFlow, SharedFlow — Câu 19–25

### 19. Vì sao cần Flow khi đã có suspend?

**Trả lời:** một lần gọi hàm suspend thường trả một kết quả; Flow phù hợp khi nhận dữ liệu nhiều lần theo thời gian.

“Lấy user một lần” → `suspend fun getUser(): User`. “Theo dõi danh sách user trong database” → `fun observeUsers(): Flow<List<User>>`. Một lần phát của Flow có thể chứa cả danh sách, không nhất thiết chỉ một user.

### 20. emit và collect có vai trò gì?

**Trả lời:** `emit` phát giá trị; `collect` nhận và xử lý giá trị.

```kotlin
val numbers = flow {
    emit(1)
    emit(2)
}

// Trong coroutine:
numbers.collect { println(it) }
```

Code in 1 rồi 2. Với `flow {}`, nếu không collect thì khối phát dữ liệu chưa chạy.

### 21. Cold và hot khác nhau như thế nào?

**Trả lời:** cold Flow tạo bằng `flow {}` chạy nguồn riêng cho từng collection. Hot Flow như StateFlow/SharedFlow chia sẻ cùng state hoặc các lần phát cho các collector.

Nếu `flow { emit(api.getUsers()) }` được collect hai lần, thường có hai lần gọi API. Nếu hai màn hình nghe cùng một StateFlow, chúng đọc cùng state được giữ trong đối tượng đó. Chỉ gán kiểu `Flow<T>` không đủ để biết nguồn là cold hay hot.

### 22. Vì sao StateFlow cần giá trị ban đầu và vì sao tách MutableStateFlow?

**Trả lời:** StateFlow biểu diễn trạng thái hiện tại, nên luôn phải có một giá trị. Ví dụ trước khi có dữ liệu, trạng thái danh sách có thể là `emptyList()`.

```kotlin
private val _count = MutableStateFlow(0)
val count: StateFlow<Int> = _count.asStateFlow()
```

ViewModel sửa `_count`; UI đọc `count`. Cách này giúp nơi sở hữu state kiểm soát thay đổi, tránh UI tự gán state tùy ý.

### 23. StateFlow có phát mọi lần gán không?

**Trả lời:** không. Giá trị mới bằng giá trị cũ theo `equals` không tạo lần phát mới; collector chậm cũng có thể bỏ qua trạng thái trung gian.

Nếu state đang là 1 rồi bạn gán 1, UI không nhận một cập nhật mới chỉ vì có phép gán. StateFlow phù hợp với “hiện tại là gì”, không phù hợp để đếm mọi sự kiện.

Với danh sách, ưu tiên tạo danh sách mới rồi gán thay vì sửa trực tiếp một danh sách mutable đã nằm trong state; nếu state không được phát lại, UI có thể không cập nhật.

### 24. SharedFlow có đảm bảo UI nhận thông báo đúng một lần không?

**Trả lời:** không. Với `replay = 0`, event phát khi không có collector sẽ mất. Với `replay = 1`, collector mới nhận lần phát gần nhất và có thể hiển thị lại thông báo cũ.

Nếu dùng SharedFlow cho Toast, cần chấp nhận hành vi đó. Nếu thông báo quan trọng phải còn khi quay lại màn hình, lưu trạng thái cần hiển thị trong state và thiết kế thao tác xác nhận/đóng thông báo. Không chọn `replay = 1` chỉ để “chắc chắn không mất” mà bỏ qua khả năng xử lý lại.

### 25. Vì sao hai collect liên tiếp không cùng nhận dữ liệu?

```kotlin
viewModel.users.collect { render(it) }
viewModel.messages.collect { showToast(it) }
```

**Trả lời:** nếu `users` là luồng dài hạn chưa kết thúc, code chưa tới dòng collect thứ hai. Cần hai coroutine riêng:

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.users.collect { render(it) } }
        launch { viewModel.messages.collect { showToast(it) } }
    }
}
```

Đặt trong `onViewCreated()`. Khi UI ngừng STARTED, cả hai collection trong block đều bị hủy.

Tham khảo: [Kotlin — Flow](https://kotlinlang.org/docs/coroutines-flow.html), [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/), [SharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/), [Android — Lifecycle collection](https://developer.android.com/topic/libraries/architecture/coroutines).

## E. Callback adapters — Câu 26–31

### 26. Khi nào dùng suspendCoroutine, suspendCancellableCoroutine hoặc callbackFlow?

**Trả lời:** đầu tiên xác định API trả kết quả một lần hay thông báo nhiều lần.

| Hành vi API | Cách chuyển đổi |
|---|---|
| Hoàn tất một lần, adapter không hỗ trợ việc hủy | `suspendCoroutine` |
| Hoàn tất một lần, coroutine cần ngừng chờ khi bị hủy | `suspendCancellableCoroutine` |
| Listener gửi dữ liệu nhiều lần | `callbackFlow` |

Nếu API đã có hàm suspend/Flow, dùng trực tiếp trước khi nghĩ đến tự bọc callback.

### 27. Continuation, resume và resumeWithException là gì?

**Trả lời:** continuation là đối tượng giúp tiếp tục coroutine đang chờ callback. `resume(value)` hoàn tất với kết quả; `resumeWithException(error)` hoàn tất với lỗi để caller xử lý bằng `try/catch`.

Hình dung coroutine đang chờ “phiếu kết quả”. Callback đưa giá trị hoặc lỗi vào phiếu đó. Một tác vụ một kết quả chỉ được hoàn tất một lần.

### 28. Callback gọi resume hai lần thì sao?

**Trả lời:** adapter một kết quả có thể ném `IllegalStateException` khi cố hoàn tất continuation lần nữa. Vì vậy, phải biết thư viện có cam kết chỉ gọi một terminal callback hay không.

Nếu callback liên tục gửi dữ liệu mới, dùng `callbackFlow`. Nếu API đáng lẽ hoàn tất một lần nhưng có thể báo lặp, cần bảo vệ việc hoàn tất theo hợp đồng API; chỉ kiểm tra `isActive` không phải bảo đảm chống hai callback chạy đồng thời.

### 29. suspendCancellableCoroutine có tự hủy request của thư viện không?

**Trả lời:** không. Nó hỗ trợ coroutine ngừng chờ khi bị hủy; thao tác của thư viện phải được nối với cancellation hook.

```kotlin
cont.invokeOnCancellation {
    request.cancel()
}
```

Nếu thư viện không cung cấp cách hủy, coroutine vẫn có thể ngừng chờ nhưng thao tác bên dưới có thể tiếp tục. Callback đến muộn cũng không đảm bảo coroutine nhận kết quả thành công sau cancellation.

### 30. Vì sao callbackFlow cần awaitClose?

**Trả lời:** để giữ việc nghe callback và có nơi gỡ listener khi collection kết thúc hoặc bị hủy.

```kotlin
val listener: (Int) -> Unit = { value -> trySend(value) }
source.addListener(listener)
awaitClose { source.removeListener(listener) }
```

Nếu gỡ một listener khác, listener đã đăng ký vẫn còn. Nếu bỏ cleanup, nguồn có thể tiếp tục giữ listener và các đối tượng mà listener tham chiếu sau khi UI không dùng nữa.

### 31. trySend thành công có nghĩa là UI đã xử lý dữ liệu chưa?

**Trả lời:** chưa. Thành công chỉ cho biết giá trị đã được channel chấp nhận; không phải xác nhận UI đã hiển thị hoặc xử lý.

`trySend` không suspend và có thể thất bại khi channel đóng hoặc buffer đầy. Với state như số đếm mới nhất, `conflate()` cho phép bỏ giá trị cũ khi người nhận chậm. Không áp dụng cách bỏ giá trị này cho yêu cầu phải xử lý mọi giao dịch/sự kiện.

Tham khảo: [suspendCoroutine](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.coroutines/suspend-coroutine.html), [suspendCancellableCoroutine](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html), [callbackFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html), [trySend](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-send-channel/try-send.html).

## F. Retrofit và Room — Câu 32–36

### 32. Retrofit dùng suspend rồi có cần enqueue hay withContext(IO) không?

**Trả lời:** với endpoint `suspend fun getUsers(): List<UserDto>`, không cần gọi `enqueue()` và không bắt buộc bọc thêm IO chỉ để thực hiện request. Retrofit hỗ trợ gọi mạng bất đồng bộ và nối cancellation với request.

Nhưng sau khi nhận dữ liệu, nếu bạn tự tính toán nặng thì phần đó vẫn cần `Default`. Không suy ra mọi code trong repository đều an toàn trên Main chỉ vì endpoint là suspend.

### 33. Retrofit trả body và Response<T> khác nhau khi HTTP lỗi thế nào?

**Trả lời:** với kiểu body như `List<UserDto>`, HTTP không thành công được báo bằng exception. Với `Response<List<UserDto>>`, caller nhận response và phải kiểm tra `isSuccessful`; lỗi mạng vẫn có thể ném exception.

Ví dụ HTTP 403 không trở thành “thành công” chỉ vì hàm suspend đã trả về một response. Chỉ đọc `body()` mà không kiểm tra status có thể làm ứng dụng hiểu sai kết quả.

### 34. Vì sao Room ghi bằng suspend nhưng observe bằng Flow không cần suspend?

**Trả lời:** hàm ghi suspend biểu diễn một thao tác chờ hoàn tất. Hàm trả Flow tạo nguồn theo dõi dữ liệu; collector nhận các kết quả theo thời gian.

```kotlin
@Insert
suspend fun insertUser(user: UserEntity)

@Query("SELECT * FROM users")
fun observeUsers(): Flow<List<UserEntity>>
```

Room hỗ trợ thực thi truy vấn bất đồng bộ ngoài Main. Muốn truy vấn một lần cũng có thể dùng `suspend fun getUsers(): List<UserEntity>` thay cho Flow.

### 35. Có phải mỗi lần collect Room Flow là gọi lại API không?

**Trả lời:** không. Room Flow theo dõi database; nó không tự gọi Retrofit. API chỉ chạy khi code của bạn gọi API.

Ở bài cơ bản, `observeUsers()` nghe Room còn `refresh()` gọi Retrofit rồi lưu Room. Hai công việc được tách rõ. Nếu tự đặt gọi API vào khối cold Flow hoặc block lifecycle, collect/chạy lại block có thể khiến API chạy lại.

### 36. API lỗi thì có nên xóa dữ liệu Room cũ không?

**Trả lời:** với màn hình muốn tiếp tục hiển thị dữ liệu đã lưu, nên giữ dữ liệu cũ và báo refresh thất bại.

Trong ví dụ “gọi API → lưu Room”, API lỗi thì bước lưu chưa chạy. Đừng xóa bảng trước khi request hoàn tất nếu chưa có lý do rõ ràng. `REPLACE` theo khóa chỉ thay hàng trùng khóa; nó không tự xóa user đã biến mất trên server.

Tham khảo: [Retrofit — Hỗ trợ coroutine trong mã nguồn](https://github.com/square/retrofit/blob/trunk/retrofit/src/main/java/retrofit2/KotlinExtensions.kt), [Android — Room asynchronous queries](https://developer.android.com/training/data-storage/room/async-queries).

## G. Hủy, lỗi và tình huống thực tế — Câu 37–40

### 37. Gọi job.cancel() có dừng mọi code ngay lập tức không?

**Trả lời:** không. Cancellation mang tính hợp tác: coroutine phải chạy tới điểm hỗ trợ kiểm tra hủy. `delay` có hỗ trợ; một vòng lặp tính toán dài không có điểm kiểm tra có thể tiếp tục chạy.

Với vòng lặp CPU dài trong coroutine, có thể gọi `ensureActive()` định kỳ. Chuyển sang IO/Default giúp tránh chặn Main nhưng không tự làm mọi thao tác blocking dừng ngay khi cancel.

### 38. Vì sao phải ném lại CancellationException?

**Trả lời:** đây là tín hiệu hủy tác vụ, không phải lỗi nghiệp vụ cần biến thành thông báo “tải thất bại”. `catch (Exception)` cũng bắt được tín hiệu này.

```kotlin
try {
    repository.refresh()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // Cập nhật trạng thái lỗi nghiệp vụ
}
```

Ném lại giúp giữ luồng hủy đúng và tránh tiếp tục xử lý lỗi thông thường khi công việc đã không còn cần thiết.

### 39. Người dùng bấm tải liên tục có tự chỉ chạy một request không?

**Trả lời:** không. Mỗi lần gọi hàm có `launch` có thể tạo tác vụ mới. Bạn cần quyết định: bỏ qua lần bấm mới khi đang tải, hay hủy tác vụ cũ để ưu tiên lần mới.

Ví dụ **bỏ qua khi đang tải**, đặt trong ViewModel và gọi `refresh()` từ Main:

```kotlin
private var refreshJob: Job? = null

fun refresh() {
    if (refreshJob?.isActive == true) return

    refreshJob = viewModelScope.launch {
        try {
            repository.refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Cập nhật state để UI biết refresh thất bại
        }
    }
}
```

Nếu yêu cầu là “chỉ giữ lần mới nhất”, có thể hủy Job cũ, nhưng phải hiểu hủy phía client không đảm bảo server chưa thực hiện thao tác. Đặc biệt không coi cancel request ghi dữ liệu là rollback phía server.

### 40. Khi xem một đoạn coroutine, nên tự hỏi những gì?

**Trả lời:** bắt đầu từ yêu cầu của công việc rồi lần theo dữ liệu.

1. **Ai sở hữu công việc?** ViewModel, View hay manager? Chọn scope tương ứng.
2. **Có code chặn thread hoặc tốn CPU không?** Chuyển đúng phần đó sang IO/Default.
3. **Cần một kết quả hay nhiều cập nhật?** Chọn suspend hoặc Flow.
4. **Đây là state hay các lần thông báo?** Chọn StateFlow/SharedFlow theo hành vi cần khi UI quay lại.
5. **Khi hủy hoặc lỗi thì sao?** Giữ cancellation, xử lý lỗi và dọn listener.
6. **Bấm lại hoặc collect lại thì sao?** Kiểm tra request trùng, nguồn cold chạy lại và vòng đời View.

Ví dụ màn hình danh sách user: ViewModel khởi chạy refresh; Retrofit lấy dữ liệu; Room lưu và phát Flow; Fragment collect theo lifecycle; lỗi mạng giữ dữ liệu cũ và hiển thị trạng thái lỗi.

Tham khảo: [Kotlin — Cancellation](https://kotlinlang.org/docs/cancellation-and-timeouts.html), [Android — Coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).

## Cách tự đánh giá

Sau mỗi nhóm, chọn một câu và giải thích lại bằng ví dụ của bạn. Có thể tự chấm:

| Mức | Dấu hiệu |
|---|---|
| Chưa chắc | Nhớ tên API nhưng chưa giải thích được khi nào dùng |
| Hiểu cơ bản | Giải thích được và đoán đúng các ví dụ ngắn |
| Áp dụng được | Tự sửa ví dụ, chọn scope/dispatcher và xử lý tình huống hủy/lỗi |

Nếu chưa chắc, ưu tiên ôn câu **3, 7, 10, 15, 21, 24, 26, 29, 34 và 38**. Đây là những điểm dễ nhầm khi bắt đầu viết Android với coroutines.

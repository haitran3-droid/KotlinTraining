# Kotlin Coroutines - Từ nền tảng đến ứng dụng Android

## Mục tiêu và cách học

Sau bài học, học viên có thể chọn scope theo vòng đời tác vụ; dùng `launch`, `async`, `withContext` và dispatcher phù hợp; chuyển API callback sang `suspend`/`Flow`; phân biệt `Flow`, `SharedFlow`, `StateFlow`; kết hợp Retrofit–Room với UI có lifecycle và cancellation đúng.

**Kiến thức đầu vào:** Kotlin cơ bản, lambda, exception, Activity/Fragment và ViewModel. Học theo thứ tự: nền tảng → scope/builders/dispatcher → cancellation → Flow → callback adapters → Retrofit–Room → kiểm thử và bài tập.

**Quy ước ví dụ:** Các đoạn code minh họa từng ý, không phải một file Kotlin để ghép tất cả vào cùng lúc. Các tên như `fetchA()`, `updateUI()` là hàm của ứng dụng cần tự triển khai; các lớp model/Repository ở mục 7–8 dùng chung một hợp đồng. Android UI cần project Android và các dependency tương ứng. Những ví dụ `runBlocking` chỉ dành cho chương trình console/demo, không gọi trên Main thread của Android.

## Mục lục

1. [Giới thiệu Coroutines](#1-giới-thiệu-coroutines)
2. [Coroutine Scope](#2-coroutine-scope)
3. [Coroutine Builders: launch, async, withContext](#3-coroutine-builders-launch-async-withcontext)
4. [Dispatchers - Phân biệt các loại](#4-dispatchers---phân-biệt-các-loại)
5. [Flow, SharedFlow, StateFlow](#5-flow-sharedflow-stateflow)
6. [callbackFlow, suspendCoroutine, suspendCancellableCoroutine](#6-callbackflow-suspendcoroutine-suspendcancellablecoroutine)
7. [Sử dụng Coroutines với Retrofit](#7-sử-dụng-coroutines-với-retrofit)
8. [Sử dụng Coroutines với Room Database](#8-sử-dụng-coroutines-với-room-database)
9. [Best Practices & Anti-patterns](#9-best-practices--anti-patterns)
10. [Bài tập thực hành](#10-bài-tập-thực-hành)
11. [Kiểm thử Coroutines và Flow](#11-kiểm-thử-coroutines-và-flow)

---

## 1. Giới thiệu Coroutines

### 1.1. Coroutine là gì?

**Coroutine** là một đơn vị tính toán có thể **tạm dừng (suspend)** và **tiếp tục (resume)** mà không blocking thread. Coroutines cho phép viết code bất đồng bộ theo phong cách tuần tự (sequential), giúp code dễ đọc và bảo trì hơn.

```
Thread truyền thống:           Coroutine:
┌─────────────────┐           ┌─────────────────┐
│  Task 1 (block) │           │  Task 1 (suspend)│
│  ██████████████  │           │  ████░░░░████    │
│  Task 2 (wait)  │           │  Task 2 (run)    │
│  ░░░░░░░░░░░░░  │           │  ░░░░████░░░░    │
└─────────────────┘           └─────────────────┘
  Thread bị block              Thread được tái sử dụng
```

### 1.2. Tại sao cần Coroutines?

| Vấn đề với Callbacks/Threads | Giải pháp với Coroutines |
|-------------------------------|--------------------------|
| Callback hell, code khó đọc  | Code tuần tự, dễ đọc     |
| Tạo nhiều thread tốn tài nguyên | Nhiều coroutine có thể chia sẻ thread; chi phí tùy workload |
| Khó xử lý lỗi                | try-catch thông thường   |
| Khó hủy (cancel) task        | Structured concurrency   |
| Memory leak                   | Lifecycle-aware scope    |

### 1.3. Suspend Function

`suspend` function là building block cơ bản của coroutines. Nó cho phép một operation **tạm dừng** và **tiếp tục** mà không ảnh hưởng đến cấu trúc code.

```kotlin
// Khai báo suspend function
suspend fun fetchUserData(): User {
    // Có thể gọi các suspend function khác
    val response = apiService.getUser() // suspend - tạm dừng tại đây
    return response.toUser()            // tiếp tục khi có kết quả
}

// Suspend function CHỈ có thể được gọi từ:
// 1. Một suspend function khác
// 2. Bên trong một coroutine builder (launch, async, ...)
```

> **Lưu ý quan trọng:** Keyword `suspend` không làm function chạy trên background thread, không tự biến code blocking thành non-blocking và không bảo đảm function thực sự suspend. Việc thực thi phụ thuộc vào **Dispatcher** và API được gọi. `delay()` suspend; `Thread.sleep()` block thread.

### 1.4. Thêm thư viện Coroutines vào project

Các phiên bản bên dưới là **mốc minh họa được cố định**, không có nghĩa là bản mới nhất. Khi tạo project Android, dùng version catalog của project và kiểm tra tương thích Kotlin/AGP/KSP. Coroutines core/android/test nên cùng phiên bản. Khai báo dependency thuộc module Android; các plugin Android/Kotlin/KSP được cấu hình riêng trong project.

```kotlin
// build.gradle.kts (Module)
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
```

Imports thường dùng trong các ví dụ coroutine thuần Kotlin (IDE có thể tự thêm import cụ thể):

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
```

---

## 2. Coroutine Scope

### 2.1. CoroutineScope là gì?

**CoroutineScope** định nghĩa vòng đời (lifecycle) của các coroutines được launch bên trong nó. Mọi coroutine phải được launch trong một scope. Khi scope bị hủy, tất cả coroutines con cũng bị hủy theo → đây chính là **Structured Concurrency**.

`CoroutineContext` chứa các thành phần như `Job` (quan hệ cha–con/cancellation), dispatcher (lập lịch thực thi), `CoroutineName` (debug). Scope chứa context để các builder kế thừa. **Scope quyết định ai quản lý tác vụ; dispatcher quyết định cách tác vụ được thực thi.** Structured concurrency còn yêu cầu cha chờ các con hoàn tất và có quy tắc lan truyền lỗi; chỉ tạo một scope tùy ý chưa đủ.

```
CoroutineScope
├── Coroutine 1 (launch)
│   ├── Child Coroutine A
│   └── Child Coroutine B
├── Coroutine 2 (async)
└── Coroutine 3 (launch)

Khi CoroutineScope bị cancel → Tất cả coroutine con đều bị cancel
```

### 2.2. Các loại CoroutineScope trong Android

#### 2.2.1. GlobalScope (⚠️ Không khuyến khích)

```kotlin
// ⚠️ KHÔNG NÊN DÙNG trong production code
// GlobalScope không có Job cha gắn với lifecycle, dễ làm tác vụ vượt quá vòng đời UI
GlobalScope.launch {
    // Không tự hủy khi Activity/Fragment bị destroy.
    // Job trả về từ launch vẫn có thể được cancel thủ công.
    val data = fetchData()
    updateUI(data)  // Activity có thể đã bị destroy!
}
```

**Vấn đề với GlobalScope:**
- Không gắn với lifecycle UI → không tự hủy theo UI
- Dễ gây memory leak
- Khó kiểm soát và test

#### 2.2.2. viewModelScope

```kotlin
class UserViewModel : ViewModel() {

    // viewModelScope được cung cấp bởi lifecycle-viewmodel-ktx
    // Tự động hủy khi ViewModel bị cleared (onCleared)
    fun loadUser(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val user = userRepository.getUser(userId) // suspend function
                _uiState.value = UiState.Success(user)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message)
            }
        }
    }

    // Khi ViewModel bị cleared:
    // - viewModelScope tự động bị cancel
    // - Tất cả coroutines đang chạy bên trong cũng bị cancel
    // - Không cần xử lý thủ công
}
```

**Dependency cần thêm:**
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
```

**Đặc điểm viewModelScope:**
- Sử dụng `Dispatchers.Main.immediate` làm dispatcher mặc định
- Có `SupervisorJob`: lỗi của một coroutine con trực tiếp không hủy các con trực tiếp khác; mỗi tác vụ vẫn cần xử lý lỗi
- Tự động cancel khi `ViewModel.onCleared()` được gọi
- Phù hợp cho các tác vụ liên quan đến UI data

#### 2.2.3. lifecycleScope

```kotlin
class UserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // lifecycleScope gắn với lifecycle của Activity/Fragment
        // Tự động hủy khi Activity/Fragment bị DESTROYED
        lifecycleScope.launch {
            val data = repository.fetchData()
            textView.text = data.toString()
        }

        // Sử dụng repeatOnLifecycle để collect Flow an toàn
        lifecycleScope.launch {
            // CHỈ collect khi lifecycle >= STARTED
            // Tự động dừng collect khi lifecycle < STARTED (ví dụ vào background)
            // Tự động restart collect khi lifecycle >= STARTED (quay lại foreground)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }
}
```

**Dependency cần thêm:**
```kotlin
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
```

**Các phương thức tiện ích:**

```kotlin
// launchWhenStarted - ⚠️ Deprecated, dùng repeatOnLifecycle thay thế
lifecycleScope.launchWhenStarted { 
    // Chạy khi lifecycle >= STARTED
    // PAUSE khi lifecycle < STARTED (không cancel)
}

// launchWhenResumed - ⚠️ Deprecated
lifecycleScope.launchWhenResumed {
    // Chạy khi lifecycle >= RESUMED
}

// ✅ Cách đúng: Dùng repeatOnLifecycle
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        // Code ở đây sẽ chạy khi STARTED và cancel khi dừng
    }
}
```

**Fragment có hai lifecycle: Fragment và View của Fragment.** Khi cập nhật binding/View, dùng lifecycle của View và đặt đăng ký trong `onViewCreated`. Coroutine gắn với Fragment có thể tồn tại sau `onDestroyView`, trong khi View cũ đã bị hủy.

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Mỗi collect dài hạn cần một coroutine riêng.
            launch { viewModel.uiState.collect { render(it) } }
            launch { viewModel.events.collect { handleEvent(it) } }
        }
    }
}
```

`repeatOnLifecycle` **cancel** block khi xuống dưới trạng thái yêu cầu và tạo block mới khi quay lại, không chỉ pause. `lifecycleScope.launch` thông thường vẫn chạy khi UI STOPPED và chỉ tự cancel khi lifecycle DESTROYED. Với Compose, collect state bằng `collectAsStateWithLifecycle()` (dependency `lifecycle-runtime-compose`).

#### 2.2.4. Custom Scope

Khi cần scope với lifecycle tùy chỉnh, bạn có thể tạo custom scope:

```kotlin
class MyCustomManager(
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    // Tạo custom scope với SupervisorJob
    // SupervisorJob: nếu 1 child coroutine fail, các child khác không bị ảnh hưởng
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineName("MyCustomScope")
    )

    fun doWork() {
        scope.launch {
            // Coroutine chạy trong custom scope
            val result = withContext(computeDispatcher) { heavyComputation() }
            processResult(result)
        }
    }

    fun doAnotherWork() {
        scope.launch {
            // Nếu coroutine này fail, coroutine ở doWork() vẫn chạy bình thường
            // nhờ SupervisorJob
            try {
                riskyOperation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e)
            }
        }
    }

    // QUAN TRỌNG: Phải cancel scope khi không cần nữa
    fun cleanup() {
        scope.cancel()
    }
}
```

**Job vs SupervisorJob:**

```kotlin
// Job thông thường: Nếu 1 child có lỗi không phải cancellation → parent và siblings bị cancel
val scope1 = CoroutineScope(Job() + Dispatchers.Main)

// SupervisorJob: Lỗi của 1 child trực tiếp không hủy supervisor/siblings trực tiếp
val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```

```
Job:                            SupervisorJob:
┌──────────────┐               ┌──────────────┐
│   Parent Job │               │ SupervisorJob │
├──────┬───────┤               ├──────┬────────┤
│Child1│Child2 │               │Child1│Child2  │
│ lỗi  │ hủy  │ ← parent hủy │ lỗi  │ chạy │ ← siblings trực tiếp tiếp tục
└──────┴───────┘               └──────┴────────┘
```

### 2.3. So sánh các loại Scope

| Scope | Dispatcher mặc định | Lifecycle gắn với | Tự động cancel khi |
|-------|---------------------|--------------------|--------------------|
| `GlobalScope` | `Dispatchers.Default` khi không chỉ định | Không có lifecycle owner | Không tự cancel theo lifecycle UI |
| `viewModelScope` | `Dispatchers.Main.immediate` | ViewModel | `onCleared()` |
| `lifecycleScope` | `Dispatchers.Main.immediate` | LifecycleOwner | Lifecycle DESTROYED |
| `viewLifecycleOwner.lifecycleScope` | `Dispatchers.Main.immediate` | View của Fragment | View lifecycle DESTROYED |
| `Custom Scope` | Tùy cấu hình | Tùy quản lý | `scope.cancel()` |
| `rememberCoroutineScope` | Dispatcher của composition | Vị trí gọi trong composition | Rời composition |

Custom scope phải có owner rõ ràng gọi `cleanup()`; sau `cancel()` scope không thể dùng lại. Nếu tác vụ cần sống lâu hơn màn hình, dùng scope do tầng application quản lý. Nếu công việc cần được lên lịch và tiếp tục sau khi process bị dừng, cân nhắc WorkManager; coroutine scope không bảo đảm điều đó.

---

## 3. Coroutine Builders: launch, async, withContext

### 3.1. launch - Tác vụ không trả kết quả nghiệp vụ

`launch` bắt đầu một coroutine mới mà **không trả về kết quả**. Trả về `Job` để quản lý coroutine.

```kotlin
class NewsViewModel : ViewModel() {

    fun refreshNews() {
        // launch trả về Job - không quan tâm kết quả trả về
        val job: Job = viewModelScope.launch {
            try {
                val news = newsRepository.fetchLatestNews()
                _newsState.value = news
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorState.value = e.message
            }
        }

        // Có thể dùng job để cancel
        // job.cancel()
    }

    // Nhiều coroutines chạy đồng thời
    fun loadDashboard() {
        viewModelScope.launch {
            // Coroutine 1: Load user info
            launch {
                val user = userRepository.getUser()
                _userState.value = user
            }
            // Coroutine 2: Load notifications (chạy song song)
            launch {
                val notifications = notificationRepository.getNotifications()
                _notificationState.value = notifications
            }
        }
    }
}
```

### 3.2. async - Trả về kết quả

`async` bắt đầu một coroutine mới và **trả về `Deferred<T>`** - một giá trị có thể await. Dùng khi cần kết quả từ coroutine.

```kotlin
class UserProfileViewModel : ViewModel() {

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            // Chạy song song 2 request
            val userDeferred: Deferred<User> = async {
                userRepository.getUser(userId)
            }
            val postsDeferred: Deferred<List<Post>> = async {
                postRepository.getUserPosts(userId)
            }

            // Đợi cả 2 kết quả - chạy song song nên tổng thời gian
            // = max(timeUser, timePosts) thay vì timeUser + timePosts
            val user = userDeferred.await()
            val posts = postsDeferred.await()

            _profileState.value = ProfileData(user, posts)
        }
    }
}
```

**So sánh tuần tự vs song song:**

```kotlin
// Tuần tự - Đúng khi bước sau phụ thuộc bước trước hoặc không cần chạy đồng thời
suspend fun loadSequential() {
    val user = userRepository.getUser()          // 2 giây
    val posts = postRepository.getUserPosts()     // 3 giây
    // Tổng: 5 giây
}

// Đồng thời với async - Phù hợp khi hai tác vụ độc lập
suspend fun loadParallel() = coroutineScope {
    val user = async { userRepository.getUser() }          // 2 giây
    val posts = async { postRepository.getUserPosts() }     // 3 giây
    // Tổng: 3 giây (chạy song song)
    ProcessData(user.await(), posts.await())
}
```

### 3.3. withContext - Chuyển đổi context

`withContext` thực thi một khối trong context được cập nhật, chờ khối và các coroutine con hoàn tất, rồi trả kết quả. Nó tạo phạm vi coroutine con gắn với lời gọi, **không khởi chạy tác vụ độc lập** như `launch`/`async`. Khi thay dispatcher, khối có thể được lập lịch sang thread khác; đổi context không luôn đồng nghĩa đổi thread.

```kotlin
class UserRepository(
    private val api: ApiService,
    private val userDao: UserDao
) {

    // Sử dụng withContext để đảm bảo chạy trên đúng thread
    suspend fun getUser(userId: String): User {
        // Retrofit suspend và Room suspend đã main-safe.
        val networkUser = api.fetchUser(userId)

        userDao.insert(networkUser.toEntity())

        // Parse data phức tạp trên Default thread (CPU-intensive)
        val processedUser = withContext(Dispatchers.Default) {
            networkUser.process() // Heavy computation
        }

        return processedUser
    }
}
```

### 3.4. So sánh launch vs async vs withContext

| Đặc điểm | `launch` | `async` | `withContext` |
|-----------|----------|---------|---------------|
| Trả về | `Job` | `Deferred<T>` | `T` (trực tiếp) |
| Mục đích | Tác vụ có vòng đời, không trả T | Tác vụ đồng thời cần kết quả | Thực thi khối với context khác |
| Chờ kết quả | Không chờ tại lời gọi; `join()` chờ hoàn tất | `await()` suspend khi chưa có kết quả | Suspend caller khi cần chờ khối/con |
| Block thread khi chờ? | Không | Không | Không |
| Use case | Side effects, UI update | Đồng thời nhiều tác vụ | Đổi context/dispatcher cho một khối |
| Cách thực thi | Khởi chạy, trả `Job` | Khởi chạy, trả `Deferred<T>` | Phạm vi con; trả T sau khi hoàn tất |

“Không block khi chờ” không có nghĩa khối code bên trong không thể blocking. Nếu đặt `Thread.sleep()`/API đồng bộ trong khối chạy Main, UI vẫn bị block. Parent vẫn chờ các child hoàn tất theo structured concurrency, kể cả child được tạo bằng `launch` không trả kết quả nghiệp vụ.

```kotlin
// Ví dụ tổng hợp
viewModelScope.launch {    // Khởi tạo coroutine trên Main thread

    val result = withContext(Dispatchers.IO) {   // Chuyển sang IO
        api.fetchData()
    }

    val processed = withContext(Dispatchers.Default) {  // Chuyển sang Default
        heavyProcess(result)
    }

    // Tự động quay lại Main thread
    _state.value = processed
}
```

### 3.5. coroutineScope và supervisorScope

```kotlin
// coroutineScope: Nếu bất kỳ child nào fail → tất cả bị cancel
suspend fun fetchTwoThings(): Pair<A, B> = coroutineScope {
    val a = async { fetchA() }  // Nếu fetchA() throw exception
    val b = async { fetchB() }  // → fetchB() cũng bị cancel
    Pair(a.await(), b.await())
}

// supervisorScope: Child fail không ảnh hưởng child khác
suspend fun fetchIndependently() = supervisorScope {
    val a = async { fetchA() }  // Nếu fetchA() throw exception
    val b = async { fetchB() }  // → fetchB() vẫn tiếp tục chạy
    
    val resultA = try { a.await() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { null }
    val resultB = try { b.await() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { null }
    Pair(resultA, resultB)
}
```

`async` mặc định bắt đầu ngay khi được gọi; gọi `async { fetchA() }.await()` rồi mới `async { fetchB() }.await()` vẫn là tuần tự. Tạo cả hai `Deferred` trước rồi mới await nếu muốn chạy đồng thời. Đồng thời (concurrency) không luôn là chạy CPU trên nhiều thread (parallelism): các request có thể cùng chờ I/O dù caller dùng Main.

### 3.6. Job, cancellation và timeout

Cancellation là **cooperative**: `cancel()` yêu cầu hủy, không cưỡng chế dừng thread. Các API như `delay`, `await` kiểm tra cancellation khi suspend; vòng lặp CPU dài phải chủ động kiểm tra.

| API | Ý nghĩa |
|-----|---------|
| `job.cancel()` | Yêu cầu hủy, trả về ngay; cleanup có thể chưa xong |
| `job.join()` | Suspend caller đến khi job hoàn tất; không lấy kết quả nghiệp vụ |
| `job.cancelAndJoin()` | Yêu cầu hủy và chờ hoàn tất cleanup |
| `ensureActive()` | Ném CancellationException nếu context đã bị hủy |
| `yield()` | Nhường cơ hội thực thi và kiểm tra cancellation |
| `withTimeout(ms)` | Hủy khối và ném TimeoutCancellationException khi hết thời gian |
| `withTimeoutOrNull(ms)` | Trả null khi chính timeout của khối này xảy ra; cancellation từ ngoài vẫn lan truyền |

```kotlin
// Console demo: không chạy runBlocking trên Android Main thread.
fun main() = runBlocking {
    val job = launch {
        try {
            while (isActive) {
                println("Working")
                delay(100)
            }
        } finally {
            println("Cleanup")
        }
    }
    delay(250)
    job.cancelAndJoin()
    println("Job đã kết thúc và cleanup xong")
}

suspend fun sumLargeRange(
    end: Long,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): Long = withContext(dispatcher) {
    var sum = 0L
    for (i in 1L..end) {
        if (i % 1_000L == 0L) currentCoroutineContext().ensureActive()
        sum += i
    }
    sum
}

suspend fun fetchWithTimeout(): User? = withTimeoutOrNull(5_000) {
    userRepository.getUser() // API phải hỗ trợ cancellation
}
```

**Cleanup:** dùng `try/finally` để giải phóng tài nguyên cả khi thành công, lỗi hoặc cancel. Nếu cleanup cần suspend, có thể dùng `withContext(NonCancellable) { ... }` cho khối cleanup ngắn, có giới hạn; không dùng nó để giữ toàn bộ tác vụ nghiệp vụ chạy tiếp sau khi owner bị hủy. Timeout không cưỡng chế dừng một lời gọi blocking tùy ý; API đó cần timeout/cancellation riêng, hoặc `runInterruptible` nếu hỗ trợ interrupt.

**Cancellation không phải lỗi nghiệp vụ.** Bắt rồi chuyển nó thành `UiState.Error`, retry hoặc `Result.failure` sẽ làm sai vòng đời tác vụ. Nếu cần catch `Exception`, bắt và ném lại `CancellationException` trước. `runCatching` cũng bắt cancellation; không dùng nó tùy tiện quanh suspend calls.

---

## 4. Dispatchers - Phân biệt các loại

### 4.1. Dispatchers.Main

Chạy coroutine trên **Main (UI) thread**. Dùng cho các tác vụ liên quan đến UI.

```kotlin
viewModelScope.launch(Dispatchers.Main) {
    // ✅ Cập nhật UI
    textView.text = "Loading..."

    val data = withContext(Dispatchers.IO) {
        repository.fetchData()
    }

    // ✅ Cập nhật UI với kết quả
    textView.text = data.toString()
}
```

**Dispatchers.Main vs Dispatchers.Main.immediate:**
```kotlin
// Dispatchers.Main: Luôn dispatch vào Main thread queue
// → Có thể có delay nhỏ
launch(Dispatchers.Main) {
    println("Chạy sau khi dispatch vào queue")
}

// Dispatchers.Main.immediate: Nếu ĐANG ở Main thread → chạy ngay
// Nếu KHÔNG ở Main thread → dispatch vào queue (giống Main)
launch(Dispatchers.Main.immediate) {
    println("Chạy ngay nếu đang ở Main thread")
}
```

### 4.2. Dispatchers.IO

Tối ưu cho **blocking I/O**: đọc/ghi file, JDBC hoặc network API đồng bộ. Mặc định giới hạn parallelism là `max(64, số CPU cores)`, có thể cấu hình; các view `limitedParallelism` của IO có tính elastic nên đây không phải giới hạn tổng số thread của toàn ứng dụng.

Retrofit `suspend` và Room DAO `suspend`/`Flow` tự quản lý phần I/O; không bắt buộc bọc mỗi lời gọi bằng `withContext(IO)`. Dispatcher vẫn cần thiết cho code blocking hoặc xử lý CPU do ứng dụng tự viết.

```kotlin
suspend fun readLargeFile(): String = withContext(Dispatchers.IO) {
    // ✅ Đọc file trên IO thread - không block Main thread
    File("large_file.txt").readText()
}

suspend fun fetchFromNetwork(): Response = withContext(Dispatchers.IO) {
    // ✅ Network call trên IO thread
    apiService.getData()
}

suspend fun saveToDatabase(user: User) = withContext(Dispatchers.IO) {
    // ✅ Database operation trên IO thread
    database.userDao().insert(user)
}
```

### 4.3. Dispatchers.Default

Tối ưu cho **CPU-intensive operations**: tính toán, parsing, sorting. Parallelism mặc định theo số CPU cores, tối thiểu 2; không nên hiểu đây là một số thread cố định cho mọi thời điểm.

```kotlin
suspend fun processLargeList(items: List<Item>): List<Result> =
    withContext(Dispatchers.Default) {
        // ✅ CPU-intensive: sort, filter, map
        items
            .filter { it.isValid() }
            .sortedBy { it.priority }
            .map { it.transform() }
    }

suspend fun parseJson(json: String): Data = withContext(Dispatchers.Default) {
    // ✅ JSON parsing phức tạp
    Gson().fromJson(json, Data::class.java)
}

suspend fun calculateComplexResult(): BigDecimal = withContext(Dispatchers.Default) {
    // ✅ Tính toán phức tạp
    (1..1_000_000).fold(BigDecimal.ZERO) { acc, i ->
        acc + BigDecimal(i).pow(2)
    }
}
```

### 4.4. Dispatchers.Unconfined

Chạy coroutine trên thread hiện tại cho đến khi gặp suspension point đầu tiên. Sau khi resume, chạy trên thread mà resumption xảy ra.

```kotlin
// ⚠️ Chỉ dùng khi hiểu rõ thread resumption; không dùng để cập nhật Android UI
launch(Dispatchers.Unconfined) {
    println("Thread: ${Thread.currentThread().name}") // Thread gọi
    delay(100)
    println("Thread: ${Thread.currentThread().name}") // Có thể khác thread!
}
```

### 4.5. Tạo Custom Dispatcher

Chỉ tạo thread pool riêng khi có nhu cầu rõ ràng và owner quản lý tài nguyên. Nếu chỉ muốn giới hạn lượng CPU work cùng chạy, cân nhắc `Dispatchers.Default.limitedParallelism(n)`. Nó không tạo n thread riêng và không thay thế `Mutex` để bảo vệ một thao tác có suspension: coroutine khác có thể chạy khi coroutine hiện tại suspend. `newSingleThreadContext` cần opt-in theo compiler và phải được đóng; ví dụ dưới là phần mở rộng, không bắt buộc cho bài cơ bản.

```kotlin
// Tạo dispatcher với 4 threads
val customDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

// Sử dụng
suspend fun doWork() = withContext(customDispatcher) {
    // Chạy trên custom thread pool
    heavyWork()
}

// Tạo dispatcher dành riêng cho một thread (tốn tài nguyên)
val singleThreadDispatcher = newSingleThreadContext("MySingleThread")

// QUAN TRỌNG: Phải close khi không dùng nữa
// customDispatcher.close()
// singleThreadDispatcher.close()
```

### 4.6. Bảng so sánh Dispatchers

| Dispatcher | Thread Pool | Use Case | Số Thread |
|------------|------------|----------|-----------|
| `Main` | Main/UI thread | Cập nhật UI | 1 |
| `Main.immediate` | Main/UI thread | UI update ngay | 1 |
| `IO` | Shared pool | Blocking I/O | Parallelism mặc định max(64, cores); có elasticity |
| `Default` | Shared pool | CPU computation | Parallelism mặc định max(2, cores) |
| `Unconfined` | Không cố định | Trường hợp đặc biệt | N/A |

```
Dispatchers.Main        → [Main Thread]
                            │
Dispatchers.IO          → [Blocking I/O, giới hạn parallelism có thể cấu hình]
                            │ (shared backing threads)
Dispatchers.Default     → [CPU work, parallelism theo cores (tối thiểu 2)]
                            │
Dispatchers.Unconfined  → [Thread gọi ban đầu, sau đó theo cơ chế resume]
```

> **Lưu ý:** `Dispatchers.IO` và `Dispatchers.Default` chia sẻ threads. Khi một coroutine trên `Default` gọi `withContext(IO)`, nó **có thể** tiếp tục trên cùng thread mà không cần chuyển.

---

## 5. Flow, SharedFlow, StateFlow

### 5.1. Flow - Cold Stream

**Flow** là interface biểu diễn một asynchronous stream. Flow tạo bằng `flow {}` là **cold**: khối chỉ chạy khi được collect và chạy lại cho mỗi collector. `SharedFlow` và `StateFlow` cũng là `Flow`, nhưng là **hot**; không được kết luận mọi `Flow` đều cold.

```kotlin
// Tạo Flow
fun getNumbers(): Flow<Int> = flow {
    for (i in 1..5) {
        delay(1000)    // Giả lập async work
        emit(i)        // Phát ra giá trị
    }
}

// Collect Flow
viewModelScope.launch {
    getNumbers().collect { number ->
        println("Received: $number")
    }
}

// Output (mỗi giây 1 dòng):
// Received: 1
// Received: 2
// Received: 3
// Received: 4
// Received: 5
```

**Đặc điểm của cold Flow tạo bằng `flow {}`:**
- **Cold**: Không chạy cho đến khi có collector
- **Sequential**: Các giá trị được phát theo thứ tự
- **Cancellable**: Tự động cancel khi scope bị cancel
- **Mỗi collector nhận stream riêng**: 2 collector = 2 lần execute

```kotlin
// Mỗi collector chạy flow riêng biệt
val numbersFlow = getNumbers()

// Collector 1 - trigger flow chạy lần 1
numbersFlow.collect { println("Collector 1: $it") }

// Collector 2 - trigger flow chạy lần 2 (hoàn toàn độc lập)
numbersFlow.collect { println("Collector 2: $it") }
```

#### Flow Operators (Toán tử biến đổi)

```kotlin
// map - Biến đổi từng giá trị
fun getUsers(): Flow<User> = flow { /* ... */ }
    .map { userData -> userData.toUser() }

// filter - Lọc giá trị
fun getActiveUsers(): Flow<User> = getUsers()
    .filter { user -> user.isActive }

// transform - Biến đổi linh hoạt (có thể emit nhiều giá trị)
fun getUserEvents(): Flow<Event> = getUsers()
    .transform { user ->
        emit(Event.Loading(user.id))
        val events = fetchEvents(user.id)
        emit(Event.Loaded(events))
    }

// flatMapConcat - Ánh xạ mỗi giá trị thành Flow khác (tuần tự)
fun getUsersWithPosts(): Flow<UserWithPosts> = getUsers()
    .flatMapConcat { user ->
        getPostsForUser(user.id).map { posts ->
            UserWithPosts(user, posts)
        }
    }

// flatMapLatest - Chỉ giữ flow mới nhất, cancel flow cũ
fun searchResults(query: Flow<String>): Flow<List<Result>> =
    query.flatMapLatest { q ->
        searchRepository.search(q)
    }

// combine - Kết hợp nhiều flow
fun combineExample(): Flow<String> =
    combine(flowA, flowB) { a, b ->
        "$a - $b"
    }

// zip - Ghép cặp từng phần tử
fun zipExample(): Flow<Pair<Int, String>> =
    flowA.zip(flowB) { a, b -> Pair(a, b) }

// debounce - Chờ một khoảng thời gian trước khi emit (dùng cho search)
fun searchDebounced(queryFlow: Flow<String>): Flow<List<Result>> =
    queryFlow
        .debounce(300) // Chờ 300ms sau lần gõ cuối
        .distinctUntilChanged() // Chỉ emit nếu giá trị thay đổi
        .flatMapLatest { query ->
            searchRepository.search(query)
        }

// catch - Xử lý exception trong flow
fun safeFlow(): Flow<Data> = dataFlow
    .catch { e ->
        emit(Data.Error(e.message))
    }

// onEach - Side effect cho mỗi giá trị
fun loggedFlow(): Flow<Data> = dataFlow
    .onEach { data -> log("Received: $data") }

// flowOn - Chuyển upstream sang dispatcher khác
fun backgroundFlow(): Flow<Data> = flow {
    // Code này chạy trên IO thread
    val data = fetchFromNetwork()
    emit(data)
}.flowOn(Dispatchers.IO) // Chỉ ảnh hưởng code TRÊN dòng này
```

### 5.2. StateFlow - State holder observable

**StateFlow** là một hot flow đặc biệt, luôn giữ **giá trị hiện tại** và phát ra giá trị mới nhất cho mọi collector. Tương tự LiveData nhưng mạnh mẽ hơn.

```kotlin
class CounterViewModel : ViewModel() {

    // MutableStateFlow - có thể thay đổi giá trị
    // Bắt buộc phải có giá trị khởi tạo (initial value)
    private val _counter = MutableStateFlow(0)

    // StateFlow (read-only) - expose ra ngoài
    val counter: StateFlow<Int> = _counter.asStateFlow()

    fun increment() {
        _counter.update { it + 1 } // Atomic read-modify-write; chỉ tăng một lần
    }

    fun reset() {
        _counter.value = 0
    }
}
```

**Collect StateFlow trong Activity/Fragment:**

```kotlin
class CounterActivity : AppCompatActivity() {

    private val viewModel: CounterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Cách đúng: Dùng repeatOnLifecycle
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.counter.collect { count ->
                    binding.counterText.text = "Count: $count"
                }
            }
        }

        // ✅ Hoặc dùng extension function
        lifecycleScope.launch {
            viewModel.counter
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { count ->
                    binding.counterText.text = "Count: $count"
                }
        }
    }
}
```

**Đặc điểm StateFlow:**

| Đặc điểm | Mô tả |
|-----------|--------|
| Hot stream | Giữ state độc lập với collector; có thể cập nhật khi không có collector |
| Có initial value | Bắt buộc khởi tạo với giá trị ban đầu |
| Conflation | Chỉ giữ giá trị mới nhất, bỏ qua intermediate values |
| distinctUntilChanged | Chỉ phát khi giá trị thay đổi (so sánh bằng `equals`) |
| Replay = 1 | Collector mới nhận ngay giá trị hiện tại |
| Thread-safe | An toàn khi cập nhật từ nhiều threads |

Gán `.value` thread-safe, nhưng `_counter.value++` gồm đọc rồi ghi nên không atomic khi nhiều coroutine cùng cập nhật. Dùng `update`; lambda có thể được chạy lại khi có cạnh tranh nên tránh side effect trong lambda. State nên immutable: tạo `copy()`/list mới thay vì sửa trực tiếp một mutable list bên trong state rồi gán lại cùng object.

### 5.3. SharedFlow - Event broadcasting

**SharedFlow** là một hot flow cho phép phát sự kiện (events) đến nhiều collectors. Khác StateFlow, nó **không có giá trị khởi tạo** và có thể cấu hình replay cache.

```kotlin
class EventViewModel : ViewModel() {

    // Sự kiện tức thời: chỉ các collector đang hoạt động nhận được (replay = 0)
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    // SharedFlow với replay cache
    private val _notifications = MutableSharedFlow<Notification>(
        replay = 1,                              // Cache 1 giá trị cho collector mới
        extraBufferCapacity = 64,                // Buffer thêm 64 giá trị
        onBufferOverflow = BufferOverflow.DROP_OLDEST // Bỏ giá trị cũ nhất khi buffer đầy
    )
    val notifications: SharedFlow<Notification> = _notifications.asSharedFlow()

    fun showMessage(message: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowSnackbar(message))
        }
    }

    fun navigateTo(route: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.Navigate(route))
        }
    }
}

// Sealed class cho UI Events
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data object ShowDialog : UiEvent()
}
```

**Collect SharedFlow:**

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UiEvent.ShowSnackbar -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                        is UiEvent.Navigate -> {
                            // Navigate...
                        }
                        is UiEvent.ShowDialog -> {
                            // Show dialog...
                        }
                    }
                }
            }
        }
    }
}
```

### 5.4. So sánh Flow vs SharedFlow vs StateFlow

**Giới hạn của SharedFlow khi dùng cho UI events:**

- `MutableSharedFlow()` mặc định không có replay/buffer. Có subscriber: `emit` có thể suspend chờ các subscriber nhận giá trị. Không có subscriber: `emit` trả ngay và giá trị bị mất.
- `extraBufferCapacity` giúp subscriber chậm; khi không có subscriber, chỉ `replay` giữ giá trị cho subscriber tương lai.
- `replay = 1` giữ sự kiện gần nhất nhưng collector mới có thể xử lý lại sau khi xoay màn hình. SharedFlow không bảo đảm sự kiện được xử lý đúng một lần.
- Snackbar tức thời có thể chấp nhận bị bỏ qua khi UI không hoạt động. Với kết quả cần giữ, biểu diễn bằng UI state và xác nhận đã xử lý theo yêu cầu sản phẩm. Channel cũng không tự cung cấp bảo đảm xử lý đúng một lần qua mọi thay đổi lifecycle/process.

| Đặc điểm | Flow | SharedFlow | StateFlow |
|-----------|------|------------|-----------|
| Loại | Cold nếu tạo bằng flow {} | Hot | Hot |
| Giá trị khởi tạo | Không | Không | Bắt buộc có |
| Replay | Không | Cấu hình được (0, 1, N) | Luôn = 1 |
| distinctUntilChanged | Không mặc định | Không mặc định | Có mặc định |
| Nhiều collector | Mỗi collector = 1 execution | Chia sẻ giữa collectors | Chia sẻ giữa collectors |
| Use case | Data streams, DB queries | Events, notifications | UI state |
| Giữ giá trị hiện tại | Không | Chỉ nếu replay > 0 | Luôn luôn |

```
Flow (Cold):
  Collector 1 subscribe → Flow bắt đầu emit → Collector 1 nhận data
  Collector 2 subscribe → Flow bắt đầu emit LẠI → Collector 2 nhận data riêng

SharedFlow (Hot):
  SharedFlow đang emit: 1, 2, 3, 4, 5...
  Collector 1 subscribe (lúc 2) → nhận: 2, 3, 4, 5...
  Collector 2 subscribe (lúc 4) → nhận: 4, 5...
  (replay=0: collector mới không nhận giá trị cũ)

StateFlow (Hot):
  StateFlow.value = "Hello" → đổi thành "World"
  Collector 1 subscribe → nhận ngay: "World" (giá trị hiện tại)
  StateFlow.value = "World" (không đổi) → KHÔNG phát lại (distinctUntilChanged)
  StateFlow.value = "Kotlin" → cả Collector 1 nhận: "Kotlin"
```

### 5.5. Chuyển đổi Flow thành StateFlow/SharedFlow

```kotlin
class SearchViewModel : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    // Chuyển Flow thành StateFlow với stateIn
    val searchResults: StateFlow<List<SearchResult>> = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else searchRepository.search(query)
        }
        .stateIn(
            scope = viewModelScope,          // Scope để chạy flow
            started = SharingStarted.WhileSubscribed(5000), // Chiến lược start/stop
            initialValue = emptyList()       // Giá trị khởi tạo
        )

    // Chuyển Flow thành SharedFlow với shareIn
    val notifications: SharedFlow<Notification> = notificationRepository
        .getNotificationsStream()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,  // Bắt đầu khi có collector đầu tiên
            replay = 1                         // Cache 1 giá trị
        )
}
```

**SharingStarted strategies:**

```kotlin
// Eagerly: Bắt đầu ngay, không dừng theo subscriber; vẫn kết thúc khi scope bị hủy
SharingStarted.Eagerly

// Lazily: Bắt đầu khi có collector đầu tiên, tiếp tục trong vòng đời scope
SharingStarted.Lazily

// WhileSubscribed: Bắt đầu khi có collector, dừng khi không còn collector
// stopTimeoutMillis = 5000: Chờ 5 giây trước khi dừng (tránh restart khi config change)
// replayExpirationMillis: Thời gian giữ replay cache sau khi dừng
SharingStarted.WhileSubscribed(
    stopTimeoutMillis = 5000,
    replayExpirationMillis = Long.MAX_VALUE
)
```

Các chiến lược `Eagerly`/`Lazily` chỉ tiếp tục **trong vòng đời scope**; khi scope bị hủy, upstream cũng bị hủy. Với `WhileSubscribed(5000)`, subscriber cuối rời đi thì chờ 5 giây trước khi dừng upstream; state/replay vẫn được giữ theo `replayExpirationMillis`. Khi có subscriber mới, cold upstream có thể chạy lại và lặp lại request/đăng ký listener. Đây không phải cơ chế lưu state qua process death.

### 5.6. Flow context, lỗi và collector chậm

`flowOn` chỉ đổi context của **upstream**, không đổi context collector. Không dùng `withContext(IO) { emit(...) }` bên trong `flow {}` để emit từ context khác; dùng `flowOn` hoặc `channelFlow`/`callbackFlow` khi cần nhiều context.

```kotlin
val uiFlow = flow {
    emit(readBlockingFile())
}
    .flowOn(Dispatchers.IO) // readBlockingFile và emit chạy trong upstream IO
    .map { parse(it) }     // map này chạy trong context collect nếu không có flowOn khác
    .catch { error ->
        // Chỉ bắt lỗi upstream trước catch; không bắt lỗi của render() bên dưới.
        emit(fallbackValue(error))
    }

lifecycleScope.launch {
    uiFlow.collect { render(it) }
}
```

`catch` không bắt lỗi cancellation dùng để hủy collection. `SharedFlow`/`StateFlow` không hoàn tất bình thường và không có kênh lỗi terminal; phải biểu diễn lỗi bằng giá trị state/event. Đặt `catch`/`retryWhen` **trước** `stateIn`/`shareIn` khi cần xử lý lỗi upstream.

| Operator | Khi producer nhanh hơn consumer | Dùng khi |
|----------|--------------------------------|----------|
| `buffer(n)` | Cho producer/consumer chạy đồng thời; mặc định suspend khi buffer đầy | Muốn giữ các giá trị và hấp thụ chênh lệch tốc độ |
| `conflate()` | Bỏ các giá trị trung gian đang chờ, giữ giá trị mới nhất | State/progress, không cần mọi lần cập nhật |
| `collectLatest` | Cancel khối xử lý giá trị cũ khi có giá trị mới | Render/tính toán suspend mà kết quả cũ không còn cần |

```kotlin
// Console demo: thay collect bằng collectLatest/conflate().collect để quan sát khác biệt.
fun main() = runBlocking {
    flow {
        for (value in 1..3) {
            emit(value)
            delay(100)
        }
    }.collectLatest { value ->
        println("Start $value")
        delay(300) // Xử lý 1 và 2 bị cancel khi nhận giá trị mới
        println("Done $value")
    }
}
```

Đừng dùng `conflate`/`collectLatest` cho nghiệp vụ bắt buộc xử lý mọi phần tử. Cancellation của `collectLatest` cũng cần code xử lý hợp tác; một vòng lặp CPU không kiểm tra cancellation vẫn có thể chạy tiếp.

`combine` đợi mỗi upstream có giá trị đầu rồi dùng giá trị mới nhất khi bất kỳ upstream đổi; `zip` ghép từng cặp tương ứng. `debounce` đợi khoảng im lặng; `flatMapLatest` hủy inner Flow cũ khi nhận query mới. Nếu đặt `debounce` trước `flatMapLatest`, việc hủy query cũ xảy ra sau khi query mới đi qua debounce, không ngay lúc gõ phím.

Một số operator như `debounce`/`flatMapLatest` có thể yêu cầu `@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)` tùy phiên bản. Đọc cảnh báo của compiler và opt-in tại phạm vi nhỏ nhất. Không cần đặt `distinctUntilChanged()` trực tiếp trên StateFlow vốn đã so sánh bằng `equals`; sau các operator biến đổi thì cân nhắc theo semantics của pipeline.

---

## 6. callbackFlow, suspendCoroutine, suspendCancellableCoroutine

### 6.1. suspendCoroutine

`suspendCoroutine` chuyển đổi một callback-based API thành suspend function. Nó tạm dừng coroutine hiện tại cho đến khi callback được gọi.

```kotlin
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Chuyển callback thành suspend function
suspend fun getUserFromCallback(): User = suspendCoroutine { continuation ->
    // API cũ dùng callback
    userApi.fetchUser(object : Callback<User> {
        override fun onSuccess(user: User) {
            // Resume coroutine với kết quả thành công
            continuation.resume(user)
        }

        override fun onError(error: Throwable) {
            // Resume coroutine với exception
            continuation.resumeWithException(error)
        }
    })
    // Không tự quan sát cancellation của Job và không tự hủy request.
    // Hợp đồng API phải gọi đúng một callback kết thúc, không gọi success/error lặp lại.
}

// Sử dụng
viewModelScope.launch {
    try {
        val user = getUserFromCallback() // Giờ có thể dùng như suspend function
        _userState.value = user
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        _errorState.value = e.message
    }
}
```

### 6.2. suspendCancellableCoroutine (✅ Khuyến khích)

Tương tự `suspendCoroutine` nhưng **hỗ trợ cancellation**. Luôn dùng thay cho `suspendCoroutine` khi có thể.

```kotlin
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean

suspend fun getUserCancellable(): User = suspendCancellableCoroutine { continuation ->
    // Bảo vệ cả trường hợp API lỗi hợp đồng: callback kết thúc bị gọi lặp/đồng thời.
    val completed = AtomicBoolean(false)
    val call = userApi.fetchUser(object : Callback<User> {
        override fun onSuccess(user: User) {
            if (completed.compareAndSet(false, true)) {
                continuation.resume(user)
            }
        }

        override fun onError(error: Throwable) {
            if (completed.compareAndSet(false, true)) {
                continuation.resumeWithException(error)
            }
        }
    })

    // ✅ Xử lý khi coroutine bị cancel
    continuation.invokeOnCancellation {
        completed.set(true)
        call.cancel() // Hủy network request
    }
}
```

`isActive` là phép kiểm tra trạng thái, **không phải khóa**: hai callback đồng thời đều có thể thấy true và cùng resume. Continuation chỉ được hoàn tất một lần; adapter phải dựa vào hợp đồng single-shot đáng tin cậy hoặc bảo vệ bằng cơ chế atomic như trên. Callback registration/cancel cũng phải tuân theo quy tắc thread của API.

`suspendCancellableCoroutine` có prompt cancellation guarantee: nếu Job bị hủy khi đang suspend, caller không tiếp tục thành công chỉ vì callback vừa resume. Tuy nhiên request/listener bên ngoài không tự hủy; phải nối `invokeOnCancellation` với `cancel`/unregister. Nếu kết quả là tài nguyên cần `close()`, phải xử lý cả trường hợp tài nguyên được callback cấp nhưng không đến được caller do cancellation race (xem overload `resume(value, onCancellation)` trong tài liệu).

**Ví dụ thực tế - Wrap Location API:**

```kotlin
@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient
): Location = suspendCancellableCoroutine { continuation ->

    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        cancellationTokenSource.token
    ).addOnSuccessListener { location ->
        if (continuation.isActive) {
            if (location != null) {
                continuation.resume(location)
            } else {
                continuation.resumeWithException(
                    Exception("Location is null")
                )
            }
        }
    }.addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }.addOnCanceledListener {
        continuation.cancel()
    }

    // Cleanup khi coroutine bị cancel
    continuation.invokeOnCancellation {
        cancellationTokenSource.cancel()
    }
}

// Sử dụng
viewModelScope.launch {
    try {
        val location = getCurrentLocation(fusedLocationClient)
        _locationState.value = location
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        _errorState.value = "Không thể lấy vị trí: ${e.message}"
    }
}
```

**Ví dụ - Wrap AlertDialog (UI thuộc lifecycle của View):**

```kotlin
suspend fun showConfirmDialog(context: Context, message: String): Boolean =
    withContext(Dispatchers.Main.immediate) {
      suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean(false)
        fun complete(value: Boolean) {
            if (completed.compareAndSet(false, true)) continuation.resume(value)
        }
        val dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                complete(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                complete(false)
            }
            .setOnCancelListener {
                complete(false)
            }
            .create()

        dialog.show()

        continuation.invokeOnCancellation {
            completed.set(true)
            // Cancellation handler có thể được gọi từ thread khác.
            Handler(Looper.getMainLooper()).post { dialog.dismiss() }
        }
      }
    }

// Sử dụng
viewLifecycleOwner.lifecycleScope.launch {
    val confirmed = showConfirmDialog(requireContext(), "Bạn có chắc muốn xóa?")
    if (confirmed) {
        viewModel.deleteItem()
    }
}
```

### 6.3. So sánh suspendCoroutine vs suspendCancellableCoroutine

| Đặc điểm | `suspendCoroutine` | `suspendCancellableCoroutine` |
|-----------|-------------------|-------------------------------|
| Quan sát Job cancellation khi chờ callback | ❌ Không | ✅ Có |
| `invokeOnCancellation` | ❌ Không có | ✅ Có |
| `isActive` check | ❌ Không có | ✅ Có |
| Cleanup khi cancel | Không hỗ trợ | Resource cleanup |
| Khuyến khích dùng | Hiếm khi | ✅ Hầu hết trường hợp |

### 6.4. callbackFlow - Chuyển callback thành Flow

`callbackFlow` tạo một **Flow từ callback-based API** phát ra nhiều giá trị. Khác với `suspendCoroutine` (chỉ 1 giá trị), `callbackFlow` có thể emit liên tục.

```kotlin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

// Ví dụ 1: Wrap Firebase Realtime Database listener
fun getRealtimeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val messages = snapshot.children.mapNotNull {
                it.getValue(Message::class.java)
            }
            // Phát giá trị vào flow
            trySend(messages)
        }

        override fun onCancelled(error: DatabaseError) {
            // Đóng flow với lỗi
            close(error.toException())
        }
    }

    // Đăng ký listener
    val ref = database.child("chats").child(chatId).child("messages")
    ref.addValueEventListener(listener)

    // Giữ listener hoạt động cho đến khi channel đóng hoặc collection bị hủy.
    awaitClose {
        // Cleanup: Hủy đăng ký listener
        ref.removeEventListener(listener)
    }
}.buffer(Channel.CONFLATED) // Callback là snapshot toàn danh sách; giữ bản mới nhất.

// Collect flow
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        getRealtimeMessages("chat_123").collect { messages ->
            adapter.submitList(messages)
        }
    }
}
```

```kotlin
// Ví dụ 2: Copy dữ liệu sensor thành snapshot immutable.
// Android có thể tái sử dụng SensorEvent sau callback: không gửi trực tiếp object đó.
data class SensorReading(val timestamp: Long, val values: List<Float>)

fun getSensorData(sensorManager: SensorManager, sensorType: Int): Flow<SensorReading> =
    callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(SensorReading(event.timestamp, event.values.toList()))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            close(IllegalStateException("Sensor không tồn tại"))
            return@callbackFlow
        }
        val registered = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            sensorManager.unregisterListener(listener)
            close(IllegalStateException("Không đăng ký được sensor"))
            return@callbackFlow
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.buffer(Channel.CONFLATED) // UI chỉ cần snapshot mới nhất
```

```kotlin
// Ví dụ 3: Wrap TextWatcher (EditText)
fun EditText.textChanges(): Flow<String> = callbackFlow {
    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            trySend(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    addTextChangedListener(watcher)
    trySend(text.toString()) // Collector mới nhận cả nội dung hiện tại

    awaitClose {
        removeTextChangedListener(watcher)
    }
}.buffer(Channel.CONFLATED) // Query mới nhất; chủ động bỏ query trung gian

// Sử dụng: Search với debounce
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        searchEditText.textChanges()
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                searchRepository.search(query)
            }
            .collect { results ->
                adapter.submitList(results)
            }
    }
}
```

```kotlin
// Ví dụ 4: Wrap NetworkCallback (API 24+, cần ACCESS_NETWORK_STATE trong manifest).
// Theo dõi default network; nhiều network có INTERNET không đồng nghĩa internet dùng được.
fun Context.networkStatusFlow(): Flow<Boolean> = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isValidated(network: Network?): Boolean {
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Chờ onCapabilitiesChanged để dùng capabilities do hệ thống cung cấp.
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            )
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }

    trySend(isValidated(connectivityManager.activeNetwork))
    connectivityManager.registerDefaultNetworkCallback(callback)

    awaitClose {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}.buffer(Channel.CONFLATED).distinctUntilChanged()
```

**Các method quan trọng trong callbackFlow:**

| Method | Mô tả |
|--------|--------|
| `trySend(value)` | Gửi giá trị vào flow (non-blocking, non-suspending) |
| `send(value)` | Gửi giá trị (suspending - sẽ đợi nếu buffer đầy) |
| `close()` | Đóng flow bình thường |
| `close(cause)` | Đóng flow với exception |
| `awaitClose { }` | Suspend đến khi channel đóng/collection bị hủy, rồi cleanup; không block thread |

Với listener dài hạn, đặt `awaitClose { unregister(...) }` để giữ registration và cleanup. Nếu block `callbackFlow` trả về khi channel vẫn mở, thư viện ném `IllegalStateException`. Adapter hữu hạn có thể chủ động `close()` và cleanup theo cấu trúc phù hợp, nên không mô tả `awaitClose` là bắt buộc cho mọi biến thể.

`trySend` trả `ChannelResult`: có thể thất bại khi channel đóng hoặc buffer đầy. Các ví dụ TextWatcher/sensor/network chọn conflation vì chỉ cần **state mới nhất**; đây không phải cách xử lý các sự kiện bắt buộc nhận đủ. Với sự kiện không được mất, định nghĩa capacity/backpressure và kiểm tra kết quả `trySend`; không tạo một `launch` cho từng callback rồi cho rằng đã giải quyết được buffer.

`callbackFlow` là cold: hai collector đăng ký hai listener nếu không chia sẻ bằng `shareIn`/`stateIn`. Đăng ký/gỡ TextWatcher phải trên Main; không áp dụng `flowOn(IO)` cho toàn adapter UI. Trạng thái network chỉ là tín hiệu cho UI; request Retrofit vẫn phải xử lý lỗi thực tế.

---

## 7. Sử dụng Coroutines với Retrofit

### 7.1. Setup Retrofit với Coroutines

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
```

### 7.2. Định nghĩa API Interface

```kotlin
// Retrofit tự động hỗ trợ suspend function từ phiên bản 2.6.0+
interface UserApiService {

    // ✅ Suspend function - trả về trực tiếp object
    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): UserResponse

    // ✅ Suspend function - trả về Response wrapper (để kiểm tra HTTP status)
    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): Response<List<UserResponse>>

    // ✅ POST request
    @POST("users")
    suspend fun createUser(@Body request: CreateUserRequest): UserResponse

    // ✅ PUT request
    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): UserResponse

    // ✅ DELETE request
    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): Response<Unit>
}
```

### 7.3. Tạo Retrofit Instance

```kotlin
object RetrofitClient {

    private const val BASE_URL = "https://api.example.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: UserApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(UserApiService::class.java)
}
```

### 7.4. Model và Repository: network-first

Các model sau dùng chung cho ví dụ Retrofit–Room ở mục 7–8. `UserApiService` dùng hợp đồng ở mục 7.2; `UserDao`/`UserEntity` ở mục 8.

```kotlin
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String?,
    val isActive: Boolean
)
data class UserResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = true
)
data class CreateUserRequest(val fullName: String, val email: String)
data class UpdateUserRequest(val fullName: String, val email: String)

fun UserResponse.toUser() = User(id, fullName, email, avatarUrl, isActive)
fun UserEntity.toUser() = User(id, fullName, email, avatarUrl, isActive)
fun User.toEntity() = UserEntity(
    id = id, fullName = fullName, email = email,
    avatarUrl = avatarUrl, isActive = isActive
)

// Ưu tiên API, fallback cache khi lỗi I/O.
// Tên riêng để không trùng UserRepository offline-first ở mục 8.
class NetworkFirstUserRepository(
    private val api: UserApiService,
    private val dao: UserDao
) {
    suspend fun getUser(userId: String): Result<User> = safeApiCall {
        val user = try {
            api.getUser(userId).toUser()
        } catch (e: IOException) {
            // Chính sách: fallback khi lỗi I/O, không che HTTP 401/403/404.
            dao.getUserById(userId)?.toUser() ?: throw e
        }
        dao.insertUser(user.toEntity())
        user
    }
}
```

**Hai kiểu trả về của Retrofit cần xử lý khác nhau:**

| Khai báo | HTTP không thành công | Lỗi network/deserialize |
|----------|------------------------|--------------------------|
| `suspend fun getUser(): UserResponse` | Ném `HttpException` | Ném exception |
| `suspend fun getUsers(): Response<List<UserResponse>>` | Trả Response, phải kiểm tra `isSuccessful` | Vẫn có thể ném exception |

HTTP thành công có body null cần chính sách riêng; không mặc định biến mọi body null thành danh sách rỗng rồi xóa cache. Retrofit hỗ trợ hủy request đang chạy khi coroutine bị cancel; ứng dụng vẫn phải giữ nguyên `CancellationException`. Hủy request không có nghĩa server chắc chắn chưa thực hiện thao tác ghi.

### 7.5. ViewModel và tải đồng thời

```kotlin
sealed interface UserUiState {
    data object Loading : UserUiState
    data class Success(val user: User) : UserUiState
    data class Error(val message: String) : UserUiState
}

class UserViewModel(
    private val repository: NetworkFirstUserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun loadUser(userId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            repository.getUser(userId)
                .onSuccess { _uiState.value = UserUiState.Success(it) }
                .onFailure {
                    _uiState.value = UserUiState.Error(it.message ?: "Không tải được user")
                }
        }
    }
}

// Các hàm fetch... là suspend, main-safe do tầng data cung cấp.
// Dashboard cần thành công toàn bộ: xử lý lỗi ngoài coroutineScope.
suspend fun loadDashboard(): Dashboard = coroutineScope {
    val user = async { fetchUser() }
    val orders = async { fetchOrders() }
    val notifications = async { fetchNotifications() }
    Dashboard(user.await(), orders.await(), notifications.await())
}

viewModelScope.launch {
    try {
        val dashboard = loadDashboard()
        renderDashboard(dashboard)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        showDashboardError(e)
    }
}
```

Một request thất bại trong `coroutineScope` sẽ cancel các request còn lại và ném lỗi về caller. Muốn hiển thị từng phần độc lập: dùng `supervisorScope`, xử lý kết quả từng `async` (mục 3.5). **Chỉ bọc một await trong coroutine cha không đủ để chặn lan truyền lỗi.**

### 7.6. Utility xử lý lỗi và retry có giới hạn

```kotlin
// Không tự đổi dispatcher: block cần tự bảo đảm main-safety.
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

// Exponential backoff: 500ms, 1000ms, 2000ms... giữa các lần thử.
// maxRetries = 2 nghĩa là tối đa 3 lần gọi.
suspend fun <T> retryNetwork(
    maxRetries: Int = 2,
    initialDelayMillis: Long = 500,
    block: suspend () -> T
): T {
    require(maxRetries in 0..10)
    require(initialDelayMillis in 1L..60_000L)
    var waitMillis = initialDelayMillis
    repeat(maxRetries + 1) { attempt ->
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val retryable = e is IOException ||
                (e is HttpException && e.code() in 500..599)
            if (!retryable || attempt == maxRetries) throw e
            delay(waitMillis) // Không chờ sau lần thử cuối.
            waitMillis = (waitMillis * 2).coerceAtMost(60_000)
        }
    }
    error("Unreachable")
}

// Retry quanh request GET, không quanh toàn Flow observe database.
suspend fun fetchUserWithRetry(api: UserApiService, userId: String): UserResponse =
    retryNetwork { api.getUser(userId) }
```

Production cần chọn lỗi transient, jitter và `Retry-After`. Không tự retry mọi 4xx hoặc POST: thao tác ghi có thể đã thành công ở server dù client mất kết nối. Chỉ retry khi semantics/idempotency cho phép. Không retry cancellation.

---

## 8. Sử dụng Coroutines với Room Database

### 8.1. Setup Room với Coroutines

```kotlin
// build.gradle.kts
dependencies {
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")    // ← Coroutines support
    kapt("androidx.room:room-compiler:$roomVersion")
    // Hoặc dùng KSP thay kapt (khuyến khích):
    // ksp("androidx.room:room-compiler:$roomVersion")
}
```

### 8.2. Entity

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    val email: String,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
```

### 8.3. DAO với Coroutines

```kotlin
@Dao
interface UserDao {

    // ===== SUSPEND FUNCTIONS (One-shot queries) =====

    // Insert - suspend function
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    // Update - suspend function
    @Update
    suspend fun updateUser(user: UserEntity): Int  // Trả về số row bị ảnh hưởng

    // Delete - suspend function
    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    // Query - suspend function (one-shot, lấy 1 lần)
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY full_name ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE is_active = 1 ORDER BY created_at DESC")
    suspend fun getActiveUsers(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    // ===== FLOW (Observable queries - tự động emit khi data thay đổi) =====

    // Flow - tự động phát giá trị mới khi bảng users thay đổi
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserFlowById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY full_name ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE is_active = 1 ORDER BY created_at DESC")
    fun getActiveUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    fun getUserCountFlow(): Flow<Int>

    // ===== TRANSACTION =====

    @Transaction
    suspend fun replaceAllUsers(users: List<UserEntity>) {
        deleteAllUsers()
        insertUsers(users)
    }

    @Transaction
    suspend fun deactivateAndArchive(userId: String) {
        val user = getUserById(userId) ?: return
        updateUser(user.copy(isActive = false))
        // Thêm vào bảng archive...
    }
}
```

### 8.4. Database

```kotlin
@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
```

Trong project Android, cấu hình plugin KSP/kapt trước khi dùng dependency compiler và thư mục xuất schema khi `exportSchema = true`. Khi tăng version database, cung cấp migration phù hợp; không dùng destructive migration mặc định cho dữ liệu cần giữ. Singleton kiểm tra lại INSTANCE bên trong synchronized để tránh tạo hai database khi nhiều thread cùng gọi.

### 8.5. Repository với Room + Retrofit: observe riêng, refresh riêng

**Single source of truth:** UI đọc từ Room. Network refresh chỉ ghi kết quả thành công vào Room; Room Flow phát dữ liệu cho UI. Không chặn việc observe cache bằng một request trong `onStart`.

```text
Room Flow ───────────────────────────→ ViewModel StateFlow → UI
   ↑                                             ↑
   └── ghi transaction ← Retrofit refresh ─── loading/error
```

```kotlin
class UserRepository(
    private val api: UserApiService,
    private val dao: UserDao,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    fun observeUsers(): Flow<List<User>> = dao.getAllUsersFlow()
        .map { entities -> entities.map { it.toUser() } }
        .distinctUntilChanged()
        .flowOn(computeDispatcher) // Mapping lớn là CPU work.

    fun observeUser(userId: String): Flow<User?> = dao.getUserFlowById(userId)
        .map { it?.toUser() }
        .distinctUntilChanged()

    suspend fun refreshUsers(): Result<Unit> = safeApiCall {
        val response = api.getUsers(page = 1)
        if (!response.isSuccessful) throw HttpException(response)
        val body = response.body()
            ?: throw IllegalStateException("HTTP thành công nhưng thiếu body")
        val entities = withContext(computeDispatcher) {
            body.map { it.toUser().toEntity() }
        }
        dao.replaceAllUsers(entities) // @Transaction: xóa + insert cùng giao dịch.
    }

    suspend fun refreshUser(userId: String): Result<Unit> = safeApiCall {
        dao.insertUser(api.getUser(userId).toUser().toEntity())
    }

    suspend fun createUser(request: CreateUserRequest): Result<User> = safeApiCall {
        val user = api.createUser(request).toUser()
        dao.insertUser(user.toEntity())
        user
    }

    suspend fun deleteUser(userId: String): Result<Unit> = safeApiCall {
        val response = api.deleteUser(userId) // Response<Unit>
        if (!response.isSuccessful) throw HttpException(response)
        dao.deleteUserById(userId) // Chỉ xóa local khi server báo thành công.
    }
}
```

**Phạm vi ví dụ:** `refreshUsers()` tải trang 1 rồi thay toàn bảng; dataset demo phải nằm trong một trang. Với pagination thật, không xóa toàn bảng sau mỗi trang: thiết kế upsert, remote keys và có thể dùng Paging `RemoteMediator`. DTO/Entity/domain là các lớp khác nhau, cần chuyển đổi rõ ràng.

**Room Flow không có nghĩa “chỉ emit khi kết quả khác”.** Room có thể query lại khi bảng liên quan bị invalidated, kể cả thay đổi không ảnh hưởng kết quả query. Dùng `distinctUntilChanged` để tránh phát lại kết quả bằng nhau. DAO trả `Flow` không cần thêm `suspend`; DAO one-shot mới dùng `suspend`. Không gọi network bên trong transaction Room.

Server/local không tạo thành một distributed transaction: server có thể thành công nhưng ghi local lỗi. App cần chiến lược refresh/reconcile; không retry toàn bộ thao tác ghi khi chưa xét idempotency. Cancel coroutine cũng không hoàn tác những thay đổi đã commit.

### 8.6. ViewModel sử dụng Room + Retrofit

```kotlin
data class UserListUiState(
    val users: List<User> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class UserListViewModel(
    private val repository: UserRepository
) : ViewModel() {
    // Chỉ chứa trạng thái thao tác; danh sách thật đến từ Room.
    private val status = MutableStateFlow(UserListUiState())
    private var refreshJob: Job? = null

    val uiState: StateFlow<UserListUiState> = combine(
        repository.observeUsers(), status
    ) { users, currentStatus ->
        currentStatus.copy(users = users)
    }
        .catch { e ->
            // Lỗi observe/query; refresh error xử lý riêng bên dưới.
            emit(UserListUiState(error = e.message ?: "Không đọc được database"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserListUiState()
        )

    init { refresh() }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            status.update { it.copy(isRefreshing = true, error = null) }
            try {
                repository.refreshUsers().onFailure { error ->
                    status.update {
                        it.copy(error = error.message ?: "Không refresh được dữ liệu")
                    }
                }
            } finally {
                status.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun dismissError() {
        status.update { it.copy(error = null) }
    }
}
```

UI có thể hiển thị cache đồng thời với progress/error. Refresh thất bại không xóa danh sách Room. Vì refresh tách khỏi collection, subscriber mới không tự kích hoạt request. `WhileSubscribed` dừng observe khi không còn UI collector; refresh ở `viewModelScope` vẫn chạy đến khi hoàn tất hoặc ViewModel bị clear.

`catch` trên xử lý một lỗi terminal của observe Flow rồi upstream kết thúc. Nếu cần tự phục hồi sau lỗi database, thiết kế retry/restart riêng; cập nhật `status` không làm upstream đã kết thúc tự chạy lại. Lỗi network không làm Flow observe database kết thúc.

**Search từ Room** (bổ sung method vào `UserDao` và `UserRepository` tương ứng):

```kotlin
// UserDao
@Query("SELECT * FROM users WHERE full_name LIKE '%' || :query || '%' ORDER BY full_name ASC")
fun searchUsersFlow(query: String): Flow<List<UserEntity>>

// UserRepository
fun searchUsers(query: String): Flow<List<User>> = dao.searchUsersFlow(query)
    .map { entities -> entities.map { it.toUser() } }
    .distinctUntilChanged()
    .flowOn(computeDispatcher)

// Trong ViewModel có repository: UserRepository
private val searchQuery = MutableStateFlow("")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
val searchResults: StateFlow<List<User>> = searchQuery
    .debounce(300)
    .flatMapLatest { query ->
        if (query.isBlank()) repository.observeUsers()
        else repository.searchUsers(query.trim())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

fun onSearchQueryChanged(query: String) {
    searchQuery.value = query
}
```

Search demo dùng dữ liệu local. Search bằng Retrofit thì inner Flow gọi request suspend, xử lý lỗi **bên trong inner Flow** để một request lỗi không làm toàn pipeline ngừng nhận query mới. Với `LIKE`, `%`/`_` là wildcard; tìm literal cần escape theo yêu cầu.

---

## 9. Best Practices & Anti-patterns

### 9.1. ✅ Best Practices

```kotlin
// 1. ✅ Sử dụng viewModelScope/lifecycleScope thay vì GlobalScope
class MyViewModel : ViewModel() {
    fun loadData() {
        viewModelScope.launch {  // ✅ Tự cancel khi ViewModel clear
            // ...
        }
    }
}

// 2. ✅ Main-safety: Suspend functions nên safe để gọi từ Main thread
class Repository(
    private val api: BlockingApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // API đồng bộ blocking: data layer chịu trách nhiệm chuyển sang IO.
    suspend fun getData(): Data = withContext(ioDispatcher) {
        api.fetchDataBlocking()
    }
}

// 3. ✅ Sử dụng structured concurrency
suspend fun loadAll() = coroutineScope {
    val a = async { loadA() }
    val b = async { loadB() }
    combine(a.await(), b.await())
}

// 4. ✅ Xử lý exception đúng cách
viewModelScope.launch {
    try {
        val data = repository.getData()
        _state.value = UiState.Success(data)
    } catch (e: CancellationException) {
        throw e // Không nuốt cancellation; nếu bắt phải ném lại.
    } catch (e: Exception) {
        _state.value = UiState.Error(e.message)
    }
}

// 5. ✅ Dùng repeatOnLifecycle để collect Flow
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> updateUI(state) }
    }
}

// 6. ✅ Dùng SupervisorJob cho independent child coroutines
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```

### 9.2. ❌ Anti-patterns

```kotlin
// 1. ❌ KHÔNG dùng GlobalScope
GlobalScope.launch {  // ❌ Không tự cancel theo UI lifecycle; dễ giữ UI quá lâu
    updateUI()
}

// 2. ❌ KHÔNG block Main thread
viewModelScope.launch {
    val data = heavyComputation()  // ❌ Chạy trên Main thread!
    // Phải dùng withContext(Dispatchers.Default)
}

// 3. ❌ KHÔNG nuốt CancellationException
try {
    suspendingFunction()
} catch (e: Exception) {  // ❌ Catch cả CancellationException
    // Coroutine không thể cancel đúng cách
}
// ✅ Fix:
try {
    suspendingFunction()
} catch (e: CancellationException) {
    throw e  // Re-throw
} catch (e: Exception) {
    handleError(e)
}

// 4. ❌ KHÔNG collect Flow trực tiếp trong lifecycleScope mà không có lifecycle awareness
lifecycleScope.launch {
    viewModel.uiState.collect {  // ❌ Tiếp tục collect khi app ở background
        updateUI(it)             //    → Waste resources, potential crashes
    }
}

// 5. ❌ KHÔNG tạo coroutine trong suspend function mà không sử dụng coroutineScope
suspend fun loadData() {
    // ❌ Không có structured concurrency
    CoroutineScope(Dispatchers.IO).launch {
        fetchData()
    }
}
// ✅ Fix:
suspend fun loadData() = coroutineScope {
    launch(Dispatchers.IO) {
        fetchData()
    }
}

// 6. ❌ KHÔNG dùng suspend function không cần thiết
suspend fun getFullName(first: String, last: String): String {
    return "$first $last"  // ❌ Không có gì cần suspend ở đây
}
// ✅ Fix: Bỏ suspend keyword
fun getFullName(first: String, last: String): String = "$first $last"
```

### 9.3. Exception Handling

```kotlin
// CoroutineExceptionHandler cho uncaught exceptions
val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Log.e("Coroutine", "Uncaught exception", throwable)
    // Report to Crashlytics, etc.
}

viewModelScope.launch(exceptionHandler) {
    riskyOperation()
}

// Chặn lỗi của nhóm tác vụ: catch ngoài coroutineScope.
viewModelScope.launch {
    try {
        coroutineScope {
            val deferred = async<String> {
                throw IllegalStateException("Oops!")
            }
            deferred.await()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IllegalStateException) {
        // coroutineScope đã kết thúc; lỗi được xử lý tại caller.
        Log.e("Coroutine", "Không hoàn tất tác vụ", e)
    }
}
```

`CoroutineExceptionHandler` chỉ nhận **uncaught exceptions**, dùng để logging/reporting sau khi coroutine đã thất bại; không phục hồi tác vụ và không thay try/catch nghiệp vụ. `async` giữ lỗi trong Deferred, nhưng lỗi của child dưới Job thông thường vẫn lan lên cha. Dưới supervisor, caller xử lý lỗi tại `await`. Một `launch` trực tiếp dưới `SupervisorJob` vẫn cần xử lý lỗi riêng hoặc handler, nếu không exception có thể làm app crash.

### 9.4. Checklist trước khi dùng trong ứng dụng

- Scope có owner và điểm kết thúc rõ ràng; không tạo scope độc lập tùy tiện trong suspend function.
- Hàm suspend main-safe; dispatcher của code blocking/CPU được truyền vào nơi cần kiểm thử.
- Không nuốt cancellation; vòng lặp CPU dài có điểm kiểm tra hủy.
- UI collect theo lifecycle đúng owner; View của Fragment dùng `viewLifecycleOwner`.
- State expose read-only và cập nhật immutable/atomic; SharedFlow không bảo đảm xử lý event đúng một lần.
- Callback adapter có buffer policy, cleanup và hợp đồng hoàn tất một lần.
- Retrofit trả Response phải kiểm tra HTTP; cache vẫn hiển thị khi refresh lỗi.
- Nhóm tác vụ có quy tắc thành công toàn bộ hoặc từng phần, xử lý lỗi tương ứng.

---

## 10. Bài tập thực hành

### Bài 1: Coroutine Basics
Tạo một ứng dụng đếm ngược (countdown timer) sử dụng `StateFlow` và `viewModelScope.launch`:

- Hiển thị đếm ngược từ 10 đến 0
- Có nút Start/Pause/Reset
- Sử dụng `delay()` cho mỗi giây
- **Nghiệm thu:** bấm Start nhiều lần không tạo nhiều timer; Pause/Reset hủy job cũ; ViewModel bị clear thì timer dừng; state không giảm dưới 0.

### Bài 2: Parallel Loading
Tạo màn hình Dashboard load 3 loại data song song:

- User Profile (từ API)
- Recent Orders (từ API)
- Notifications (từ API)
- Sử dụng `async` để load song song
- Hiển thị loading state và error handling
- **Nghiệm thu:** với fake request 2s/3s/1s, tổng thời gian gần 3s thay vì 6s; một request lỗi thì các request còn lại bị hủy theo chính sách all-or-nothing.
- **Mở rộng:** đổi sang `supervisorScope`, hiển thị phần thành công và phần lỗi riêng; không nuốt cancellation.

### Bài 3: Search với Flow
Implement chức năng search sử dụng:

- `callbackFlow` để wrap `EditText.TextWatcher`
- `debounce(300)` để tránh gọi API quá nhiều
- `distinctUntilChanged` để tránh trùng query
- `flatMapLatest` để cancel search cũ khi có query mới
- **Nghiệm thu:** listener được gỡ khi View dừng collect; query hiện tại được phát khi collect lại; gõ nhanh không gọi API cho mọi ký tự; request cũ bị hủy khi query mới đi qua debounce.
- **Lỗi cần thử:** một request thất bại, query tiếp theo vẫn được xử lý; không để lỗi đó kết thúc toàn pipeline.

### Bài 4: Offline-first Architecture
Tạo ứng dụng hiển thị danh sách Users:

- Sử dụng Room DAO với `Flow` để observe data
- Fetch từ Retrofit khi có network
- Hiển thị cached data khi offline
- Sử dụng `UserListUiState(users, isRefreshing, error)` để hiển thị cache cùng progress/error
- **Nghiệm thu:** cache xuất hiện mà không đợi network; HTTP 500 giữ nguyên cache; DELETE HTTP 403 không xóa local hoặc báo thành công; xóa + insert local nằm trong một transaction.
- **Lifecycle:** xoay màn hình không tự tạo thêm refresh chỉ vì đăng ký collector mới; vào background dừng UI collection theo chính sách đã chọn.

### Bài 5: callbackFlow
Tạo Flow wrapper cho:

- Firebase Realtime Database listener
- Location updates (FusedLocationProviderClient)
- Network connectivity status (ConnectivityManager)
- **Nghiệm thu:** mỗi collection đăng ký/gỡ đúng listener; không tiếp tục phát dữ liệu vào UI đã bị hủy; chọn buffer theo yêu cầu giữ mọi event hay chỉ state mới nhất; không giữ SensorEvent có thể bị framework tái sử dụng.

### Bài 6: Chuyển callback một kết quả và cancellation

- Tạo fake API callback một lần, có hàm `cancel()`.
- Viết hai adapter bằng `suspendCoroutine` và `suspendCancellableCoroutine`; so sánh khi caller cancel trước khi có kết quả.
- **Nghiệm thu:** adapter cancellable gọi `cancel()` của API; callback đến muộn không cập nhật UI; callback success/error bị gọi lặp không resume continuation hai lần.

### Bài 7: StateFlow và SharedFlow khi UI dừng

- Dừng collector, thay đổi StateFlow, phát một event vào `MutableSharedFlow(replay = 0)`, rồi đăng ký lại.
- **Nghiệm thu:** giải thích tại sao state mới nhất còn nhưng event đã mất; thử `replay = 1` và chỉ ra nguy cơ xử lý lại. Chọn state/event theo hành vi sản phẩm cần.

### Câu hỏi kiểm tra cuối buổi

1. Một hàm có `suspend` nhưng gọi `Thread.sleep()` trên Main có gây đứng UI không? Vì sao?
2. ViewModel tồn tại qua xoay màn hình, nhưng View của Fragment bị hủy: collect UI nên thuộc scope nào?
3. Hai `async` được tạo trong `launch`; một cái throw trước `await`. Cha có thể bị hủy không?
4. Khi không có subscriber, `extraBufferCapacity = 64` có giữ 64 sự kiện SharedFlow cho UI quay lại không?
5. Vì sao `trySend` có thể thất bại? Khi nào dùng conflation là hợp lý?
6. Retrofit trả `Response<Unit>` với HTTP 403: request suspend đã trả về, vậy thao tác có thành công không?

**Đáp án ngắn:** (1) Có, suspend không biến blocking thành non-blocking. (2) `viewLifecycleOwner` và `repeatOnLifecycle`. (3) Có dưới Job cha thông thường; catch ngoài `coroutineScope` hoặc chọn supervision phù hợp. (4) Không; khi không có subscriber chỉ replay giữ giá trị. (5) Channel đóng/đầy; conflation phù hợp state mới nhất. (6) Không; phải kiểm tra HTTP status.

---

## 11. Kiểm thử Coroutines và Flow

### 11.1. Dependency và virtual time

```kotlin
// Module Android: phiên bản coroutines-test phải khớp core/android.
dependencies {
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("junit:junit:4.13.2")
}
```

`runTest` cho phép kiểm soát thời gian giả lập trên test dispatcher. `runCurrent()` chạy task tại thời điểm hiện tại; `advanceTimeBy(n)` tiến thời gian n ms; sau đó gọi `runCurrent()` để chạy task đúng tại mốc mới. `advanceUntilIdle()` chạy đến khi scheduler hết việc: không dùng tùy tiện với producer lặp vô hạn. Delay trên dispatcher thật như IO/Default không được tự bỏ qua; truyền `StandardTestDispatcher(testScheduler)` vào code cần test.

### 11.2. Kiểm tra cancellation, debounce và cleanup listener

Các test dưới đây dùng JUnit 4, không cần Android UI. Đặt trong test source set. `safeApiCall` lấy từ mục 7.6; `FakeEventSource` chỉ dùng trong test.

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class FakeEventSource {
    var listener: ((Int) -> Unit)? = null
        private set
    var removed = 0
        private set

    fun register(callback: (Int) -> Unit): () -> Unit {
        check(listener == null)
        listener = callback
        return {
            listener = null
            removed++
        }
    }

    fun emit(value: Int) { listener?.invoke(value) }
}

fun FakeEventSource.values(): Flow<Int> = callbackFlow {
    val unregister = register { value -> trySend(value) }
    awaitClose { unregister() }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CoroutineExamplesTest {
    @Test
    fun safeApiCallDoesNotConvertCancellationToFailure() = runTest {
        try {
            safeApiCall<Unit> { throw CancellationException("Owner bị hủy") }
            fail("Cancellation phải được ném lại")
        } catch (expected: CancellationException) {
            assertEquals("Owner bị hủy", expected.message)
        }
    }

    @Test
    fun childFailureCancelsSiblingAndReachesScopeCaller() = runTest {
        val siblingStarted = CompletableDeferred<Unit>()
        var siblingCleanedUp = false
        try {
            coroutineScope {
                launch {
                    try {
                        siblingStarted.complete(Unit)
                        awaitCancellation()
                    } finally {
                        siblingCleanedUp = true
                    }
                }
                async<Unit> {
                    siblingStarted.await()
                    throw IllegalStateException("API lỗi")
                }.await()
            }
            fail("Scope phải ném lỗi về caller")
        } catch (expected: IllegalStateException) {
            assertEquals("API lỗi", expected.message)
        }
        assertTrue(siblingCleanedUp)
        assertTrue(currentCoroutineContext().isActive)
    }

    @Test
    fun debounceEmitsOnlyAfterQuietPeriod() = runTest {
        val queries = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val received = mutableListOf<String>()
        backgroundScope.launch {
            queries.debounce(300).collect { received.add(it) }
        }
        runCurrent() // Đăng ký collector trước khi emit (replay = 0).
        queries.emit("k")
        runCurrent()
        advanceTimeBy(100)
        queries.emit("ko")
        runCurrent()
        advanceTimeBy(299)
        runCurrent()
        assertTrue(received.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("ko"), received)
    }

    @Test
    fun callbackFlowUnregistersWhenCollectorIsCancelled() = runTest {
        val source = FakeEventSource()
        val received = mutableListOf<Int>()
        val job = backgroundScope.launch {
            source.values().collect { received.add(it) }
        }
        runCurrent()
        assertNotNull(source.listener)
        source.emit(7)
        runCurrent()
        assertEquals(listOf(7), received)
        job.cancelAndJoin()
        assertNull(source.listener)
        assertEquals(1, source.removed)
    }
}
```

### 11.3. Test ViewModel và StateFlow

- Local JVM test không có Android Main dispatcher thực: gọi `Dispatchers.setMain(testDispatcher)` **trước khi tạo ViewModel**; luôn `resetMain()` trong teardown/finally. Các test dispatcher cần dùng chung scheduler.
- Ưu tiên kiểm tra `state.value` nếu chỉ cần trạng thái cuối. StateFlow có conflation nên không giả định collector nhận mọi trạng thái trung gian.
- Với `stateIn(WhileSubscribed/Lazily)`, cần collector đang hoạt động để upstream bắt đầu; chỉ đọc `.value` không kích hoạt nó. Dùng `backgroundScope` cho collection dài hạn, tránh test chờ Flow hot kết thúc.
- Fake API/DAO nên cho phép điều khiển thành công, lỗi HTTP/I/O và cancellation. Kiểm tra cache giữ nguyên khi refresh lỗi, HTTP lỗi không xóa local và request bị hủy theo owner.
- Những test ở mục 11.2 kiểm tra hành vi coroutine thuần Kotlin; lifecycle, binding và Room transaction cần kiểm thử Android/integration tương ứng, không thể kết luận chỉ từ các test này.

---

## Tài liệu tham khảo

| Nguồn | Link |
|-------|------|
| Kotlin Coroutines Basics | https://kotlinlang.org/docs/coroutines-basics.html |
| Kotlin Flow | https://kotlinlang.org/docs/coroutines-flow.html |
| Coroutine Context & Dispatchers | https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html |
| Android Coroutines | https://developer.android.com/kotlin/coroutines |
| StateFlow & SharedFlow | https://developer.android.com/kotlin/flow/stateflow-and-sharedflow |
| Architecture Components + Coroutines | https://developer.android.com/topic/libraries/architecture/coroutines |
| Composing Suspending Functions | https://kotlinlang.org/docs/composing-suspending-functions.html |
| Cancellation and Timeouts | [Kotlin: cancellation](https://kotlinlang.org/docs/cancellation-and-timeouts.html) |
| Coroutine Exception Handling | [Kotlin: exception handling](https://kotlinlang.org/docs/exception-handling.html) |
| withContext và structured concurrency | [Kotlin API: withContext](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html) |
| callbackFlow và cleanup | [Kotlin API: callbackFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html) |
| Continuation và cancellation race | [Kotlin API: suspendCancellableCoroutine](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html) |
| SharedFlow replay/buffer | [Kotlin API: SharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/) |
| Dispatcher IO parallelism | [Kotlin API: Dispatchers.IO](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-i-o.html) |
| Main-safety và cancellation trong Android | [Android: coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices) |
| Room suspend/observable queries | [Android: asynchronous DAO queries](https://developer.android.com/training/data-storage/room/async-queries) |
| Kiểm thử Coroutines | [Android: testing coroutines](https://developer.android.com/kotlin/coroutines/test) |
| Kiểm thử Flow/StateFlow | [Android: testing flows](https://developer.android.com/kotlin/flow/test) |
| SensorEvent lifetime | [Android: SensorEventListener](https://developer.android.com/reference/android/hardware/SensorEventListener) |
| Connectivity callback và validation | [Android: read network state](https://developer.android.com/develop/connectivity/network-ops/reading-network-state) |

---

> **Ghi chú:** Các dependency được cố định để minh họa, không được gọi là “phiên bản mới nhất”. Khi triển khai, dùng bộ phiên bản tương thích của project và kiểm tra tài liệu chính thức. Ví dụ minh họa có thể cần imports/opt-in và code UI/model của ứng dụng như đã nêu đầu bài.

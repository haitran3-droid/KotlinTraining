# Kotlin Coroutines - Bài giảng toàn diện

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
| Thread tốn tài nguyên (~1MB/thread) | Coroutine nhẹ (~few KB) |
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

> **Lưu ý quan trọng:** Keyword `suspend` không làm function chạy trên background thread. Nó chỉ đánh dấu rằng function có thể tạm dừng. Việc chạy trên thread nào phụ thuộc vào **Dispatcher**.

### 1.4. Thêm thư viện Coroutines vào project

```kotlin
// build.gradle.kts (Module)
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
```

---

## 2. Coroutine Scope

### 2.1. CoroutineScope là gì?

**CoroutineScope** định nghĩa vòng đời (lifecycle) của các coroutines được launch bên trong nó. Mọi coroutine phải được launch trong một scope. Khi scope bị hủy, tất cả coroutines con cũng bị hủy theo → đây chính là **Structured Concurrency**.

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
// GlobalScope tồn tại suốt vòng đời ứng dụng, dễ gây memory leak
GlobalScope.launch {
    // Coroutine này sẽ chạy cho đến khi hoàn thành
    // hoặc process bị kill - KHÔNG tự động hủy
    val data = fetchData()
    updateUI(data)  // Activity có thể đã bị destroy!
}
```

**Vấn đề với GlobalScope:**
- Không gắn với lifecycle nào → không tự hủy
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

#### 2.2.4. Custom Scope

Khi cần scope với lifecycle tùy chỉnh, bạn có thể tạo custom scope:

```kotlin
class MyCustomManager {

    // Tạo custom scope với SupervisorJob
    // SupervisorJob: nếu 1 child coroutine fail, các child khác không bị ảnh hưởng
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineName("MyCustomScope")
    )

    fun doWork() {
        scope.launch {
            // Coroutine chạy trong custom scope
            val result = heavyComputation()
            processResult(result)
        }
    }

    fun doAnotherWork() {
        scope.launch {
            // Nếu coroutine này fail, coroutine ở doWork() vẫn chạy bình thường
            // nhờ SupervisorJob
            riskyOperation()
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
// Job thông thường: Nếu 1 child fail → tất cả children bị cancel
val scope1 = CoroutineScope(Job() + Dispatchers.Main)

// SupervisorJob: Nếu 1 child fail → chỉ child đó bị cancel, các child khác tiếp tục
val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```

```
Job:                            SupervisorJob:
┌──────────────┐               ┌──────────────┐
│   Parent Job │               │ SupervisorJob │
├──────┬───────┤               ├──────┬────────┤
│Child1│Child2 │               │Child1│Child2  │
│  ✗   │  ✗    │ ← cả 2 fail  │  ✗   │  ✓    │ ← chỉ Child1 fail
└──────┴───────┘               └──────┴────────┘
```

### 2.3. So sánh các loại Scope

| Scope | Dispatcher mặc định | Lifecycle gắn với | Tự động cancel khi |
|-------|---------------------|--------------------|--------------------|
| `GlobalScope` | `Dispatchers.Default` | Application | Process bị kill |
| `viewModelScope` | `Dispatchers.Main.immediate` | ViewModel | `onCleared()` |
| `lifecycleScope` | `Dispatchers.Main.immediate` | Activity/Fragment | `onDestroy()` |
| `Custom Scope` | Tùy cấu hình | Tùy quản lý | `scope.cancel()` |
| `rememberCoroutineScope` | `Dispatchers.Main` | Composable | Composable leave composition |

---

## 3. Coroutine Builders: launch, async, withContext

### 3.1. launch - "Fire and forget"

`launch` bắt đầu một coroutine mới mà **không trả về kết quả**. Trả về `Job` để quản lý coroutine.

```kotlin
class NewsViewModel : ViewModel() {

    fun refreshNews() {
        // launch trả về Job - không quan tâm kết quả trả về
        val job: Job = viewModelScope.launch {
            try {
                val news = newsRepository.fetchLatestNews()
                _newsState.value = news
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
// ❌ Tuần tự - Tổng thời gian = 2 + 3 = 5 giây
suspend fun loadSequential() {
    val user = userRepository.getUser()          // 2 giây
    val posts = postRepository.getUserPosts()     // 3 giây
    // Tổng: 5 giây
}

// ✅ Song song với async - Tổng thời gian = max(2, 3) = 3 giây
suspend fun loadParallel() = coroutineScope {
    val user = async { userRepository.getUser() }          // 2 giây
    val posts = async { postRepository.getUserPosts() }     // 3 giây
    // Tổng: 3 giây (chạy song song)
    ProcessData(user.await(), posts.await())
}
```

### 3.3. withContext - Chuyển đổi context

`withContext` chuyển đổi dispatcher (thread) cho một khối code. **Không tạo coroutine mới**, chỉ thay đổi context thực thi.

```kotlin
class UserRepository(
    private val api: ApiService,
    private val userDao: UserDao
) {

    // Sử dụng withContext để đảm bảo chạy trên đúng thread
    suspend fun getUser(userId: String): User {
        // Gọi network trên IO thread
        val networkUser = withContext(Dispatchers.IO) {
            api.fetchUser(userId)
        }

        // Lưu vào database trên IO thread
        withContext(Dispatchers.IO) {
            userDao.insert(networkUser.toEntity())
        }

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
| Mục đích | Fire-and-forget | Tính toán song song | Chuyển context |
| Blocking? | Không | Không (cho đến `.await()`) | Có (suspend) |
| Use case | Side effects, UI update | Song song nhiều tác vụ | Chuyển thread |
| Tạo coroutine mới? | Có | Có | Không |

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
    
    val resultA = try { a.await() } catch (e: Exception) { null }
    val resultB = try { b.await() } catch (e: Exception) { null }
    Pair(resultA, resultB)
}
```

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

Tối ưu cho **I/O operations**: network requests, đọc/ghi file, database. Sử dụng thread pool có thể mở rộng (mặc định tối đa 64 threads).

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

Tối ưu cho **CPU-intensive operations**: tính toán, parsing, sorting. Sử dụng thread pool với số thread = số CPU cores.

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
// ⚠️ Chỉ dùng trong testing hoặc trường hợp đặc biệt
launch(Dispatchers.Unconfined) {
    println("Thread: ${Thread.currentThread().name}") // Thread gọi
    delay(100)
    println("Thread: ${Thread.currentThread().name}") // Có thể khác thread!
}
```

### 4.5. Tạo Custom Dispatcher

```kotlin
// Tạo dispatcher với 4 threads
val customDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

// Sử dụng
suspend fun doWork() = withContext(customDispatcher) {
    // Chạy trên custom thread pool
    heavyWork()
}

// Tạo single-thread dispatcher (dùng cho thread-safe operations)
val singleThreadDispatcher = newSingleThreadContext("MySingleThread")

// QUAN TRỌNG: Phải close khi không dùng nữa
// customDispatcher.close()
```

### 4.6. Bảng so sánh Dispatchers

| Dispatcher | Thread Pool | Use Case | Số Thread |
|------------|------------|----------|-----------|
| `Main` | Main/UI thread | Cập nhật UI | 1 |
| `Main.immediate` | Main/UI thread | UI update ngay | 1 |
| `IO` | Shared pool | Network, File, DB | Lên đến 64 |
| `Default` | Shared pool | CPU computation | = Số CPU cores |
| `Unconfined` | Không cố định | Testing, đặc biệt | N/A |

```
Dispatchers.Main        → [Main Thread]
                            │
Dispatchers.IO          → [Thread Pool: IO-1, IO-2, ..., IO-64]
                            │ (shared backing threads)
Dispatchers.Default     → [Thread Pool: Default-1, ..., Default-N]
                            │ (N = CPU cores)
Dispatchers.Unconfined  → [Bất kỳ thread nào available]
```

> **Lưu ý:** `Dispatchers.IO` và `Dispatchers.Default` chia sẻ threads. Khi một coroutine trên `Default` gọi `withContext(IO)`, nó **có thể** tiếp tục trên cùng thread mà không cần chuyển.

---

## 5. Flow, SharedFlow, StateFlow

### 5.1. Flow - Cold Stream

**Flow** là một cold asynchronous stream phát ra nhiều giá trị theo thứ tự. Flow chỉ bắt đầu phát giá trị khi có collector (cold = lazy).

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

**Đặc điểm của Flow:**
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
        _counter.value++                    // Cách 1: Gán trực tiếp
        _counter.update { it + 1 }         // Cách 2: Thread-safe update
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
| Hot stream | Luôn active, phát giá trị ngay cả khi không có collector |
| Có initial value | Bắt buộc khởi tạo với giá trị ban đầu |
| Conflation | Chỉ giữ giá trị mới nhất, bỏ qua intermediate values |
| distinctUntilChanged | Chỉ phát khi giá trị thay đổi (so sánh bằng `equals`) |
| Replay = 1 | Collector mới nhận ngay giá trị hiện tại |
| Thread-safe | An toàn khi cập nhật từ nhiều threads |

### 5.3. SharedFlow - Event broadcasting

**SharedFlow** là một hot flow cho phép phát sự kiện (events) đến nhiều collectors. Khác StateFlow, nó **không có giá trị khởi tạo** và có thể cấu hình replay cache.

```kotlin
class EventViewModel : ViewModel() {

    // SharedFlow cho one-time events (snackbar, navigation, ...)
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

| Đặc điểm | Flow | SharedFlow | StateFlow |
|-----------|------|------------|-----------|
| Loại | Cold | Hot | Hot |
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
// Eagerly: Bắt đầu ngay khi tạo, không bao giờ dừng
SharingStarted.Eagerly

// Lazily: Bắt đầu khi có collector đầu tiên, không bao giờ dừng
SharingStarted.Lazily

// WhileSubscribed: Bắt đầu khi có collector, dừng khi không còn collector
// stopTimeoutMillis = 5000: Chờ 5 giây trước khi dừng (tránh restart khi config change)
// replayExpirationMillis: Thời gian giữ replay cache sau khi dừng
SharingStarted.WhileSubscribed(
    stopTimeoutMillis = 5000,
    replayExpirationMillis = Long.MAX_VALUE
)
```

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
    // ⚠️ KHÔNG thể cancel request khi coroutine bị cancel!
}

// Sử dụng
viewModelScope.launch {
    try {
        val user = getUserFromCallback() // Giờ có thể dùng như suspend function
        _userState.value = user
    } catch (e: Exception) {
        _errorState.value = e.message
    }
}
```

### 6.2. suspendCancellableCoroutine (✅ Khuyến khích)

Tương tự `suspendCoroutine` nhưng **hỗ trợ cancellation**. Luôn dùng thay cho `suspendCoroutine` khi có thể.

```kotlin
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun getUserCancellable(): User = suspendCancellableCoroutine { continuation ->
    val call = userApi.fetchUser(object : Callback<User> {
        override fun onSuccess(user: User) {
            // Kiểm tra coroutine còn active không trước khi resume
            if (continuation.isActive) {
                continuation.resume(user)
            }
        }

        override fun onError(error: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
    })

    // ✅ Xử lý khi coroutine bị cancel
    continuation.invokeOnCancellation {
        call.cancel() // Hủy network request
    }
}
```

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
    } catch (e: Exception) {
        _errorState.value = "Không thể lấy vị trí: ${e.message}"
    }
}
```

**Ví dụ - Wrap AlertDialog:**

```kotlin
suspend fun showConfirmDialog(context: Context, message: String): Boolean =
    suspendCancellableCoroutine { continuation ->
        val dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                continuation.resume(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                continuation.resume(false)
            }
            .setOnCancelListener {
                continuation.resume(false)
            }
            .create()

        dialog.show()

        continuation.invokeOnCancellation {
            dialog.dismiss()
        }
    }

// Sử dụng
viewModelScope.launch {
    val confirmed = showConfirmDialog(context, "Bạn có chắc muốn xóa?")
    if (confirmed) {
        deleteItem()
    }
}
```

### 6.3. So sánh suspendCoroutine vs suspendCancellableCoroutine

| Đặc điểm | `suspendCoroutine` | `suspendCancellableCoroutine` |
|-----------|-------------------|-------------------------------|
| Cancellation support | ❌ Không | ✅ Có |
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

    // ⚠️ BẮT BUỘC phải có awaitClose
    // Được gọi khi flow bị cancel (collector dừng collect)
    awaitClose {
        // Cleanup: Hủy đăng ký listener
        ref.removeEventListener(listener)
    }
}

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
// Ví dụ 2: Wrap Sensor listener
fun getSensorData(sensorManager: SensorManager, sensorType: Int): Flow<SensorEvent> =
    callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event) // Emit sensor data
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val sensor = sensorManager.getDefaultSensor(sensorType)
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
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

    awaitClose {
        removeTextChangedListener(watcher)
    }
}

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
// Ví dụ 4: Wrap BroadcastReceiver
fun Context.networkStatusFlow(): Flow<Boolean> = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(request, callback)

    // Emit trạng thái hiện tại
    val currentState = connectivityManager.activeNetwork != null
    trySend(currentState)

    awaitClose {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}
```

**Các method quan trọng trong callbackFlow:**

| Method | Mô tả |
|--------|--------|
| `trySend(value)` | Gửi giá trị vào flow (non-blocking, non-suspending) |
| `send(value)` | Gửi giá trị (suspending - sẽ đợi nếu buffer đầy) |
| `close()` | Đóng flow bình thường |
| `close(cause)` | Đóng flow với exception |
| `awaitClose { }` | **Bắt buộc** - Block cho đến khi flow bị cancel, dùng để cleanup |

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

### 7.4. Repository Layer

```kotlin
class UserRepository(
    private val apiService: UserApiService,
    private val userDao: UserDao
) {

    // Pattern: Network-first với local cache fallback
    suspend fun getUser(userId: String): Result<User> {
        return try {
            // Gọi API trên IO dispatcher
            val response = withContext(Dispatchers.IO) {
                apiService.getUser(userId)
            }
            val user = response.toUser()

            // Cache vào database
            withContext(Dispatchers.IO) {
                userDao.insertUser(user.toEntity())
            }

            Result.success(user)
        } catch (e: HttpException) {
            // Lỗi HTTP (4xx, 5xx)
            Result.failure(ApiException(e.code(), e.message()))
        } catch (e: IOException) {
            // Lỗi network - fallback to cache
            val cachedUser = withContext(Dispatchers.IO) {
                userDao.getUserById(userId)?.toUser()
            }
            if (cachedUser != null) {
                Result.success(cachedUser)
            } else {
                Result.failure(NetworkException("No internet connection"))
            }
        }
    }

    // Pattern: Trả về Flow cho realtime updates
    fun getUserStream(userId: String): Flow<User> {
        return userDao.getUserFlowById(userId) // Room trả về Flow
            .filterNotNull()
            .map { it.toUser() }
            .onStart {
                // Refresh từ network khi bắt đầu collect
                try {
                    val networkUser = apiService.getUser(userId)
                    userDao.insertUser(networkUser.toEntity())
                } catch (e: Exception) {
                    // Bỏ qua lỗi network, dùng cache
                }
            }
            .flowOn(Dispatchers.IO)
    }

    // Pattern: Xử lý pagination
    fun getUsersPaginated(): Flow<PagingData<User>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { UserPagingSource(apiService) }
        ).flow
    }
}
```

### 7.5. ViewModel

```kotlin
class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading

            userRepository.getUser(userId)
                .onSuccess { user ->
                    _uiState.value = UserUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = UserUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    // Gọi nhiều API song song
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading

            try {
                // Chạy song song 3 request
                val userDeferred = async { userRepository.getUser("me") }
                val ordersDeferred = async { orderRepository.getOrders() }
                val notifDeferred = async { notificationRepository.getUnread() }

                val user = userDeferred.await().getOrThrow()
                val orders = ordersDeferred.await().getOrThrow()
                val notifications = notifDeferred.await().getOrThrow()

                _uiState.value = UserUiState.Dashboard(user, orders, notifications)
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Retry logic
    fun loadWithRetry(userId: String, maxRetries: Int = 3) {
        viewModelScope.launch {
            var lastException: Exception? = null

            repeat(maxRetries) { attempt ->
                try {
                    val user = userRepository.getUser(userId).getOrThrow()
                    _uiState.value = UserUiState.Success(user)
                    return@launch // Thành công, thoát
                } catch (e: Exception) {
                    lastException = e
                    delay((attempt + 1) * 1000L) // Exponential backoff
                }
            }

            _uiState.value = UserUiState.Error(
                "Failed after $maxRetries retries: ${lastException?.message}"
            )
        }
    }
}

// UI State sealed class
sealed class UserUiState {
    data object Loading : UserUiState()
    data class Success(val user: User) : UserUiState()
    data class Dashboard(
        val user: User,
        val orders: List<Order>,
        val notifications: List<Notification>
    ) : UserUiState()
    data class Error(val message: String) : UserUiState()
}
```

### 7.6. Generic API Caller (Utility)

```kotlin
// Utility function để handle API calls
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    apiCall: suspend () -> T
): Result<T> {
    return withContext(dispatcher) {
        try {
            Result.success(apiCall())
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Result.failure(ApiException(e.code(), errorBody ?: e.message()))
        } catch (e: IOException) {
            Result.failure(NetworkException("No internet connection"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Sử dụng
class UserRepository(private val api: UserApiService) {

    suspend fun getUser(userId: String): Result<User> = safeApiCall {
        api.getUser(userId).toUser()
    }

    suspend fun getUsers(page: Int): Result<List<User>> = safeApiCall {
        val response = api.getUsers(page)
        if (response.isSuccessful) {
            response.body()?.map { it.toUser() } ?: emptyList()
        } else {
            throw HttpException(response)
        }
    }
}
```

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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 8.5. Repository với Room + Retrofit

```kotlin
class UserRepository(
    private val api: UserApiService,
    private val userDao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // ===== Pattern: Single Source of Truth =====
    // Database là nguồn chính, network chỉ để refresh
    fun getUsers(): Flow<Resource<List<User>>> = flow {
        // 1. Emit Loading
        emit(Resource.Loading())

        // 2. Emit cached data trước
        val cachedUsers = userDao.getAllUsers().map { it.toUser() }
        if (cachedUsers.isNotEmpty()) {
            emit(Resource.Success(cachedUsers))
        }

        // 3. Fetch từ network
        try {
            val networkUsers = api.getUsers(page = 1)
            if (networkUsers.isSuccessful) {
                val users = networkUsers.body() ?: emptyList()
                // 4. Lưu vào database
                userDao.replaceAllUsers(users.map { it.toEntity() })
            }
        } catch (e: Exception) {
            if (cachedUsers.isEmpty()) {
                emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }

        // 5. Observe database cho realtime updates
        emitAll(
            userDao.getAllUsersFlow()
                .map { entities ->
                    Resource.Success(entities.map { it.toUser() })
                }
        )
    }.flowOn(ioDispatcher)

    // ===== Pattern: Offline-first =====
    fun getUserById(userId: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())

        // Observe từ database
        emitAll(
            userDao.getUserFlowById(userId)
                .map { entity ->
                    if (entity != null) {
                        Resource.Success(entity.toUser())
                    } else {
                        Resource.Error("User not found")
                    }
                }
        )
    }.onStart {
        // Refresh từ network khi bắt đầu
        try {
            val networkUser = api.getUser(userId)
            userDao.insertUser(networkUser.toEntity())
        } catch (_: Exception) {
            // Ignore network error, use cache
        }
    }.flowOn(ioDispatcher)

    // ===== One-shot operations =====
    suspend fun createUser(request: CreateUserRequest): Result<User> =
        withContext(ioDispatcher) {
            try {
                val response = api.createUser(request)
                val user = response.toUser()
                userDao.insertUser(user.toEntity())
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteUser(userId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                api.deleteUser(userId)
                userDao.deleteUserById(userId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

// Resource wrapper
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Loading<T> : Resource<T>()
    class Success<T>(data: T) : Resource<T>(data = data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data = data, message = message)
}
```

### 8.6. ViewModel sử dụng Room + Retrofit

```kotlin
class UserListViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    // Observe users từ repository (Room Flow → StateFlow)
    val usersState: StateFlow<Resource<List<User>>> = userRepository
        .getUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    // Search với debounce
    private val _searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<User>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                userRepository.getAllUsersFlow()
            } else {
                userRepository.searchUsers(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // Delete user
    private val _deleteEvent = MutableSharedFlow<String>()
    val deleteEvent: SharedFlow<String> = _deleteEvent.asSharedFlow()

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            userRepository.deleteUser(userId)
                .onSuccess {
                    _deleteEvent.emit("User deleted successfully")
                }
                .onFailure { error ->
                    _deleteEvent.emit("Failed to delete: ${error.message}")
                }
        }
    }
}
```

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
class Repository(private val api: ApiService) {
    // Repository đảm bảo main-safety bằng withContext
    suspend fun getData(): Data = withContext(Dispatchers.IO) {
        api.fetchData()
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
        throw e // ⚠️ KHÔNG bao giờ catch CancellationException
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
GlobalScope.launch {  // ❌ Memory leak, không cancel được
    updateUI()
}

// 2. ❌ KHÔNG block Main thread
viewModelScope.launch {
    val data = heavyComputation()  // ❌ Chạy trên Main thread!
    // Phải dùng withContext(Dispatchers.Default)
}

// 3. ❌ KHÔNG catch CancellationException
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

// Exception trong async - phải catch tại await()
viewModelScope.launch {
    val deferred = async {
        throw RuntimeException("Oops!")
    }

    try {
        deferred.await() // Exception thrown ở đây
    } catch (e: RuntimeException) {
        // Handle exception
    }
}
```

---

## 10. Bài tập thực hành

### Bài 1: Coroutine Basics
Tạo một ứng dụng đếm ngược (countdown timer) sử dụng `StateFlow` và `viewModelScope.launch`:
- Hiển thị đếm ngược từ 10 đến 0
- Có nút Start/Pause/Reset
- Sử dụng `delay()` cho mỗi giây

### Bài 2: Parallel Loading
Tạo màn hình Dashboard load 3 loại data song song:
- User Profile (từ API)
- Recent Orders (từ API)
- Notifications (từ API)
- Sử dụng `async` để load song song
- Hiển thị loading state và error handling

### Bài 3: Search với Flow
Implement chức năng search sử dụng:
- `callbackFlow` để wrap `EditText.TextWatcher`
- `debounce(300)` để tránh gọi API quá nhiều
- `distinctUntilChanged` để tránh trùng query
- `flatMapLatest` để cancel search cũ khi có query mới

### Bài 4: Offline-first Architecture
Tạo ứng dụng hiển thị danh sách Users:
- Sử dụng Room DAO với `Flow` để observe data
- Fetch từ Retrofit khi có network
- Hiển thị cached data khi offline
- Sử dụng `Resource<T>` wrapper cho Loading/Success/Error states

### Bài 5: callbackFlow
Tạo Flow wrapper cho:
- Firebase Realtime Database listener
- Location updates (FusedLocationProviderClient)
- Network connectivity status (ConnectivityManager)

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

---

> **Ghi chú:** Bài giảng này sử dụng Kotlin Coroutines phiên bản `1.11.0` và Android Architecture Components phiên bản mới nhất (2024-2025). Các API và best practices có thể thay đổi trong các phiên bản tương lai.

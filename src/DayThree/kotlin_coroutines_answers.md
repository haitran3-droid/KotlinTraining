# Đáp án bài tập thực hành Kotlin Coroutines

Tài liệu tham chiếu cho 7 bài ở [bài giảng Kotlin Coroutines](kotlin_coroutines.md#10-bài-tập-thực-hành). Mỗi bài có hướng giải, code tham khảo, giải thích và tình huống tự kiểm tra. Bạn có thể tự làm trước rồi đối chiếu; không cần viết giống hệt đáp án nếu hành vi đúng.

## Cách sử dụng

- Bài 1, 3, 4, 5 cần project Android. Bài 2, 6, 7 có phần demo console để quan sát hành vi.
- Mỗi bài là một ví dụ độc lập. Không ghép tất cả các hàm `main()` hay các model của các bài vào một file Kotlin.
- Dependency và cấu hình Android/Kotlin/Retrofit/Room dùng theo bài giảng và version catalog của project. Bài 5 cần thêm Firebase Realtime Database/Google Play services Location tương ứng.
- Imports Android và SDK của từng bài do IDE hỗ trợ thêm. Với coroutine thuần Kotlin, các imports thường dùng:

~~~kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
~~~

Các đoạn ghép UI chỉ minh họa chỗ nối logic với màn hình; bạn tự tạo layout, binding, ViewModel factory và giao diện hiển thị. Đáp án không bao gồm cấu hình server, Firebase project hay migration database của một ứng dụng cụ thể.

## Mục lục

1. [Countdown timer](#1-countdown-timer)
2. [Dashboard tải đồng thời](#2-dashboard-tải-đồng-thời)
3. [Search với TextWatcher và Flow](#3-search-với-textwatcher-và-flow)
4. [Danh sách user offline-first](#4-danh-sách-user-offline-first)
5. [Các adapter callbackFlow](#5-các-adapter-callbackflow)
6. [Callback một kết quả và cancellation](#6-callback-một-kết-quả-và-cancellation)
7. [StateFlow và SharedFlow khi dừng collector](#7-stateflow-và-sharedflow-khi-dừng-collector)
8. [Đáp án câu hỏi cuối buổi](#8-đáp-án-câu-hỏi-cuối-buổi)

---

## 1. Countdown timer

### Hướng giải

ViewModel giữ state và Job của timer. Start bỏ qua nếu timer đang hoạt động hoặc đã về 0. Pause hủy Job; Reset vừa hủy vừa đưa số giây về 10. Timer giảm mỗi lần `delay(1_000)` hoàn tất. Các thao tác UI gọi trên Main, không gọi đồng thời từ nhiều thread.

### Code tham khảo

~~~kotlin
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

data class CountdownState(
    val seconds: Int = 10,
    val isRunning: Boolean = false
)

class CountdownViewModel : ViewModel() {
    private val _state = MutableStateFlow(CountdownState())
    val state: StateFlow<CountdownState> = _state.asStateFlow()
    private var timerJob: Job? = null

    @MainThread
    fun start() {
        val previousJob = timerJob
        if (previousJob?.isActive == true || _state.value.seconds <= 0) return

        timerJob = viewModelScope.launch {
            // Nếu Job trước vừa bị cancel, chờ finally của nó trước khi bắt đầu.
            previousJob?.join()
            _state.update { it.copy(isRunning = true) }
            try {
                while (_state.value.seconds > 0) {
                    delay(1_000)
                    _state.update {
                        it.copy(seconds = (it.seconds - 1).coerceAtLeast(0))
                    }
                }
            } finally {
                _state.update { it.copy(isRunning = false) }
            }
        }
    }

    @MainThread
    fun pause() {
        timerJob?.cancel()
        // Giữ reference để Start tiếp theo có thể join Job cũ.
        _state.update { it.copy(isRunning = false) }
    }

    @MainThread
    fun reset() {
        pause()
        _state.value = CountdownState()
    }
}
~~~

Trong `Fragment.onViewCreated`, nối các nút với `start()`/`pause()`/`reset()` và collect state:

~~~kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { state ->
            binding.timerText.text = state.seconds.toString()
            binding.startButton.isEnabled = !state.isRunning && state.seconds > 0
        }
    }
}

binding.startButton.setOnClickListener { viewModel.start() }
binding.pauseButton.setOnClickListener { viewModel.pause() }
binding.resetButton.setOnClickListener { viewModel.reset() }
~~~

### Vì sao đáp án đạt yêu cầu?

- Chỉ có một Job đang chạy; Start lặp không tạo timer mới.
- `delay` hỗ trợ cancellation nên Pause ngăn lần giảm tiếp theo.
- Start chờ Job cũ hoàn tất cleanup, tránh `finally` cũ ghi đè trạng thái timer mới.
- StateFlow giữ giá trị hiện tại; UI quay lại nhận số giây mới nhất.
- ViewModel bị clear thì `viewModelScope` hủy timer.

**Chính sách lifecycle:** timer tiếp tục khi UI STOPPED nhưng ViewModel còn sống. UI ngừng collect không đồng nghĩa timer bị hủy. Muốn tự Pause khi rời màn hình, gọi Pause theo lifecycle hoặc đổi owner theo yêu cầu bài toán.

Đây là timer phục vụ bài học: `delay(1_000)` không bảo đảm độ chính xác của đồng hồ thực. Với deadline thực, tính số giây còn lại từ một mốc thời gian monotonic thay vì chỉ trừ 1 sau mỗi delay.

### Tự kiểm tra

| Thao tác | Kết quả cần có |
|----------|----------------|
| Start 5 lần liên tiếp | Chỉ giảm 1 mỗi nhịp, không giảm 5 |
| Pause ở 7 | Giữ 7 sau khi chờ thêm vài giây |
| Start sau Pause | Tiếp tục từ 7 |
| Reset lúc chạy | Trở lại 10 và dừng |
| Start ở 0 | Không giảm xuống số âm |
| ViewModel bị clear | Job dừng |
| Xoay màn hình với cùng ViewModel | Nhận state hiện tại, không tự tạo timer mới |

---

## 2. Dashboard tải đồng thời

### Hướng giải

Repository dùng `coroutineScope`, tạo đủ 3 `async` trước khi await. ViewModel xử lý Loading/Success/Error. Cancellation được ném lại. Dashboard mặc định cần tất cả request thành công; bản mở rộng dùng `supervisorScope` để giữ kết quả từng phần.

### Model, API giả và Repository

~~~kotlin
data class Profile(val name: String)
data class Order(val id: String)
data class NotificationItem(val message: String)

data class Dashboard(
    val profile: Profile,
    val orders: List<Order>,
    val notifications: List<NotificationItem>
)

interface DashboardApi {
    suspend fun getProfile(): Profile
    suspend fun getOrders(): List<Order>
    suspend fun getNotifications(): List<NotificationItem>
}

class FakeDashboardApi(
    private val failOrders: Boolean = false
) : DashboardApi {
    private suspend fun <T> request(
        name: String,
        duration: Long,
        result: () -> T
    ): T {
        try {
            delay(duration)
            return result()
        } catch (e: CancellationException) {
            println("$name bị cancel")
            throw e
        } finally {
            println("$name cleanup")
        }
    }

    override suspend fun getProfile(): Profile =
        request("profile", 2_000) { Profile("An") }

    override suspend fun getOrders(): List<Order> =
        request("orders", if (failOrders) 300 else 3_000) {
            if (failOrders) error("Không tải được orders")
            listOf(Order("O-01"))
        }

    override suspend fun getNotifications(): List<NotificationItem> =
        request("notifications", 1_000) {
            listOf(NotificationItem("Có đơn hàng mới"))
        }
}

class DashboardRepository(private val api: DashboardApi) {
    suspend fun load(): Dashboard = coroutineScope {
        val profile = async { api.getProfile() }
        val orders = async { api.getOrders() }
        val notifications = async { api.getNotifications() }

        Dashboard(profile.await(), orders.await(), notifications.await())
    }
}
~~~

### ViewModel

~~~kotlin
sealed interface DashboardState {
    data object Loading : DashboardState
    data class Success(val data: Dashboard) : DashboardState
    data class Error(val message: String) : DashboardState
}

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.value = DashboardState.Loading
            try {
                _state.value = DashboardState.Success(repository.load())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: "Tải dashboard thất bại")
            }
        }
    }
}
~~~

### Demo console và kết quả

~~~kotlin
fun main() = runBlocking {
    val repository = DashboardRepository(FakeDashboardApi())
    val start = System.nanoTime()
    println(repository.load())
    println("Thời gian: " + (System.nanoTime() - start) / 1_000_000 + " ms")
}
~~~

Thành công mất khoảng 3 giây với fake API trên. Nếu thay thành `FakeDashboardApi(failOrders = true)`, orders lỗi sau khoảng 300 ms; profile và notifications đang chờ sẽ bị cancel, chạy cleanup; `load()` ném lỗi về caller. Muốn quan sát lỗi trong console thì bọc lời gọi `load()` bằng try/catch tương tự ViewModel.

### Mở rộng: kết quả từng phần

~~~kotlin
data class DashboardParts(
    val profile: Result<Profile>,
    val orders: Result<List<Order>>,
    val notifications: Result<List<NotificationItem>>
)

suspend fun <T> awaitResult(deferred: Deferred<T>): Result<T> = try {
    Result.success(deferred.await())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

suspend fun loadParts(api: DashboardApi): DashboardParts = supervisorScope {
    val profile = async { api.getProfile() }
    val orders = async { api.getOrders() }
    val notifications = async { api.getNotifications() }
    DashboardParts(awaitResult(profile), awaitResult(orders), awaitResult(notifications))
}
~~~

Orders lỗi không hủy hai request còn lại; UI nhận Result riêng để hiển thị từng phần. Cancellation của caller vẫn hủy cả nhóm. Tạo đủ Deferred trước khi await là điều kiện để chúng chạy đồng thời.

### Tự kiểm tra

- Ba request thành công: tổng thời gian khoảng max(2s, 3s, 1s), không phải tổng 6s.
- Orders lỗi sớm: bản `load()` hủy siblings; bản `loadParts()` vẫn có profile/notifications.
- Hủy ViewModel: không chuyển cancellation thành DashboardState.Error.
- Không lấy `SupervisorJob` ở viewModelScope để suy ra các `async` bên trong một `launch` tự độc lập; Job cha trực tiếp của chúng là coroutine `launch` đó.

---

## 3. Search với TextWatcher và Flow

### Hướng giải

TextWatcher phát **query hiện tại** bằng callbackFlow. Dùng conflation vì chỉ cần query mới nhất, rồi normalize → debounce → distinctUntilChanged → flatMapLatest. Mỗi request xử lý lỗi trong inner Flow để request lỗi không làm pipeline ngừng nhận query mới.

### Adapter TextWatcher

~~~kotlin
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

fun EditText.textChanges(): Flow<String> = callbackFlow {
    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            trySend(s?.toString().orEmpty())
        }
    }
    addTextChangedListener(watcher)
    trySend(text.toString())
    awaitClose { removeTextChangedListener(watcher) }
}.buffer(Channel.CONFLATED)
~~~

### API giả và pipeline tìm kiếm

~~~kotlin
data class SearchItem(val id: String, val title: String)

interface SearchApi {
    suspend fun search(query: String): List<SearchItem>
}

class FakeSearchApi : SearchApi {
    override suspend fun search(query: String): List<SearchItem> {
        delay(800)
        if (query == "error") error("Request search thất bại")
        return listOf(SearchItem(query, "Kết quả cho $query"))
    }
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Success(val items: List<SearchItem>) : SearchState
    data class Error(val message: String) : SearchState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun searchStates(
    queries: Flow<String>,
    api: SearchApi
): Flow<SearchState> = queries
    .map { it.trim() }
    .debounce(300)
    .distinctUntilChanged()
    .flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf<SearchState>(SearchState.Idle)
        } else {
            flow<SearchState> {
                emit(SearchState.Loading)
                emit(SearchState.Success(api.search(query)))
            }.catch { e ->
                // Chỉ lỗi của request này; cancellation của Flow không bị nuốt.
                emit(SearchState.Error(e.message ?: "Search thất bại"))
            }
        }
    }
~~~

### Collect trong Fragment

Đặt đoạn sau trong `onViewCreated`. `renderSearch` là hàm UI bạn viết để hiển thị progress, danh sách hoặc lỗi.

~~~kotlin
val searchApi: SearchApi = FakeSearchApi()

viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        searchStates(binding.searchEditText.textChanges(), searchApi)
            .collect { state -> renderSearch(state) }
    }
}
~~~

### Điểm cần hiểu

- `callbackFlow` đăng ký listener khi collect; cancellation vào background sẽ chạy awaitClose và gỡ listener.
- Collect lại phát nội dung EditText hiện tại, dù người dùng chưa gõ ký tự mới.
- Đăng ký/gỡ TextWatcher chạy trên Main trong ví dụ; không bọc adapter UI bằng `flowOn(IO)`.
- `flatMapLatest` hủy request cũ khi query mới **đã đi qua debounce**. Trong 300 ms đợi debounce, request cũ có thể hoàn tất và phát kết quả.
- Nếu phải loại bỏ kết quả cũ ngay từ lần gõ đầu tiên, cần thiết kế pipeline khác hoặc kiểm tra query/version trước khi render.
- Đặt `catch` bên ngoài toàn pipeline sẽ phát Error rồi kết thúc pipeline sau một lỗi; đặt trong inner Flow giúp query tiếp theo vẫn hoạt động.

### Tự kiểm tra

| Tình huống | Kết quả cần có |
|------------|----------------|
| Gõ k → ko → kot trong dưới 300 ms | Chỉ request query cuối sau khoảng im lặng |
| Phát lại cùng query đã normalize | Không gọi request lặp |
| Query mới đi qua debounce khi request cũ chưa xong | Request cũ bị cancel |
| Gõ error rồi kotlin | Có Error rồi vẫn tìm được kotlin |
| UI STOPPED | Listener được gỡ, request đang collect bị hủy |
| UI STARTED lại | Listener mới được đăng ký, query hiện tại được search |
| Xóa hết query | Idle sau debounce, không gọi search rỗng |

---

## 4. Danh sách user offline-first

### Hướng giải

UI luôn đọc từ Room. Refresh Retrofit là một tác vụ riêng: thành công thì ghi transaction vào Room; lỗi thì giữ cache và cập nhật error. Không đặt request network trong `onStart` của Flow observe cache.

Bài này định nghĩa model riêng, không dùng chung các model của bài 2–3.

### Model và DAO

~~~kotlin
import androidx.room.*

data class UserDto(val id: String, val fullName: String, val email: String)
data class User(val id: String, val fullName: String, val email: String)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    val email: String
)

fun UserDto.toEntity() = UserEntity(id, fullName, email)
fun UserEntity.toUser() = User(id, fullName, email)

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY full_name ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: String)

    @Transaction
    suspend fun replaceAll(users: List<UserEntity>) {
        deleteAll()
        insertUsers(users)
    }
}

@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
~~~

`exportSchema = false` giúp tập trung vào demo; ứng dụng thật nên quản lý schema/migration. Tạo database một lần bằng `Room.databaseBuilder(applicationContext, UserDatabase::class.java, "users.db").build()` và truyền DAO vào Repository. Không bật `allowMainThreadQueries()`.

### API và Repository

~~~kotlin
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface UserApi {
    @GET("users")
    suspend fun getUsers(): Response<List<UserDto>>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): Response<Unit>
}

suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

class UserRepository(
    private val api: UserApi,
    private val dao: UserDao,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    fun observeUsers(): Flow<List<User>> = dao.observeUsers()
        .map { entities -> entities.map { it.toUser() } }
        .distinctUntilChanged()
        .flowOn(computeDispatcher)

    suspend fun refresh(): Result<Unit> = safeApiCall {
        val response = api.getUsers()
        if (!response.isSuccessful) throw HttpException(response)

        val users = response.body()
            ?: throw IllegalStateException("HTTP thành công nhưng body null")
        val entities = withContext(computeDispatcher) {
            users.map { it.toEntity() }
        }
        dao.replaceAll(entities)
    }

    suspend fun deleteUser(userId: String): Result<Unit> = safeApiCall {
        val response = api.deleteUser(userId)
        if (!response.isSuccessful) throw HttpException(response)
        dao.deleteById(userId)
    }
}
~~~

Retrofit suspend và Room suspend/Flow tự quản lý I/O. Dispatcher Default ở đây dành cho mapping lớn, không phải để thay Room query executor.

### ViewModel

~~~kotlin
data class UserListUiState(
    val users: List<User> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class UserListViewModel(
    private val repository: UserRepository
) : ViewModel() {
    private val operationState = MutableStateFlow(UserListUiState())
    private var refreshJob: Job? = null

    val uiState: StateFlow<UserListUiState> = combine(
        repository.observeUsers(),
        operationState
    ) { users, operation ->
        operation.copy(users = users)
    }
        .catch { error ->
            emit(UserListUiState(error = error.message ?: "Không đọc được database"))
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
            operationState.update { it.copy(isRefreshing = true, error = null) }
            try {
                repository.refresh().onFailure { error ->
                    operationState.update {
                        it.copy(error = error.message ?: "Refresh thất bại")
                    }
                }
            } finally {
                operationState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            operationState.update { it.copy(error = null) }
            repository.deleteUser(userId).onFailure { error ->
                operationState.update {
                    it.copy(error = error.message ?: "Xóa thất bại")
                }
            }
        }
    }

    fun dismissError() {
        operationState.update { it.copy(error = null) }
    }
}
~~~

### Ghép UI và chính sách dữ liệu

Trong `onViewCreated`, collect `uiState` bằng `viewLifecycleOwner.repeatOnLifecycle(STARTED)`. Hàm render phải hiển thị độc lập ba thành phần:

~~~kotlin
fun renderUsers(state: UserListUiState) {
    adapter.submitList(state.users)
    binding.refreshProgress.isVisible = state.isRefreshing
    binding.errorText.isVisible = state.error != null
    binding.errorText.text = state.error.orEmpty()
}
~~~

Adapter/binding là UI do bạn triển khai; `isVisible` dùng extension của AndroidX Core.

**Xoay màn hình:** lấy cùng ViewModel bằng cơ chế Android ViewModel/factory, không tự tạo ViewModel mới mỗi lần tạo View. Không gọi `refresh()` trong block collect/repeatOnLifecycle; `init` chỉ chạy khi ViewModel được tạo. UI quay lại collect không tự gọi thêm API.

**STOPPED:** UI ngừng collect ngay. `WhileSubscribed(5_000)` giữ upstream observe tối đa thêm 5 giây sau subscriber cuối; sau đó dừng. Refresh ở viewModelScope vẫn có thể chạy khi UI STOPPED.

**Room query lỗi:** `catch` trong ví dụ phát state lỗi rồi upstream kết thúc; nếu cần phục hồi thì thiết kế retry/restart. Đây là lỗi khác với refresh network: lỗi network không kết thúc observe Room.

**Phạm vi đáp án:** API GET trả toàn bộ danh sách, không phải một trang. Nếu server phân trang, không dùng mỗi trang để thay toàn bộ bảng. Server và Room không có transaction chung; trường hợp server xóa thành công nhưng local delete lỗi cần reconcile/refresh theo yêu cầu app.

### Tự kiểm tra

| Tình huống | Kết quả cần có |
|------------|----------------|
| Room có cache, request chậm 5s | Cache hiện trước khi request hoàn tất |
| GET HTTP 500 | Cache giữ nguyên, progress kết thúc, có error |
| GET body null dù status thành công | Báo lỗi, không biến thành empty list để xóa cache |
| GET thành công với danh sách rỗng hợp lệ | Room được thay bằng danh sách rỗng |
| DELETE HTTP 403 | Không gọi dao.deleteById, không báo thành công |
| GET thành công | Room cập nhật transaction, UI nhận list mới |
| Insert lỗi giữa transaction replaceAll | Transaction rollback, không giữ trạng thái xóa dở |
| Xoay màn hình, ViewModel còn sống | Không refresh thêm chỉ vì có collector mới |

---

## 5. Các adapter callbackFlow

### Hướng giải chung

Mỗi collection đăng ký một callback/listener; `awaitClose` gỡ đúng registration khi channel đóng hoặc collection bị hủy. Các ví dụ dưới phát **snapshot/state mới nhất**, nên dùng conflation. Nếu cần giữ mọi sự kiện, phải thiết kế capacity/backpressure và xử lý kết quả `trySend` theo yêu cầu đó.

`callbackFlow` là cold; hai collector riêng sẽ đăng ký hai listener. Dùng `shareIn`/`stateIn` với scope phù hợp nếu muốn chia sẻ một registration.

### 5.1. Firebase Realtime Database

Firebase project, DatabaseReference và quyền đọc phải được cấu hình trong ứng dụng. Mỗi callback dưới trả snapshot toàn bộ danh sách của ref.

~~~kotlin
import com.google.firebase.database.*

data class ChatMessage(
    val id: String = "",
    val text: String = ""
)

fun DatabaseReference.messagesFlow(): Flow<List<ChatMessage>> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                val messages = snapshot.children.mapNotNull { child ->
                    child.getValue(ChatMessage::class.java)
                        ?.copy(id = child.key.orEmpty())
                }
                trySend(messages)
            } catch (e: Exception) {
                // Lỗi deserialize trong callback không tự đi vào coroutine catch.
                close(e)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}.buffer(Channel.CONFLATED)
~~~

Ví dụ collect trong lifecycle của View:

~~~kotlin
val messagesRef = FirebaseDatabase.getInstance()
    .getReference("chats")
    .child(chatId)
    .child("messages")

viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        messagesRef.messagesFlow()
            .catch { error -> showMessageError(error) }
            .collect { messages -> messageAdapter.submitList(messages) }
    }
}
~~~

`chatId`, adapter và hàm hiển thị lỗi do UI cung cấp. Sau lỗi terminal, collection hiện tại kết thúc; muốn tự retry cần chính sách riêng. Conflation phù hợp snapshot toàn danh sách; không áp dụng máy móc cho event kiểu “thêm từng message” cần giữ đủ.

### 5.2. FusedLocationProviderClient

Caller phải có runtime permission vị trí phù hợp trước khi collect. `@SuppressLint` chỉ tắt cảnh báo lint, không cấp quyền. Adapter này thu vị trí khi UI đang collect, không thiết kế theo dõi nền liên tục.

~~~kotlin
import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.*
import java.util.concurrent.atomic.AtomicBoolean

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val elapsedRealtimeNanos: Long
)

@SuppressLint("MissingPermission")
fun FusedLocationProviderClient.locationUpdates(): Flow<LocationPoint> = callbackFlow {
    val client = this@locationUpdates
    val closed = AtomicBoolean(false)
    val request = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        5_000L
    ).build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (closed.get()) return
            val location = result.lastLocation ?: return
            trySend(
                LocationPoint(
                    location.latitude,
                    location.longitude,
                    location.elapsedRealtimeNanos
                )
            )
        }
    }

    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        .addOnSuccessListener {
            // Registration hoàn tất bất đồng bộ: collector có thể đã bị hủy.
            if (closed.get()) client.removeLocationUpdates(callback)
        }
        .addOnFailureListener { error -> close(error) }

    awaitClose {
        closed.set(true)
        client.removeLocationUpdates(callback)
    }
}.buffer(Channel.CONFLATED)
~~~

UI collect theo lifecycle, xử lý lỗi bằng `catch` trước collect. Mỗi lần UI STARTED lại tạo collection/registration mới. Success listener kiểm tra `closed` để gỡ lại nếu registration hoàn tất sau cleanup.

`removeLocationUpdates` trả Task: đoạn trên yêu cầu gỡ, không suspend để chờ xác nhận đã gỡ. Dữ liệu đến muộn không vào UI vì channel đã đóng. App cần bảo đảm xác nhận cleanup hoặc xử lý lỗi gỡ callback thì theo dõi Task đó riêng.

Snapshot dùng các giá trị nguyên thủy, không giữ một object Location có thể bị sửa về sau. Khoảng 5 giây là interval yêu cầu, không phải cam kết thiết bị luôn có một vị trí đúng mỗi 5 giây.

### 5.3. Network connectivity

Cần `android.permission.ACCESS_NETWORK_STATE` trong manifest. Ví dụ dùng default network callback, yêu cầu API 24+.

~~~kotlin
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicReference

@RequiresApi(Build.VERSION_CODES.N)
fun Context.networkStatusFlow(): Flow<Boolean> = callbackFlow {
    val manager = applicationContext.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager

    fun hasValidatedInternet(capabilities: NetworkCapabilities?): Boolean =
        capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    // Snapshot ban đầu: chưa có callback nào chạy.
    val initialNetwork = manager.activeNetwork
    trySend(hasValidatedInternet(initialNetwork?.let(manager::getNetworkCapabilities)))
    val trackedNetwork = AtomicReference<Network?>(null)

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trackedNetwork.set(network)
            // Đợi capabilities từ onCapabilitiesChanged.
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            if (trackedNetwork.get() == network) {
                trySend(hasValidatedInternet(capabilities))
            }
        }

        override fun onLost(network: Network) {
            // Không để onLost của network cũ ghi đè state của network mới.
            val current = trackedNetwork.get()
            if (current == network && trackedNetwork.compareAndSet(current, null)) trySend(false)
        }
    }

    manager.registerDefaultNetworkCallback(callback)
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.buffer(Channel.CONFLATED).distinctUntilChanged()
~~~

`NET_CAPABILITY_INTERNET` cho biết network có khả năng internet; `VALIDATED` cho biết hệ thống đã xác nhận kết nối. Giá trị true là tín hiệu UI, không bảo đảm request đến server của ứng dụng sẽ thành công; Retrofit vẫn phải xử lý exception/HTTP.

### Tự kiểm tra cả ba adapter

- Đếm số lần register/unregister khi STARTED → STOPPED → STARTED. Mỗi collection có một registration và cleanup tương ứng.
- Đóng Flow do callback lỗi: listener được gỡ.
- Cancel trước khi đăng ký Location hoàn tất: success callback sau đó vẫn yêu cầu remove.
- Không cập nhật binding sau `onDestroyView`.
- Không dùng `GlobalScope` để phát dữ liệu từ callback.
- State snapshot có thể bỏ giá trị trung gian; nếu đổi sang event cần giữ đủ thì đáp án buffer phải đổi.
- Nếu thêm sensor như phần mở rộng, copy `event.values` thành snapshot trước khi gửi; không phát trực tiếp SensorEvent có thể được Android tái sử dụng.

---

## 6. Callback một kết quả và cancellation

### Hướng giải

Fake API chạy trong scope riêng để mô phỏng SDK bên ngoài: cancel coroutine caller không tự cancel request của API. API trả handle `cancel()`. Viết hai adapter, bảo vệ callback kết thúc bằng AtomicBoolean và so sánh thời gian cancel caller.

### Fake API

~~~kotlin
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class CallbackUser(val id: String, val name: String)

interface UserCallback {
    fun onSuccess(user: CallbackUser)
    fun onError(error: Throwable)
}

interface CancelableCall {
    fun cancel()
}

class FakeUserApi(
    private val workerScope: CoroutineScope,
    private val deliverAfterCancel: Boolean = false,
    private val duplicateCompletion: Boolean = true
) {
    val cancelCalls = AtomicInteger(0)

    fun fetchUser(callback: UserCallback): CancelableCall {
        val job = workerScope.launch {
            delay(500)
            println("API phát callback")
            callback.onSuccess(CallbackUser("U-01", "An"))
            if (duplicateCompletion) {
                // Cố tình vi phạm hợp đồng để thử adapter.
                callback.onError(IllegalStateException("Callback kết thúc bị gọi lần hai"))
            }
        }
        return object : CancelableCall {
            override fun cancel() {
                cancelCalls.incrementAndGet()
                if (!deliverAfterCancel) job.cancel()
            }
        }
    }
}
~~~

### Adapter suspendCoroutine

~~~kotlin
suspend fun FakeUserApi.awaitUserWithSuspendCoroutine(): CallbackUser =
    suspendCoroutine { continuation ->
        val completed = AtomicBoolean(false)
        fetchUser(object : UserCallback {
            override fun onSuccess(user: CallbackUser) {
                if (completed.compareAndSet(false, true)) continuation.resume(user)
            }

            override fun onError(error: Throwable) {
                if (completed.compareAndSet(false, true)) {
                    continuation.resumeWithException(error)
                }
            }
        })
        // Handle trả về không được nối với Job cancellation.
        // Coroutine vẫn phải đợi callback mới được resume.
    }
~~~

Adapter này tránh resume hai lần nhưng không quan sát cancellation của caller trong lúc suspend. Cancel caller không tự gọi `cancel()` của API. Callback vẫn có thể resume thành công và code sau lời gọi có thể chạy dù Job đã bị hủy; caller muốn bảo vệ bước tiếp theo phải tự kiểm tra `ensureActive()`.

### Adapter suspendCancellableCoroutine

~~~kotlin
suspend fun FakeUserApi.awaitUserCancellable(): CallbackUser =
    suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean(false)
        val call = fetchUser(object : UserCallback {
            override fun onSuccess(user: CallbackUser) {
                if (completed.compareAndSet(false, true)) continuation.resume(user)
            }

            override fun onError(error: Throwable) {
                if (completed.compareAndSet(false, true)) {
                    continuation.resumeWithException(error)
                }
            }
        })

        continuation.invokeOnCancellation {
            completed.set(true)
            call.cancel()
        }
    }
~~~

`isActive` đơn lẻ không bảo vệ hai callback cùng resume: hai thread có thể đều thấy true. AtomicBoolean chỉ cho một callback kết thúc được quyền complete. Cancellation có thể xảy ra sau khi một callback đã giành quyền complete; prompt cancellation của suspendCancellableCoroutine vẫn bảo vệ caller đang suspend.

Trong adapter thật, thao tác register/cancel phải tuân theo thread-safety của SDK. Kết quả là tài nguyên cần close thì phải xử lý tài nguyên không đến được caller do race cancellation; object dữ liệu immutable trong demo không có yêu cầu close.

### Demo console

~~~kotlin
suspend fun compareCancellation(
    label: String,
    cancellable: Boolean,
    deliverAfterCancel: Boolean
) = coroutineScope {
    println("\n--- $label ---")
    val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val api = FakeUserApi(workerScope, deliverAfterCancel = deliverAfterCancel)
    var uiUpdates = 0

    try {
        val caller = launch {
            val user = if (cancellable) api.awaitUserCancellable()
                       else api.awaitUserWithSuspendCoroutine()
            println("Hàm trả user; caller còn active = " + currentCoroutineContext().isActive)
            // Đặc biệt cần khi dùng suspendCoroutine.
            currentCoroutineContext().ensureActive()
            uiUpdates++
            println("UI nhận " + user.name)
        }

        delay(100) // Cancel trước khi request giả hoàn tất.
        val start = System.nanoTime()
        caller.cancelAndJoin()
        println("cancelAndJoin mất " + (System.nanoTime() - start) / 1_000_000 + " ms")

        delay(600) // Cho callback cố tình đến muộn có thời gian phát.
        println("Số lần API.cancel = " + api.cancelCalls.get())
        println("Số lần cập nhật UI = $uiUpdates")
    } finally {
        workerScope.cancel()
    }
}

fun main() = runBlocking {
    compareCancellation("suspendCoroutine", cancellable = false, deliverAfterCancel = false)
    compareCancellation("cancellable", cancellable = true, deliverAfterCancel = false)
    compareCancellation("callback đến muộn", cancellable = true, deliverAfterCancel = true)
}
~~~

### Kết quả cần quan sát

| Biến thể | API.cancel | Caller sau cancel | Cập nhật UI |
|----------|------------|-------------------|-------------|
| suspendCoroutine | 0 | Đợi callback khoảng thời gian còn lại; ensureActive chặn bước tiếp theo | 0 nhờ kiểm tra ở caller |
| Cancellable, API tôn trọng cancel | 1 | Kết thúc nhanh, không chờ đủ 500 ms | 0 |
| Cancellable, API vẫn phát callback muộn | 1 | Kết thúc nhanh; callback muộn bị adapter bỏ qua | 0 |

Thời gian là xấp xỉ, phụ thuộc lập lịch. Cancellation trước callback là điều kiện của demo. Khi API thành công **trước** cancellation và caller đã cập nhật UI, cancellation sau đó không thể hoàn tác cập nhật.

Để thử thành công và callback lặp: không cancel caller; thay `delay(100) + caller.cancelAndJoin()` bằng `caller.join()`. Cả hai adapter trả user một lần; callback lỗi thứ hai bị bỏ qua, không có IllegalStateException do resume lặp.

### Tự kiểm tra

- Caller cancel trước callback: adapter cancellable nối được tới API.cancel.
- API bỏ qua cancel: callback đến muộn không cập nhật UI.
- Success rồi error, hoặc hai callback cùng lúc: chỉ hoàn tất continuation một lần.
- Chỉ gọi `isActive` rồi resume là chưa đủ cho trường hợp callback đồng thời.
- Không dùng scope của caller làm workerScope của fake API: nếu làm vậy, hủy Job cha có thể tự hủy fake request và che mất sự khác biệt cần học.

---

## 7. StateFlow và SharedFlow khi dừng collector

### Hướng giải

Tách ba trường hợp: state có giá trị hiện tại, event replay = 0 không giữ khi không có subscriber, và event replay = 1 giữ giá trị nhưng collector mới có thể xử lý lại. Dừng collector bằng cancelAndJoin trước khi emit để tránh kết quả phụ thuộc thời điểm subscriber cũ còn sống.

### Demo console

~~~kotlin
fun main() = runBlocking {
    val state = MutableStateFlow(0)
    val events = MutableSharedFlow<String>() // replay = 0
    val bufferedEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val replayEvents = MutableSharedFlow<String>(replay = 1)

    val firstStateCollector = launch(start = CoroutineStart.UNDISPATCHED) {
        state.collect { println("State collector 1: $it") }
    }
    val firstEventCollector = launch(start = CoroutineStart.UNDISPATCHED) {
        events.collect { println("Event collector 1: $it") }
    }

    state.value = 1
    yield()
    events.emit("A: khi UI đang hoạt động")

    firstStateCollector.cancelAndJoin()
    firstEventCollector.cancelAndJoin()
    check(events.subscriptionCount.value == 0)

    println("--- UI dừng ---")
    state.value = 2
    state.value = 3
    events.emit("B: khi UI dừng")
    bufferedEvents.emit("D: extraBufferCapacity không giữ event khi không có subscriber")
    replayEvents.emit("R: sự kiện được replay")

    println("--- UI quay lại ---")
    val secondStateCollector = launch(start = CoroutineStart.UNDISPATCHED) {
        state.collect { println("State collector 2: $it") }
    }
    val secondEventCollector = launch(start = CoroutineStart.UNDISPATCHED) {
        events.collect { println("Event collector 2: $it") }
    }
    val bufferCollector = launch(start = CoroutineStart.UNDISPATCHED) {
        bufferedEvents.collect { println("Buffer collector: $it") }
    }
    val replayCollector1 = launch(start = CoroutineStart.UNDISPATCHED) {
        replayEvents.collect { println("Replay collector 1 xử lý: $it") }
    }

    events.emit("C: sự kiện mới")
    bufferedEvents.emit("E: sự kiện mới")
    replayCollector1.cancelAndJoin()

    val replayCollector2 = launch(start = CoroutineStart.UNDISPATCHED) {
        replayEvents.collect { println("Replay collector 2 xử lý lại: $it") }
    }

    secondStateCollector.cancelAndJoin()
    secondEventCollector.cancelAndJoin()
    bufferCollector.cancelAndJoin()
    replayCollector2.cancelAndJoin()
}
~~~

`CoroutineStart.UNDISPATCHED` giúp collector bắt đầu ngay và đăng ký trước khi tiếp tục emit trong demo. Không cần dùng cách này cho mọi collector trong app; ở đây nhằm làm thí nghiệm dễ quan sát, không dùng delay tùy ý để đoán đã subscribe hay chưa.

### Kết quả

- State collector 1 nhận giá trị ban đầu 0 và cập nhật 1.
- Khi UI dừng, state chuyển 2 → 3. Collector 2 nhận **3**, không nhận lịch sử 2.
- Event collector 2 không nhận B; nó chỉ nhận C phát sau khi đăng ký.
- Buffer collector không nhận D dù extraBufferCapacity = 64; nó nhận E.
- Replay collector 1 và replay collector 2 cùng nhận R, dù không có lần emit R mới giữa hai collection.

Thứ tự in giữa các nguồn có thể thay đổi; các quan hệ trên mới là điều cần kiểm tra. StateFlow giữ state trong bộ nhớ, không tự phục hồi qua process death. SharedFlow không phải hàng đợi bền vững.

### Chọn state hay event

| Nhu cầu | Hướng chọn | Lý do |
|---------|------------|-------|
| Danh sách, số giây còn lại, loading | StateFlow | UI mới cần giá trị hiện tại |
| Lỗi refresh cần hiển thị khi UI quay lại | Error trong UI state | Không mất chỉ vì UI tạm dừng |
| Snackbar có thể bỏ qua khi UI không hoạt động | SharedFlow replay = 0 | Sự kiện tức thời, chấp nhận bị mất |
| Kết quả thao tác quan trọng cần xác nhận | State có mã kết quả + thao tác xác nhận | Có thể giữ đến khi UI xử lý |
| Sự kiện cần xử lý đúng một lần qua process death | Thiết kế persistence/idempotency phù hợp | Chỉ đổi replay không đủ |

Ví dụ xác nhận message bằng state:

~~~kotlin
data class PendingMessage(val id: Long, val text: String)
data class MessageUiState(val pendingMessage: PendingMessage? = null)

// Trong ViewModel: id phải ổn định và duy nhất theo nguồn kết quả của ứng dụng.
private val _messageState = MutableStateFlow(MessageUiState())
val messageState: StateFlow<MessageUiState> = _messageState.asStateFlow()

fun publishMessage(message: PendingMessage) {
    _messageState.update { it.copy(pendingMessage = message) }
}

fun acknowledgeMessage(id: Long) {
    _messageState.update { state ->
        if (state.pendingMessage?.id == id) state.copy(pendingMessage = null)
        else state
    }
}
~~~

UI chỉ xác nhận sau khi xử lý. So sánh id giúp một acknowledgement cũ không xóa message mới. Một slot pendingMessage không phải queue: nếu cần giữ nhiều message liên tiếp thì dùng cấu trúc hàng đợi và persistence theo yêu cầu. Mẫu này cũng không tự bảo đảm xử lý đúng một lần nếu process chết giữa side effect và acknowledge.

### Tự kiểm tra

- Cancel và join collector cũ trước khi phát event lúc UI dừng.
- StateFlow gán lại cùng giá trị không phát state mới theo equals.
- SharedFlow replay = 0 mất event không có subscriber.
- extraBufferCapacity không thay replay.
- replay = 1 có thể làm snackbar/navigation chạy lại khi collector đăng ký mới.
- Với Android, dùng lifecycle của View để tái hiện STOPPED/STARTED; đừng chỉ dừng Activity nhưng để collector chạy liên tục trong lifecycleScope.

---

## 8. Đáp án câu hỏi cuối buổi

### 1. Suspend nhưng Thread.sleep trên Main có làm đứng UI không?

Có. `suspend` cho phép một hàm có khả năng suspend, không chuyển thread hoặc thay code blocking. `Thread.sleep` vẫn block thread đang chạy; dùng `delay` cho chờ thời gian, hoặc chuyển code blocking phù hợp sang IO.

### 2. ViewModel còn sống nhưng View của Fragment bị hủy, dùng scope nào?

UI collection thuộc `viewLifecycleOwner.lifecycleScope` và `viewLifecycleOwner.repeatOnLifecycle(STARTED)`. Công việc/data của ViewModel thuộc viewModelScope. Không dùng lifecycle của Fragment để giữ coroutine cập nhật binding của View cũ.

### 3. Async throw trước await có thể hủy cha không?

Có, nếu async là child của Job thông thường. Lỗi được giữ trong Deferred nhưng vẫn có thể lan lên Job cha. Dùng try/catch ngoài coroutineScope cho nhóm phải thành công toàn bộ; supervisorScope và xử lý từng await cho nhóm độc lập. Cancellation từ ngoài vẫn phải được ném lại.

### 4. ExtraBufferCapacity = 64 có giữ 64 event khi không có subscriber không?

Không. Khi không có subscriber, chỉ replay giữ giá trị cho subscriber tương lai. extraBufferCapacity hỗ trợ subscriber đang hoạt động nhưng xử lý chậm.

### 5. TrySend thất bại khi nào, conflation phù hợp khi nào?

Channel đóng hoặc buffer đầy có thể làm trySend thất bại. Conflation phù hợp state/snapshot mới nhất, như query hiện tại, progress hoặc vị trí hiện tại. Nghiệp vụ bắt buộc xử lý mọi event cần chính sách giữ đủ và backpressure khác.

### 6. Response<Unit> HTTP 403 đã trả về, có thành công không?

Không. Response đã được nhận chỉ cho biết client có response; phải kiểm tra `isSuccessful`/status. Không xóa local hay báo thành công trước khi xác nhận kết quả HTTP.

---

## Tài liệu đối chiếu

| Nội dung | Tài liệu chính thức |
|----------|---------------------|
| Scope, cancellation và lỗi | [Kotlin coroutine exception handling](https://kotlinlang.org/docs/exception-handling.html) |
| Flow, debounce và các operators | [Kotlin Flow](https://kotlinlang.org/docs/flow.html) |
| Adapter callback nhiều kết quả | [callbackFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html) |
| Callback một kết quả có cancellation | [suspendCancellableCoroutine](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html) |
| Replay và buffer | [SharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/) |
| UI lifecycle | [Android lifecycle-aware coroutines](https://developer.android.com/topic/libraries/architecture/coroutines) |
| Room observable queries | [Android asynchronous DAO queries](https://developer.android.com/training/data-storage/room/async-queries) |
| Firebase listener | [Firebase read/write on Android](https://firebase.google.com/docs/database/android/read-and-write) |
| Location registration và removal | [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient) |
| Network capabilities và callback | [Android read network state](https://developer.android.com/develop/connectivity/network-ops/reading-network-state) |

**Phạm vi kiểm tra tài liệu:** các đáp án là code tham khảo trong Markdown; cần tự triển khai và chạy trong project tương ứng, đặc biệt permission/lifecycle/Room/Firebase. Không suy ra các ví dụ đã được chạy trên thiết bị chỉ từ việc chúng xuất hiện trong tài liệu.

package DayOne

// Bài 3: Generic Resource Wrapper
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

data class User(val id: Int, val name: String)

fun fetchUserProfile(userId: Int): Resource<User> {
    return if (userId > 0) {
        Resource.Success(User(userId, "Nguyễn Văn Cường"))
    } else {
        Resource.Error("ID không hợp lệ! ID phải lớn hơn 0.")
    }
}

fun main() {
    println("--- Test Bài 3: Generic Resource ---")

    val stateLoading: Resource<User> = Resource.Loading
    val stateSuccess = fetchUserProfile(1)
    val stateError = fetchUserProfile(-1)

    fun handleState(resource: Resource<User>) {
        when (resource) {
            is Resource.Loading -> println("Đang tải dữ liệu...")
            is Resource.Success -> println("Thành công: ${resource.data}")
            is Resource.Error -> println("Lỗi: ${resource.message}")
        }
    }

    handleState(stateLoading)
    handleState(stateSuccess)
    handleState(stateError)
}

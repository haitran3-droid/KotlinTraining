package DayOne

// Bài 7: Producer với out (Covariance)
open class Animal(val name: String)
class Dog(name: String, val breed: String) : Animal(name)

interface ReadOnlyRepository<out T> {
    fun getAll(): List<T>
    fun getById(id: Int): T?
}

class DogRepository : ReadOnlyRepository<Dog> {
    private val dogs = listOf(
        Dog("Milu", "Corgi"),
        Dog("Kiki", "Husky")
    )

    override fun getAll(): List<Dog> = dogs

    override fun getById(id: Int): Dog? = dogs.getOrNull(id)
}

fun main() {
    println("--- Test Bài 7: Covariance với out ---")

    val dogRepo: ReadOnlyRepository<Dog> = DogRepository()
    val animalRepo: ReadOnlyRepository<Animal> = dogRepo

    val animals: List<Animal> = animalRepo.getAll()
    for (animal in animals) {
        println("Động vật: ${animal.name}")
    }
}

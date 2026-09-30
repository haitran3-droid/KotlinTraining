package DayOne

// Bài 8: Consumer với in (Contravariance)
interface Printer<in T> {
    fun printInfo(item: T)
}

class AnimalPrinter : Printer<Animal> {
    override fun printInfo(item: Animal) {
        println("AnimalPrinter: Đang in thông tin của con vật tên là '${item.name}'")
    }
}

fun main() {
    println("--- Test Bài 8: Contravariance với in ---")

    val animalPrinter: Printer<Animal> = AnimalPrinter()
    val dogPrinter: Printer<Dog> = animalPrinter

    val myDog = Dog("Lucky", "Golden Retriever")
    dogPrinter.printInfo(myDog)
}

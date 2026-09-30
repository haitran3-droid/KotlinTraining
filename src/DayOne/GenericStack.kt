package DayOne

// Bài 1: Generic Stack
class MyStack<T> {
    private val elements = mutableListOf<T>()

    fun push(item: T) {
        elements.add(item)
    }

    fun pop(): T? {
        if (isEmpty()) return null
        return elements.removeAt(elements.size - 1)
    }

    fun peek(): T? {
        return elements.lastOrNull()
    }

    fun isEmpty(): Boolean {
        return elements.isEmpty()
    }

    fun size(): Int {
        return elements.size
    }
}

fun main() {
    println("--- Test Bài 1: MyStack ---")

    val intStack = MyStack<Int>()
    intStack.push(10)
    intStack.push(20)
    intStack.push(30)
    println("Peek: ${intStack.peek()}")
    println("Pop: ${intStack.pop()}")
    println("Size còn lại: ${intStack.size()}")

    val stringStack = MyStack<String>()
    stringStack.push("Android")
    stringStack.push("Kotlin")
    println("Pop: ${stringStack.pop()}")
}

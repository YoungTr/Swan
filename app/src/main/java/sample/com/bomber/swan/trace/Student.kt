package sample.com.bomber.swan.trace

/**
 * @author youngtr
 * @data 2022/6/3
 */
data class Student(var name: String, var age: Int) {
    fun sayHi() {
        val a = 3
        val b = 5
        val c = 6
    }

    fun sayGo() {

    }

    fun sayCome() {
        sayHi()
        sayGo()
    }
}

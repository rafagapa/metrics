package pl.minimal.metrics

class Logger(name: String) {
    private val name = name.take(10).padStart(10, ' ')
    fun trace(msg: String) {
        print("trac $name: ")
        println(msg)
    }
    fun info(msg: String) {
        print("info $name: ")
        println(msg)
    }
    fun warn(msg: String) {
        System.err.print("warn $name: ")
        System.err.println(msg)
    }
}
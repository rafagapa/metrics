package pl.minimal.metrics

interface Logger {
    fun trace(msg: String)
    fun info(msg: String)
    fun warn(msg: String)
    fun trace(msg: () -> String)
    fun info(msg: () -> String)
    fun warn(msg: () -> String)
}

class NoopLogger : Logger {
    override fun trace(msg: String) {}
    override fun info(msg: String) {}
    override fun warn(msg: String) {}
    override fun trace(msg: () -> String) {}
    override fun info(msg: () -> String) {}
    override fun warn(msg: () -> String) {}
}

class ConsoleLogger(name: String) : Logger {
    private val name = name.take(10).padStart(10, ' ')
    override fun trace(msg: String) {
        print("trac $name: ")
        println(msg)
    }
    override fun info(msg: String) {
        print("info $name: ")
        println(msg)
    }
    override fun warn(msg: String) {
        System.err.print("warn $name: ")
        System.err.println(msg)
    }
    override fun trace(msg: () -> String) = trace(msg())
    override fun info(msg: () -> String) = info(msg())
    override fun warn(msg: () -> String) = warn(msg())
}

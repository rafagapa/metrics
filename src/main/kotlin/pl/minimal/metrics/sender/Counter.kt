package pl.minimal.metrics.sender

import pl.minimal.metrics.ConsoleLogger
import pl.minimal.metrics.Logger

interface Counter {
    fun next(): Int
    fun commit(counter: Int)
}

class SimpleCounter(private val logger: Logger = ConsoleLogger("counter")) : Counter {

    private var counter = 0

    override fun next(): Int = counter++

    override fun commit(counter: Int) {
        logger.info("Committed counter $counter")
    }
}
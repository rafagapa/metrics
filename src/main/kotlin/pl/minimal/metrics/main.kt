package pl.minimal.metrics

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Missing base path for log files")
        exitProcess(1)
    }
    val writer = LogsFileWriter(path = args[0])
    val receiver = LogsReceiver(handler = writer)
    val shutdown = Thread{
        receiver.close()
    }.also { it.name = "shutdown-hook" }
    Runtime.getRuntime().addShutdownHook(shutdown)
}
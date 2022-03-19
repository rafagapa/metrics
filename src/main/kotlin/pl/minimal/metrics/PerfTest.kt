package pl.minimal.metrics

import java.net.InetAddress
import java.util.UUID
import kotlin.random.Random

@Volatile
private var running = true

fun main() {
    val senders = (0 until 100).map { UUID.randomUUID() }.map { guid ->
        guid to UdpLogsSender(guid, InetAddress.getLocalHost(), 4445)
    }
    while (running) {
        val (guid, sender) = senders.random()
        sender.send(guid.toString().repeat(Random.nextInt(10)))
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        running = false
        senders.forEach { it.second.close() }
    })
}
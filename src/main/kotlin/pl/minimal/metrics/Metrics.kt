package pl.minimal.metrics

import java.net.DatagramPacket
import java.net.DatagramSocket

private var running = true

fun main() {
    println("Hello")

    val socket = DatagramSocket(4445)
    // Max size (imposed by underlying IP protocol is 65,507, but let's round up
    val buf = ByteArray(65535)
    val packet = DatagramPacket(buf, buf.size)
    while(running) {
        socket.receive(packet)
        println("Received from ${packet.address}:${packet.port}, length: ${packet.length}")
        val message = String(packet.data, packet.offset, packet.length)
        print(message)
    }
}

package pl.minimal.metrics

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun main() {
    val socket = DatagramSocket()
    repeat(10) { i ->
        val buf = "Hello! - $i".toByteArray()
//        val packet = DatagramPacket(buf, buf.size, InetAddress.getLocalHost(), 4445)
//        val packet = DatagramPacket(buf, buf.size, InetAddress.getByName("192.168.0.101"), 4445)
        val packet = DatagramPacket(buf, buf.size, InetAddress.getByName("minimal-games.pl"), 4445)
        socket.send(packet)
        Thread.sleep(1000)
    }
    socket.close()
}

object Sender {
}
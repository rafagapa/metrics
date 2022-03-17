package pl.minimal.metrics

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class Receiver(private val port: Int, private val path: String, private val logger: Logger = Logger("receiver")) {
    private var running = true

    fun run() {
        val socket = DatagramSocket(port)
        // Max size (imposed by underlying IP protocol is 65,507, but let's round up
        val buf = ByteArray(65535)
        val packet = DatagramPacket(buf, buf.size)
        while (running) {
            socket.receive(packet)
            //println("Received from ${packet.address}:${packet.port}, length: ${packet.length}")
            val message = String(packet.data, packet.offset, packet.length)
            print(message)
        }
    }
}

class TestReceiver(private val port: Int = 4445, private val logger: Logger = Logger("receiver")) : Runnable {
    private val thread = Thread(this, "receiver").also { it.start() }

    @Volatile
    private var running = true

    @Volatile
    private var sock: DatagramSocket? = null

    val results = ConcurrentLinkedQueue<Pair<UUID, String>>()

    override fun run() {
        logger.info("Started, port: $port")
        val socket = DatagramSocket(port)
        sock = socket
        val buffer = ByteBuffer.allocate(65535)
        val packet = DatagramPacket(buffer.array(), buffer.capacity())
        while (running) {
            try {
                socket.receive(packet)
            } catch (e: InterruptedException) {
                logger.info("Interrupted, we are about to stop")
                break
            } catch (e: SocketException) {
                logger.info("Socket closed, we are about to stop: ${e.message}")
                break
            }
            if (packet.length < 16) {
                logger.info("Received invalid from ${packet.address}:${packet.port}, length: ${packet.length}, dropping")
            } else {
                logger.info("Received from ${packet.address}:${packet.port}, length: ${packet.length}")
                buffer.position(packet.offset)
                val uuid = UUID(buffer.long, buffer.long)
                val message = String(packet.data, packet.offset + 16, packet.length - 16)
                logger.info("Received message: $message")
                results.add(uuid to message)
            }

        }
        sock?.close()
        sock = null
    }

    fun stop() {
        logger.info("Stopping")
        running = false
        // thread.interrupt doesn't interrupt thread blocked on socket, the only way is to close the socket
        sock?.close()
        sock = null
        thread.interrupt()
        thread.join()
        logger.info("Stopped")
    }
}
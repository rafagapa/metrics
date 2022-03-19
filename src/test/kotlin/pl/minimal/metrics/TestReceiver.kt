package pl.minimal.metrics

import pl.minimal.metrics.UdpLogsSender.Companion.MAGIC_NUMBER
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class TestReceiver(private val port: Int = 4445, private val logger: ConsoleLogger = ConsoleLogger("receiver")) :
    Runnable, AutoCloseable {
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
            logger.info("Received from ${packet.address}:${packet.port}, length: ${packet.length}")
            buffer.position(packet.offset)
            buffer.limit(packet.length - packet.offset)
            val magic = buffer.int
            if (magic == MAGIC_NUMBER) {
                val uuid = UUID(buffer.long, buffer.long)
                val message = String(buffer.array(), buffer.position(), buffer.remaining())
                logger.info("Received message: $message")
                results.add(uuid to message)
            } else {
                logger.info(
                    "Received invalid from ${packet.address}:${packet.port}, invalid header (expected: %h, got %h)"
                        .format(MAGIC_NUMBER, magic)
                )
            }
        }
        sock?.close()
        sock = null
    }

    override fun close() {
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
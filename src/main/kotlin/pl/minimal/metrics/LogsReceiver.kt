package pl.minimal.metrics

import pl.minimal.metrics.UdpLogsSender.Companion.HEADER_SIZE
import pl.minimal.metrics.UdpLogsSender.Companion.MAGIC_NUMBER
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.UUID

class LogsReceiver(
    private val port: Int = 4445,
    private val handler: LogsHandler,
    private val logger: ConsoleLogger = ConsoleLogger("receiver")
) : Runnable, AutoCloseable {

    private val thread = Thread(this, "receiver").also { it.start() }

    @Volatile
    private var running = true

    @Volatile
    private var sock: DatagramSocket? = null

    override fun run() {
        logger.info("Started, port: $port")
        val socket = DatagramSocket(port)
        sock = socket
        // Max packet size is 65507, but let's round up
        val buffer = ByteBuffer.allocate(65535)
        val packet = DatagramPacket(buffer.array(), buffer.capacity())
        while (running) {
            try {
                socket.receive(packet)
            } catch (e: InterruptedException) {
                logger.info("Interrupted, we are about to stop: ${e.message}")
                break
            } catch (e: SocketException) {
                logger.info("Socket closed, we are about to stop: ${e.message}")
                break
            }
            if (packet.length <= HEADER_SIZE) {
                logger.info("Received invalid from ${packet.address}:${packet.port}, length: ${packet.length} is not greater than $HEADER_SIZE, dropping")
            } else {
                logger.info("Received from ${packet.address}:${packet.port}, length: ${packet.length}")
                buffer.position(packet.offset)
                buffer.limit(packet.length - packet.offset)
                val magic = buffer.int
                if (magic == MAGIC_NUMBER) {
                    val uuid = UUID(buffer.long, buffer.long)
                    handler.write(uuid, buffer)
                } else {
                    logger.info(
                        "Received invalid from ${packet.address}:${packet.port}, invalid header (expected: %h, got %h)"
                            .format(MAGIC_NUMBER, magic)
                    )
                }
            }

        }
        sock?.close()
        sock = null
        logger.info("Stopped")
    }

    override fun close() {
        logger.info("Stopping")
        running = false
        // thread.interrupt doesn't interrupt thread blocked on socket, the only way is to close the socket
        sock?.close()
        sock = null
        thread.interrupt()
    }
}
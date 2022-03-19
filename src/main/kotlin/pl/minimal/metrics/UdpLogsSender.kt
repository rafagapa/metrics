package pl.minimal.metrics

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue

interface LogsSender {
    fun send(msg: String)
    fun close()
}

class UdpLogsSender(
    private val guid: UUID,
    private val host: InetAddress,
    private val port: Int,
    private val logger: Logger = NoopLogger(),
    private val bufferSize: Int = 65507 // Max size (imposed by underlying IP protocol) is 65507
) : LogsSender {

    private val headerSize = 16                              // length of UUID
    internal val maxMessageSize = bufferSize - headerSize -1 // minus one for newline character
    private val newline: Byte = 0x0A                         // '\n'.toByte() is deprecated

    private val thread = Thread(this::run, "logs-sender")
    private var buffer = createBuffer()
    private val queue = ArrayBlockingQueue<ByteBuffer>(2)

    @Volatile
    private var running = true

    init {
        if (bufferSize > 65507) {
            throw IllegalArgumentException("Invalid buffer size ($bufferSize), maximum UDP packet size is 65507")
        }
        thread.start()
    }

    private fun createBuffer() = ByteBuffer.allocate(bufferSize).also {
        it.putLong(guid.mostSignificantBits)
        it.putLong(guid.leastSignificantBits)
    }

    private fun run() {
        try {
            DatagramSocket().use { socket ->
                while (running || queue.peek() != null) {
                    val buffer = queue.take()
                    val packet = DatagramPacket(buffer.array(), buffer.position(), host, port)
                    socket.send(packet)
                    logger.info("Sent ${buffer.position()} bytes to $host, port: $port")
                }
            }
        } catch (e: Exception) {
            logger.warn("Thread failed, ${e.javaClass.simpleName}: ${e.message}")
        } finally {
            // to make sure queue is no longer flooded with data
            running = false
        }
    }

    override fun send(msg: String) {
        if (!running) {
            logger.warn("Message dropped, thread is no longer running, message: $msg")
            return
        }
        val bytes = msg.toByteArray()
        if (bytes.size > maxMessageSize) {
            logger.warn("Message dropped, message size (${bytes.size}) is larger than max message size ($maxMessageSize)")
            return
        }
        val required = bytes.size + 1 // one extra fo end of line
        if (buffer.remaining() < required) {
            if (!queue.offer(buffer)) {
                logger.warn("Failed to push buffer to queue, dropping message: $msg")
                return
            }
            buffer = createBuffer()
        }
        buffer.put(bytes)
        buffer.put(newline)
    }

    override fun close() {
        if (buffer.position() > headerSize) {
            if (!queue.offer(buffer)) {
                logger.warn("Failed to push buffer to queue, dropping ${buffer.position()} bytes")
            }
        }
        running = false
    }
}
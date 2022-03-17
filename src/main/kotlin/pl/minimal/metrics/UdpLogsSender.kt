package pl.minimal.metrics

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.UUID

interface LogsSender {
    fun send(msg: String)
    fun close()
}

class UdpLogsSender(
    private val guid: UUID,
    private val host: InetAddress,
    private val port: Int,
    private val logger: Logger = Logger("sender")
) : LogsSender, Runnable {

    private var lastSentMs = 0L
    private val maxIntervalMs = 1000L

    // Using char '\n' would put extra character in the buffer, '\n'.toByte() is deprecated
    private val newline: Byte = 0x0A

    @Volatile
    private var running = true

    // Max size (imposed by underlying IP protocol) is 65,507
    private val first = ByteBuffer.allocate(65507)
    private val second = ByteBuffer.allocate(65507)

    private data class Buffers(val send: ByteBuffer, val write: ByteBuffer) {
        fun swap() = Buffers(write, send)
    }

    @Volatile
    private var buffers = Buffers(first, second)

    private val thread = Thread(this, "logs-sender").also { it.start() }

    private fun swap() {
        buffers = buffers.swap()
    }

    init {
        prepareWriteBuffer(buffers.write)
        prepareWriteBuffer(buffers.send)
    }

    override fun send(msg: String) {
        if (running) {
            val bytes = msg.toByteArray()
            if (buffers.write.remaining() > bytes.size) {
                buffers.write.put(bytes)
                buffers.write.put(newline)
                logger.trace("Written message: $msg, write buffer position: ${buffers.write.position()}")
            } else {
                logger.warn("Write buffer is full (remaining: ${buffers.write.remaining()}), dropping message: $msg")
            }
        } else {
            logger.warn("Running is false, dropping message: $msg")
        }
    }

    override fun close() {
        logger.info("Closing")
        running = false
        thread.interrupt()
    }

    override fun run() {
        DatagramSocket().use { socket ->
            logger.info("Started, address: $host, port: $port")
            while (running) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    // do nothing, just try to send the last chunk
                    logger.info("Interrupted, we are probably about to close")
                }
                val elapsedMs = System.currentTimeMillis() - lastSentMs
                if (buffers.write.position() > 16 &&
                    (
                            buffers.write.position() > buffers.write.capacity() / 2 ||
                                    elapsedMs > maxIntervalMs ||
                                    !running // last run before closing, send what's remaining
                            )
                ) {
                    logger.info("Sending, write buffer position: ${buffers.write.position()}, last sent: ${elapsedMs / 1000}s ago, running: $running")
                    swap()
                    val packet = DatagramPacket(buffers.send.array(), buffers.send.position(), host, port)
                    socket.send(packet)
                    // prepare send buffer to be used in place of write buffer
                    prepareWriteBuffer(buffers.send)
                    lastSentMs = System.currentTimeMillis()
                }
            }
        }
    }

    private fun prepareWriteBuffer(buffer: ByteBuffer) {
        buffer.clear()
        buffer.putLong(guid.mostSignificantBits)
        buffer.putLong(guid.leastSignificantBits)
    }
}
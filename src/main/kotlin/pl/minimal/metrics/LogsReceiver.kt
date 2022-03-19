package pl.minimal.metrics

import pl.minimal.metrics.UdpLogsSender.Companion.HEADER_SIZE
import pl.minimal.metrics.UdpLogsSender.Companion.MAGIC_NUMBER
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Missing base path for log files")
        exitProcess(1)
    }
    val receiver = LogsReceiver(path = args[0])
    val shutdown = Thread{
        receiver.close()
    }.also { it.name = "shutdown-hook" }
    Runtime.getRuntime().addShutdownHook(shutdown)
}

class LogsReceiver(
    private val port: Int = 4445,
    path: String,
    private val logger: ConsoleLogger = ConsoleLogger("receiver")
) : Runnable, AutoCloseable {

    private val thread = Thread(this, "receiver").also { it.start() }

    @Volatile
    private var running = true

    @Volatile
    private var sock: DatagramSocket? = null

    private val base = File(path).also {
        if (!it.exists()) {
            if (it.mkdirs()) {
                logger.info("Created base directory ${it.absolutePath}")
            } else {
                throw IllegalStateException("Failed to create base directory: ${it.absolutePath}")
            }
        }
        if (!it.isDirectory) {
            throw IllegalStateException("Base path is not a directory: ${it.absolutePath}")
        }
        if (!it.canWrite()) {
            throw IllegalStateException("Don't have write access to base path: ${it.absolutePath}")
        }
        logger.info("Base path: ${it.absolutePath}")
    }

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
                    write(uuid, buffer)
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

    private fun write(guid: UUID, buffer: ByteBuffer) {
        val file = File(base, guid.toString())
        FileOutputStream(file, true).use {
            it.write(buffer.array(), buffer.position(), buffer.remaining())
        }
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
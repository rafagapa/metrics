package pl.minimal.metrics

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.UUID

interface LogsHandler {
    fun write(guid: UUID, counter: Int, buffer: ByteBuffer)
}

class LogsFileWriter(
    path: String,
    private val logger: ConsoleLogger = ConsoleLogger("file writer")
) : LogsHandler {
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

    override fun write(guid: UUID, counter: Int, buffer: ByteBuffer) {
        val file = File(base, guid.toString())
        FileOutputStream(file, true).use {
            it.write(("# " + LocalDateTime.now() + " (counter: $counter)\n").toByteArray())
            it.write(buffer.array(), buffer.position(), buffer.remaining())
        }
    }
}

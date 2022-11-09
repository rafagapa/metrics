package pl.minimal.metrics

import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class TestLogsCollector : LogsHandler {
    val results = ConcurrentLinkedQueue<Pair<UUID, String>>()

    override fun write(guid: UUID, buffer: ByteBuffer) {
        val message = String(buffer.array(), buffer.position(), buffer.remaining())
        results.add(guid to message)
    }
}
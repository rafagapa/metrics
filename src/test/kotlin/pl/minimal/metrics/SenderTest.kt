package pl.minimal.metrics

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.framework.concurrency.EventuallyConfig
import io.kotest.framework.concurrency.eventually
import io.kotest.matchers.shouldBe
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalKotest::class)
class SenderTest : FunSpec() {
    val port = 4567

    init {
        test("should handle UUID") {
            val arr = ByteArray(100)
            val buf = ByteBuffer.wrap(arr)
            val longBuffer = buf.asLongBuffer()
            val uuid = UUID.randomUUID()
            longBuffer.put(uuid.mostSignificantBits)
            longBuffer.put(uuid.leastSignificantBits)

            val uuid2 = UUID(buf.long, buf.long)

            uuid shouldBe uuid2
        }

        test("should silently drop messages if buffer is full") {
//            val receiver = TestReceiver(port)
//            val sender = UdpLogsSender(UUID.randomUUID(), "localhost", port)

        }

        test("should write each user to it's own file") {
            val receiver = TestReceiver(port)
            val sender = UdpLogsSender(UUID.randomUUID(), InetAddress.getLocalHost(), port)
            Thread.sleep(100)
            sender.send("jeden")
            sender.send("dwa")
            Thread.sleep(100)
            sender.send("trzy")
            sender.send("cztery")
            eventually(5.seconds) {
//            Thread.sleep(2000)
                receiver.results.joinToString(""){it.second} shouldBe """
                    jeden
                    dwa
                    trzy
                    cztery
                    
                """.trimIndent()
                // todo: configure interval
                Thread.sleep(100)
            }
            sender.close()
            receiver.stop()
        }

        test("should ignore message with invalid uuid") {

        }

        test("should write message counters") {

        }
    }
}
package pl.minimal.metrics

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.framework.concurrency.eventually
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.net.InetAddress
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalKotest::class)
class UdpLogsSenderTest : FunSpec() {
    private val port = 5000

    init {
        test("should flush buffer on close") {
            TestReceiver(port).use { receiver ->
                val guid = UUID.randomUUID()
                val sender = UdpLogsSender(guid, InetAddress.getLocalHost(), port, logger = ConsoleLogger("sender"))
                // Make sure receiver starts listening
                Thread.sleep(100)

                sender.send("jeden")
                sender.send("dwa")
                receiver.results.shouldBeEmpty()

                // Buffer will be flushed on close
                sender.close()
                eventually(1.seconds) {
                    receiver.results.toList() shouldBe listOf(
                        guid to "jeden\ndwa\n"
                    )
                }
            }
        }

        test("should flush buffer if there is no space for message") {
            TestReceiver(port).use { receiver ->
                val guid = UUID.randomUUID()
                val sender = UdpLogsSender(guid, InetAddress.getLocalHost(), port, logger = ConsoleLogger("sender"))
                // Make sure receiver starts listening
                Thread.sleep(100)

                val bigMessage = "X".repeat(sender.maxMessageSize)
                sender.send(bigMessage)
                sender.send("jeden")
                eventually(1.seconds) {
                    receiver.results.toList() shouldBe listOf(
                        guid to bigMessage + "\n"
                    )
                }
                sender.send(bigMessage)
                eventually(1.seconds) {
                    receiver.results.toList() shouldBe listOf(
                        guid to bigMessage + "\n",
                        guid to "jeden\n"
                    )
                }
                sender.send("dwa")
                eventually(1.seconds) {
                    receiver.results.toList() shouldBe listOf(
                        guid to bigMessage + "\n",
                        guid to "jeden\n",
                        guid to bigMessage + "\n"
                    )
                }
                // Buffer will be flushed on close
                sender.close()
                eventually(1.seconds) {
                    receiver.results.toList() shouldBe listOf(
                        guid to bigMessage + "\n",
                        guid to "jeden\n",
                        guid to bigMessage + "\n",
                        guid to "dwa\n"
                    )
                }
            }
        }

        test("should drop message larger than buffer size") {
            TestReceiver(port).use { receiver ->
                val guid = UUID.randomUUID()
                val sender = UdpLogsSender(guid, InetAddress.getLocalHost(), port, logger = ConsoleLogger("sender"))
                // Make sure receiver starts listening
                Thread.sleep(100)

                val maxMessage = "A".repeat(sender.maxMessageSize)
                sender.send(maxMessage)
                val tooBigMessage = "B".repeat(sender.maxMessageSize + 1)
                sender.send(tooBigMessage)
                sender.send("jeden")
                sender.close()
                eventually(1.seconds) {
                    receiver.results.size shouldBe 2
                    receiver.results.toList() shouldBe listOf(
                        guid to maxMessage + "\n",
                        guid to "jeden\n"
                    )
                }
            }
        }
    }
}
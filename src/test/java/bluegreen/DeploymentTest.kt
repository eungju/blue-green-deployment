package bluegreen

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import rawhttp.core.HttpVersion
import rawhttp.core.RawHttpHeaders
import rawhttp.core.RawHttpRequest
import rawhttp.core.RequestLine
import java.net.URI
import java.util.Random

class DeploymentTest {
    private val publicPort = 1024 + Random().nextInt(1024)

    fun blueServer() = App("blue", publicPort)

    fun greenServer() = App("green", publicPort)

    fun connection() = RawHttpConnection("localhost", publicPort)

    fun pingRequest() = RawHttpRequest(
        RequestLine("GET", URI.create("/"), HttpVersion.HTTP_1_1),
        RawHttpHeaders.newBuilder()
            .with("Host", "localhost:$publicPort")
            .build(),
        null, null)

    @Nested
    inner class WhenOpen {
        @Test
        fun acceptNewConnection() {
            blueServer().use { blue ->
                blue.connectionControl.open()
                connection().use { establishedConn ->
                    assertEquals("blue", establishedConn.request(pingRequest()).body.get().decodeBodyToString(Charsets.UTF_8))
                }
                blue.connectionControl.close()
            }
        }
    }

    @Nested
    inner class WhenClosed {
        @Test
        fun rejectNewConnections() {
            blueServer().use { blue ->
                blue.connectionControl.open()
                connection().use { establishedConn ->
                    establishedConn.request(pingRequest()).body.get()
                }
                blue.connectionControl.close()
                assertThrows(Exception::class.java) { RawHttpConnection("localhost", publicPort) }
            }
        }

        @Test
        fun keepEstablishedConnections() {
            blueServer().use { blue ->
                blue.connectionControl.open()
                connection().use { establishedConn ->
                    establishedConn.request(pingRequest()).body.get()
                    blue.connectionControl.close()
                    assertEquals("blue", establishedConn.request(pingRequest()).body.get().decodeBodyToString(Charsets.UTF_8))
                }
            }
        }

        @Test
        fun handleOneRequestAndThenClose() {
            blueServer().use { blue ->
                blue.connectionControl.open()
                connection().use { establishedConn ->
                    establishedConn.request(pingRequest()).body.get()
                    blue.connectionControl.close()
                    assertEquals("blue", establishedConn.request(pingRequest()).body.get().decodeBodyToString(Charsets.UTF_8))
                    assertFalse(establishedConn.isOpen)
                }
            }
        }
    }

    @Test
    fun handover() {
        blueServer().use { blue ->
            greenServer().use { green ->
                blue.connectionControl.open()
                connection().use { blueConn ->
                    assertEquals("blue", blueConn.request(pingRequest()).body.get().decodeBodyToString(Charsets.UTF_8))
                    green.connectionControl.open()
                    blue.connectionControl.close()
                    connection().use { greenConn ->
                        assertEquals("green", greenConn.request(pingRequest()).body.get().decodeBodyToString(Charsets.UTF_8))
                        assertEquals("blue", blueConn.request(pingRequest()).body.get().decodeBodyToString(Charsets.UTF_8))
                        assertFalse(blueConn.isOpen)
                    }
                }
            }
        }
    }
}

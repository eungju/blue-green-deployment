package bluegreen

import com.athaydes.rawhttp.core.MethodLine
import com.athaydes.rawhttp.core.RawHttpHeaders
import com.athaydes.rawhttp.core.RawHttpRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

class DeploymentTest {
    private val publicPort = 8000

    fun pingRequest() = RawHttpRequest(MethodLine("GET", URI.create("/"), "HTTP/1.1"),
            RawHttpHeaders.Builder.newBuilder()
                    .with("Host", "localhost:$publicPort")
                    .build(),
            null)

    fun blueServer() = App("blue", publicPort)

    fun greenServer() = App("green", publicPort)

    fun connection() = RawHttpConnection("localhost", publicPort)

    @Nested
    inner class WhenOpen {
        @Test
        fun acceptNewConnection() {
            blueServer().use { blue ->
                blue.connectionControl.open()
                connection().use { establishedConn ->
                    assertEquals("blue", establishedConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
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
                    assertEquals("blue", establishedConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
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
                    assertEquals("blue", establishedConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
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
                    assertEquals("blue", blueConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
                    green.connectionControl.open()
                    blue.connectionControl.close()
                    connection().use { greenConn ->
                        assertEquals("green", greenConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
                        assertEquals("blue", blueConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
                        assertFalse(blueConn.isOpen)
                    }
                }
            }
        }
    }
}

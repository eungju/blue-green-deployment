package bluegreen

import com.athaydes.rawhttp.core.MethodLine
import com.athaydes.rawhttp.core.RawHttpHeaders
import com.athaydes.rawhttp.core.RawHttpRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

class DeploymentTest {
    private val servicePort = 8000

    fun pingRequest() = RawHttpRequest(MethodLine("GET", URI.create("/"), "HTTP/1.1"),
            RawHttpHeaders.Builder.newBuilder()
                    .with("Host", "localhost:$servicePort")
                    .build(),
            null)

    @Nested
    inner class WhenOpen {
        @Test
        fun acceptNewConnection() {
            App("blue", servicePort).use { blue ->
                blue.connectionControl.open()
                RawHttpConnection("localhost", servicePort).use { establishedConn ->
                    assertEquals("blue", establishedConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
                }
                blue.connectionControl.close()
            }
        }
    }

    @Nested
    inner class WhenClosed {
        @Test
        fun keepEstablishedConnections() {
            App("blue", servicePort).use { blue ->
                blue.connectionControl.open()
                RawHttpConnection("localhost", servicePort).use { establishedConn ->
                    establishedConn.request(pingRequest()).body.get()
                    blue.connectionControl.close()
                    assertEquals("blue", establishedConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
                }
            }
        }

        @Test
        fun rejectNewConnections() {
            App("blue", servicePort).use { blue ->
                blue.connectionControl.open()
                RawHttpConnection("localhost", servicePort).use { establishedConn ->
                    establishedConn.request(pingRequest()).body.get()
                }
                blue.connectionControl.close()
                assertThrows(Exception::class.java) { RawHttpConnection("localhost", servicePort) }
            }
        }
    }

    @Test
    fun handover() {
        App("blue", servicePort).use { blue ->
            App("green", servicePort).use { green ->
                blue.connectionControl.open()
                RawHttpConnection("localhost", servicePort).use { blueConn ->
                    assertEquals("blue", blueConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
                    green.connectionControl.open()
                    blue.connectionControl.close()
                    RawHttpConnection("localhost", servicePort).use { greenConn ->
                        assertEquals("green", greenConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
                        assertEquals("blue", blueConn.request(pingRequest()).body.get().asString(Charsets.UTF_8))
                    }
                }
            }
        }
    }
}

package bluegreen

import com.athaydes.rawhttp.core.MethodLine
import com.athaydes.rawhttp.core.RawHttpHeaders
import com.athaydes.rawhttp.core.RawHttpRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI

class DeploymentTest {
    private val servicePort = 8000

    fun pingRequest() = RawHttpRequest(MethodLine("GET", URI.create("/"), "HTTP/1.1"),
            RawHttpHeaders.Builder.newBuilder()
                    .with("Host", "localhost:$servicePort")
                    .build(),
            null)

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

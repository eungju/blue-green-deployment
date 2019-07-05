package bluegreen

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Random

class DeploymentTest {
    private val publicPort = 1024 + Random().nextInt(1024)

    fun blueServer() = App("blue", publicPort)

    fun greenServer() = App("green", publicPort)

    fun connection() = RawHttpConnection("localhost", publicPort)

    @Test
    fun trafficHandover() {
        blueServer().use { blue ->
            greenServer().use { green ->
                blue.connectionControl.open()
                connection().use { blueConn ->
                    assertEquals("blue", blueConn.hello().body.get().decodeBodyToString(Charsets.UTF_8))
                    green.connectionControl.open()
                    blue.connectionControl.close()
                    connection().use { greenConn ->
                        assertEquals("green", greenConn.hello().body.get().decodeBodyToString(Charsets.UTF_8))
                        assertEquals("blue", blueConn.hello().body.get().decodeBodyToString(Charsets.UTF_8))
                        assertFalse(blueConn.isOpen)
                    }
                }
            }
        }
    }
}

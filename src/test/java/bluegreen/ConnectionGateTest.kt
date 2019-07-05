package bluegreen

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rawhttp.core.HttpVersion
import rawhttp.core.RawHttpHeaders
import rawhttp.core.RawHttpRequest
import rawhttp.core.RequestLine
import java.net.ConnectException
import java.net.URI
import java.util.Random

abstract class ConnectionGateTest {
    protected val port = Short.MAX_VALUE + Random().nextInt(Short.MAX_VALUE.toInt())
    protected val STATUS_OK = 200

    abstract fun server(f: (ConnectionGate) -> Unit)
    
    fun connect() = RawHttpConnection("localhost", port)

    fun pingRequest() = RawHttpRequest(
        RequestLine("GET", URI.create("/"), HttpVersion.HTTP_1_1),
        RawHttpHeaders.newBuilder()
            .with("Host", "localhost:$port")
            .build(),
        null, null)

    @Test
    fun openAndClose() {
        server { dut ->
            dut.open()
            connect().use {
                assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
                dut.close()
                assertTrue(dut.getState() == ConnectionGate.State.CLOSED)
            }
        }
    }

    @Test
    fun reopen() {
        server { dut ->
            dut.open()
            assertTrue(dut.getState() == ConnectionGate.State.OPEN)
            connect().use {
                assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
            }
            dut.close()
            assertTrue(dut.getState() == ConnectionGate.State.CLOSED)
            dut.open()
            assertTrue(dut.getState() == ConnectionGate.State.OPEN)
            connect().use {
                assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
            }
            dut.close()
        }
    }

    @Test
    fun whenOpenAcceptNewConnection() {
        server { dut ->
            dut.open()
            connect().use {
                assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
            }
            dut.close()
        }
    }

    @Test
    fun whenClosedRejectNewConnections() {
        server { dut ->
            dut.open()
            dut.close()
            assertThrows(ConnectException::class.java) { connect() }
        }
    }

    @Test
    fun whenClosedKeepEstablishedConnections() {
        server { dut ->
            dut.open()
            connect().use {
                assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
                dut.close()
                assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
            }
        }
    }

    @Test
    fun whenClosedCloseConnection() {
        server { dut ->
            dut.open()
            connect().use {
                assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
                dut.close()
                assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
                assertFalse(it.isOpen)
            }
        }
    }

    @Test
    fun trafficHandover() {
        server { blue ->
            server { green ->
                blue.open()
                connect().use { blueConn ->
                    assertEquals(STATUS_OK, blueConn.request(pingRequest()).statusCode)
                    green.open()
                    blue.close()
                    connect().use { greenConn ->
                        assertEquals(STATUS_OK, greenConn.request(pingRequest()).statusCode)
                        assertTrue(greenConn.isOpen)
                        assertEquals(STATUS_OK, blueConn.request(pingRequest()).statusCode)
                        assertFalse(blueConn.isOpen)
                    }
                }
            }
        }
    }
}

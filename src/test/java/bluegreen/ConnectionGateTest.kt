package bluegreen

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ConnectException
import java.util.Random

abstract class ConnectionGateTest {
    protected val port = Short.MAX_VALUE + Random().nextInt(Short.MAX_VALUE.toInt())
    protected val STATUS_OK = 200

    abstract fun server(f: (ConnectionGate) -> Unit)
    
    fun connect() = RawHttpConnection("localhost", port)

    @Test
    fun openAndClose() {
        server { dut ->
            dut.open()
            assertTrue(dut.getState() == ConnectionGate.State.OPEN)
            connect().use {
                assertEquals(STATUS_OK, it.hello().statusCode)
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
                assertEquals(STATUS_OK, it.hello().statusCode)
            }
            dut.close()
            assertTrue(dut.getState() == ConnectionGate.State.CLOSED)
            dut.open()
            assertTrue(dut.getState() == ConnectionGate.State.OPEN)
            connect().use {
                assertEquals(STATUS_OK, it.hello().statusCode)
            }
            dut.close()
        }
    }

    @Test
    fun whenOpenAcceptNewConnection() {
        server { dut ->
            dut.open()
            connect().use {
                assertEquals(STATUS_OK, it.hello().statusCode)
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
                assertEquals(STATUS_OK, it.hello().statusCode)
                dut.close()
                assertEquals(STATUS_OK, it.hello().statusCode)
            }
        }
    }

    @Test
    fun whenClosedCloseConnection() {
        server { dut ->
            dut.open()
            connect().use {
                assertEquals(STATUS_OK, it.hello().statusCode)
                dut.close()
                assertEquals(STATUS_OK, it.hello().statusCode)
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
                    assertEquals(STATUS_OK, blueConn.hello().statusCode)
                    green.open()
                    blue.close()
                    connect().use { greenConn ->
                        assertEquals(STATUS_OK, greenConn.hello().statusCode)
                        assertTrue(greenConn.isOpen)
                        assertEquals(STATUS_OK, blueConn.hello().statusCode)
                        assertFalse(blueConn.isOpen)
                    }
                }
            }
        }
    }
}

package bluegreen

import bluegreen.undertow.UndertowConnectionGate
import io.undertow.Undertow
import io.undertow.UndertowOptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.xnio.OptionMap
import org.xnio.XnioWorker
import org.xnio.nio.NioXnioProvider
import rawhttp.core.HttpVersion
import rawhttp.core.RawHttpHeaders
import rawhttp.core.RawHttpRequest
import rawhttp.core.RequestLine
import java.net.ConnectException
import java.net.URI
import java.time.Clock
import java.util.Random

class ConnectionControlTest {
    private val port = Short.MAX_VALUE + Random().nextInt(Short.MAX_VALUE.toInt())
    private lateinit var worker: XnioWorker
    private lateinit var target: Undertow
    private lateinit var dut: ConnectionControl
    private lateinit var gate: ConnectionGate
    private val HEADER_CONNECTION = "Connection"
    private val STATUS_OK = 200

    @BeforeEach
    fun setUp() {
        worker = NioXnioProvider().instance.createWorker(OptionMap.EMPTY)
        dut = ConnectionControl(Clock.systemUTC())
        target = Undertow.builder()
                .addHttpListener(port, "", {
                    if (dut.getState() is ConnectionControl.State.CLOSED) {
                        it.setPersistent(false)
                    }
                    it.responseSender.send("OK")
                    it.endExchange()
                })
                .setWorker<Nothing>(worker)
                .setServerOption(UndertowOptions.ENABLE_STATISTICS, true)
                .build()
        gate = UndertowConnectionGate(port.toString(), target)
            .also {
                dut.register(it)
            }
    }

    @AfterEach
    fun tearDown() {
        worker.shutdown()
    }

    fun connect() = RawHttpConnection("localhost", port)

    fun pingRequest() = RawHttpRequest(
        RequestLine("GET", URI.create("/"), HttpVersion.HTTP_1_1),
        RawHttpHeaders.newBuilder()
            .with("Host", "localhost:$port")
            .build(),
        null, null)

    @Test
    fun connectionCount() {
        assertEquals(0, dut.getEstablished())
        dut.open()
        connect().use {
            it.request(pingRequest())
            assertEquals(1, dut.getEstablished())
        }
    }

    @Test
    fun open() {
        assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
        assertThrows(ConnectException::class.java) { connect().request(pingRequest()) }
        dut.open()
        assertTrue(dut.getState() is ConnectionControl.State.OPEN)
        connect().use {
            assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
        }
    }

    @Test
    fun openAndClose() {
        dut.open()
        connect().use {
            assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
            dut.close()
            assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
            assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
            assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
        }
        assertThrows(ConnectException::class.java) { connect().request(pingRequest()) }
    }

    @Test
    fun reopenAndClose() {
        dut.open()
        dut.close()
        dut.open()
        assertTrue(dut.getState() is ConnectionControl.State.OPEN)
        connect().use {
            assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
            dut.close()
            assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
            assertEquals(STATUS_OK, it.request(pingRequest()).statusCode)
        }
        assertThrows(ConnectException::class.java) { connect().request(pingRequest()) }
    }

    @Test
    fun closeEstablishedConnections() {
        dut.open()
        connect().use {
            it.request(pingRequest()).let {
                assertEquals(STATUS_OK, it.statusCode)
                assertEquals("keep-alive", it.headers.getFirst(HEADER_CONNECTION).orElse(null))
            }
            dut.close()
            assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
            it.request(pingRequest()).let {
                assertEquals(STATUS_OK, it.statusCode)
                assertEquals("close", it.headers.getFirst(HEADER_CONNECTION).orElse(null))
            }
        }
    }
}

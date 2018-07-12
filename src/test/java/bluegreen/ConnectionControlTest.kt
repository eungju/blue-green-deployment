package bluegreen

import bluegreen.undertow.UndertowConnectionGate
import io.undertow.Undertow
import io.undertow.UndertowOptions
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.xnio.OptionMap
import org.xnio.XnioWorker
import org.xnio.nio.NioXnioProvider
import java.net.ConnectException
import java.time.Clock
import java.util.Random

class ConnectionControlTest {
    private val port = Short.MAX_VALUE + Random().nextInt(Short.MAX_VALUE.toInt())
    private lateinit var worker: XnioWorker
    private lateinit var target: Undertow
    private lateinit var dut: ConnectionControl
    private lateinit var gate: UndertowConnectionGate
    private lateinit var client: OkHttpClient
    private lateinit var request: Request

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
        client = OkHttpClient()
        request = Request.Builder().url("http://localhost:$port").build()
    }

    @AfterEach
    fun tearDown() {
        worker.shutdown()
    }

    @Test
    fun connectionCount() {
        assertEquals(0, dut.getEstablished())
        dut.open()
        client.newCall(request).response()
        assertEquals(1, dut.getEstablished())
    }

    @Test
    fun open() {
        assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
        assertThrows(ConnectException::class.java, { client.newCall(request).response() })
        dut.open()
        assertTrue(dut.getState() is ConnectionControl.State.OPEN)
        assertEquals(200, client.newCall(request).response().code())
    }

    @Test
    fun openAndClose() {
        dut.open()
        assertEquals(200, client.newCall(request).response().code())

        dut.close()
        assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
        assertEquals(200, client.newCall(request).response().code())
        assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
        client.connectionPool().evictAll()
        assertThrows(ConnectException::class.java, { client.newCall(request).response() })
    }

    @Test
    fun reopenAndClose() {
        dut.open()
        dut.close()
        dut.open()
        assertTrue(dut.getState() is ConnectionControl.State.OPEN)
        assertEquals(200, client.newCall(request).response().code())

        dut.close()
        assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
        assertEquals(200, client.newCall(request).response().code())
        client.connectionPool().evictAll()
        assertThrows(ConnectException::class.java, { client.newCall(request).response() })
    }

    @Test
    fun closeEstablishedConnections() {
        dut.open()
        client.newCall(request).response().let {
            assertEquals(200, it.code())
            assertEquals("keep-alive", it.header("Connection"))
        }

        dut.close()
        assertTrue(dut.getState() is ConnectionControl.State.CLOSED)
        client.newCall(request).response().let {
            assertEquals(200, it.code())
            assertEquals("close", it.header("Connection"))
        }
    }

    private fun Call.response() = this.execute().use { it }
}

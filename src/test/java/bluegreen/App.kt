package bluegreen

import io.undertow.Undertow
import io.undertow.UndertowOptions
import org.xnio.OptionMap
import org.xnio.nio.ReuseNioXnioProvider

class App(val name: String, port: Int) : AutoCloseable {
    private val xnioWorker = ReuseNioXnioProvider().instance.createWorker(OptionMap.EMPTY)

    private val httpServer = Undertow.builder()
            .setWorker<Undertow.Builder>(xnioWorker)
            .addHttpListener(port, "")
            .setServerOption(UndertowOptions.ENABLE_STATISTICS, true)
            .setHandler {
                it.responseSender.send(name)
            }
            .build()

    val connectionGate: ConnectionGate = UndertowConnectionGate(httpServer, port)

    override fun close() {
        httpServer.stop()
        xnioWorker.shutdown()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val blue = App("blue", 8000)
            val blueControl = ConnectionGateControlServer(blue.connectionGate, 8010)
            val green = App("green", 8000)
            val greenControl = ConnectionGateControlServer(green.connectionGate, 8020)
        }
    }
}
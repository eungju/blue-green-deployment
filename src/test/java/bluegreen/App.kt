package bluegreen

import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import org.xnio.OptionMap
import org.xnio.nio.ReuseNioXnioProvider

class App(val name: String, port: Int) : AutoCloseable {
    private val xnioWorker = ReuseNioXnioProvider().instance.createWorker(OptionMap.EMPTY)

    private val closingHandler = ConnectionCloseHandler(HttpHandler {
        it.statusCode = 200
        it.responseSender.send(name)
        it.endExchange()
    })

    private val httpServer = Undertow.builder()
            .setWorker<Undertow.Builder>(xnioWorker)
            .addHttpListener(port, "")
            .setServerOption(UndertowOptions.ENABLE_STATISTICS, true)
            .setHandler(closingHandler)
            .build()

    val connectionControl: ConnectionControl = UndertowConnectionControl(httpServer, port)
            .also { it.addListener(closingHandler) }

    override fun close() {
        connectionControl.close()
        connectionControl.removeListener(closingHandler)
        xnioWorker.shutdown()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val blue = App("blue", 8000)
            val blueControl = ConnectionControlServer(blue.connectionControl, 8010)
            val green = App("green", 8000)
            val greenControl = ConnectionControlServer(green.connectionControl, 8020)
        }
    }
}
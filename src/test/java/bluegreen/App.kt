package bluegreen

import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import org.xnio.OptionMap
import org.xnio.nio.ReuseNioXnioProvider
import java.time.Clock

class App(val name: String, port: Int) : AutoCloseable {
    val connectionControl = ConnectionControl(Clock.systemUTC())

    private val xnioWorker = ReuseNioXnioProvider().instance.createWorker(OptionMap.EMPTY)

    private val publicServer = Undertow.builder()
            .setWorker<Undertow.Builder>(xnioWorker)
            .addHttpListener(port, "")
            .setServerOption(UndertowOptions.ENABLE_STATISTICS, true)
            .setHandler(UndertowConnectionClose(connectionControl, HttpHandler {
                it.statusCode = 200
                it.responseSender.send("$name")
            }))
            .build()

    private val publicGate = UndertowConnectionGate("public", publicServer)
            .also { connectionControl.register(it) }

    override fun close() {
        connectionControl.unregister(publicGate)
        publicGate.close()
        xnioWorker.shutdown()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val blue = App("blue", 8000)
            val blueControl = AppControlServer(blue.connectionControl, 8010)
            val green = App("green", 8000)
            val greenControl = AppControlServer(green.connectionControl, 8020)
        }
    }
}

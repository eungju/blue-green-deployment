package bluegreen

import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import org.xnio.OptionMap
import org.xnio.nio.ReuseNioXnioProvider

class App(val name: String, publicPort: Int, privatePort: Int) : AutoCloseable {
    val connectionControl = ConnectionControl(name)

    private val xnioWorker = ReuseNioXnioProvider().instance.createWorker(OptionMap.EMPTY)

    private val publicServer = Undertow.builder()
            .setWorker<Undertow.Builder>(xnioWorker)
            .addHttpListener(publicPort, "")
            .setServerOption(UndertowOptions.ENABLE_STATISTICS, true)
            .setHandler(UndertowConnectionClose(connectionControl, HttpHandler {
                it.statusCode = 200
                it.responseSender.send("$name")
            }))
            .build()

    private val publicAcceptor = UndertowConnectionGate("public", publicServer)
            .also { connectionControl.register(it) }

    private val privateServer = Undertow.builder()
            .setWorker<Undertow.Builder>(xnioWorker)
            .addHttpListener(privatePort, "")
            .setServerOption(UndertowOptions.ENABLE_STATISTICS, true)
            .setHandler(UndertowConnectionClose(connectionControl, HttpHandler {
                it.statusCode = 200
                it.responseSender.send("$name-private")
            }))
            .build()

    private val privateAcceptor = UndertowConnectionGate("private", privateServer)
            .also { connectionControl.register(it) }

    override fun close() {
        connectionControl.unregister(privateAcceptor)
        privateAcceptor.close()
        connectionControl.unregister(publicAcceptor)
        publicAcceptor.close()
        xnioWorker.shutdown()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val blue = App("blue", 8000, 8001)
            val blueControl = AppControlServer(blue.connectionControl, 8010)
            val green = App("green", 8000, 8001)
            val greenControl = AppControlServer(green.connectionControl, 8020)
        }
    }
}

package bluegreen

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

class UndertowConnectionClose(private val connectionControl: ConnectionControl, private val next: HttpHandler) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        if (connectionControl.getState() is ConnectionControl.State.CLOSED) {
            exchange.isPersistent = false
        }
        next.handleRequest(exchange)
    }
}

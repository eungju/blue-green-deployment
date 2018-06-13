package bluegreen

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

class UndertowConnectionClose(private val connectionGate: ConnectionGate, private val next: HttpHandler) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        if (connectionGate.getState() == ConnectionGate.State.CLOSED) {
            exchange.isPersistent = false
        }
        next.handleRequest(exchange)
    }
}

package bluegreen

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

class UndertowConnectionClose(private val acceptor: Acceptor, private val next: HttpHandler) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        if (acceptor.getState() == Acceptor.State.CLOSED) {
            exchange.isPersistent = false
        }
        next.handleRequest(exchange)
    }
}

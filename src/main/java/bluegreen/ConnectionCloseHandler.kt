package bluegreen

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

class ConnectionCloseHandler(private val next: HttpHandler) : HttpHandler, ConnectionControl.Listener {
    private lateinit var state: ConnectionControl.State

    override fun onChange(state: ConnectionControl.State) {
        this.state = state
    }

    override fun handleRequest(exchange: HttpServerExchange) {
        if (state == ConnectionControl.State.CLOSED) {
            exchange.isPersistent = false
        }
        next.handleRequest(exchange)
    }
}

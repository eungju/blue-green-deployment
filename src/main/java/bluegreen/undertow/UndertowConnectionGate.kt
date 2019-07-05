package bluegreen.undertow

import bluegreen.ConnectionGate
import io.undertow.Undertow
import java.util.concurrent.atomic.AtomicReference

class UndertowConnectionGate(override val name: String, private val undertow: Undertow) : ConnectionGate {
    private val state = AtomicReference(ConnectionGate.State.CLOSED)

    override fun open() {
        if (state.compareAndSet(ConnectionGate.State.CLOSED, ConnectionGate.State.OPEN)) {
            undertow.start()
        }
    }

    override fun close() {
        if (state.compareAndSet(ConnectionGate.State.OPEN, ConnectionGate.State.CLOSED)) {
            undertow.stop()
        }
    }

    override fun getState() = state.get()
}

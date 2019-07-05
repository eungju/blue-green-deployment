package bluegreen

import java.util.concurrent.atomic.AtomicReference

abstract class AbstractConnectionGate(override val name: String) : ConnectionGate {
    private val state = AtomicReference(ConnectionGate.State.CLOSED)

    override fun open() {
        if (state.compareAndSet(ConnectionGate.State.CLOSED, ConnectionGate.State.OPEN)) {
            doOpen()
        }
    }

    override fun close() {
        if (state.compareAndSet(ConnectionGate.State.OPEN, ConnectionGate.State.CLOSED)) {
            doClose()
        }
    }

    override fun getState() = state.get()

    protected abstract fun doOpen()

    protected abstract fun doClose()
}

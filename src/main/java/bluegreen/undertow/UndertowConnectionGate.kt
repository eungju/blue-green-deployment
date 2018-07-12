package bluegreen.undertow

import bluegreen.ConnectionGate
import io.undertow.Undertow
import io.undertow.server.AggregateConnectorStatistics
import io.undertow.server.ConnectorStatistics
import java.util.concurrent.atomic.AtomicReference

class UndertowConnectionGate(override val name: String, private val undertow: Undertow) : ConnectionGate {
    private val state = AtomicReference(ConnectionGate.State.CLOSED)
    private var listenerInfo = emptyList<Undertow.ListenerInfo>()
    private var connectorStatistics: ConnectorStatistics = AggregateConnectorStatistics(emptyArray())

    override fun open() {
        if (state.compareAndSet(ConnectionGate.State.CLOSED, ConnectionGate.State.OPEN)) {
            undertow.start()
            listenerInfo = listenerInfo.filter { it.connectorStatistics.activeConnections > 0 } +
                    undertow.listenerInfo
            connectorStatistics = AggregateConnectorStatistics(listenerInfo.map { it.connectorStatistics }.toTypedArray())
        }
    }

    override fun close() {
        if (state.compareAndSet(ConnectionGate.State.OPEN, ConnectionGate.State.CLOSED)) {
            undertow.stop()
        }
    }

    override fun getState() = state.get()

    override fun getEstablished(): Int = connectorStatistics.activeConnections.toInt()
}

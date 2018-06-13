package bluegreen

import io.undertow.Undertow
import io.undertow.server.AggregateConnectorStatistics
import io.undertow.server.ConnectorStatistics
import java.util.concurrent.atomic.AtomicReference

class UndertowConnectionGate(override val name: String, private val undertow: Undertow) : ConnectionGate {
    private val _state = AtomicReference(ConnectionGate.State.CLOSED)
    private var listenerInfo = emptyList<Undertow.ListenerInfo>()
    private var connectorStatistics: ConnectorStatistics = AggregateConnectorStatistics(emptyArray())

    override fun open() {
        if (_state.compareAndSet(ConnectionGate.State.CLOSED, ConnectionGate.State.OPEN)) {
            undertow.start()
            listenerInfo = listenerInfo.filter { it.connectorStatistics.activeConnections > 0 } +
                    undertow.listenerInfo
            connectorStatistics = AggregateConnectorStatistics(listenerInfo.map { it.connectorStatistics }.toTypedArray())
        }
    }

    override fun close() {
        if (_state.compareAndSet(ConnectionGate.State.OPEN, ConnectionGate.State.CLOSED)) {
            undertow.stop()
        }
    }

    override fun getState() = _state.get()

    override fun getEstablished(): Int = connectorStatistics.activeConnections.toInt()
}

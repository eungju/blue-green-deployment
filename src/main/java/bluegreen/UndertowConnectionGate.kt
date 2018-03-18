package bluegreen

import io.undertow.Undertow
import io.undertow.server.AggregateConnectorStatistics
import io.undertow.server.ConnectorStatistics
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

class UndertowConnectionGate(private val undertow: Undertow, override val port: Int) : ConnectionGate {
    override val state: ConnectionGate.State get() = _state.get()
    private val _state = AtomicReference(ConnectionGate.State.CLOSED)
    private var listenerInfo = emptyList<Undertow.ListenerInfo>()
    private var connectorStatistics: ConnectorStatistics = AggregateConnectorStatistics(emptyArray())

    override fun open() {
        if (_state.compareAndSet(ConnectionGate.State.CLOSED, ConnectionGate.State.OPEN)) {
            undertow.start()
            listenerInfo = listenerInfo.filter { it.connectorStatistics.activeConnections > 0 } +
                    undertow.listenerInfo.filter { (it.address as? InetSocketAddress)?.port == port }.first()
            connectorStatistics = AggregateConnectorStatistics(listenerInfo.map { it.connectorStatistics }.toTypedArray())
        }
    }

    override fun halfClose() {
        if (_state.compareAndSet(ConnectionGate.State.OPEN, ConnectionGate.State.HALF_CLOSED)) {
            undertow.stop()
        }
    }

    override fun close() {
        if (_state.compareAndSet(ConnectionGate.State.HALF_CLOSED, ConnectionGate.State.CLOSING)) {
            //TODO: Start to send response with "Connection: close"
            if (connectorStatistics.activeConnections == 0L) {
                if (_state.compareAndSet(ConnectionGate.State.CLOSING, ConnectionGate.State.CLOSED)) {
                }
            }
        }
    }

    override fun getEstablished(): Long = connectorStatistics.activeConnections
}

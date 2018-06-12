package bluegreen

import io.undertow.Undertow
import io.undertow.server.AggregateConnectorStatistics
import io.undertow.server.ConnectorStatistics
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

class UndertowConnectionControl(private val undertow: Undertow, override val port: Int) : ConnectionControl {
    override val state: ConnectionControl.State get() = _state.get()
    private val _state = AtomicReference(ConnectionControl.State.CLOSED)
    private var listenerInfo = emptyList<Undertow.ListenerInfo>()
    private var connectorStatistics: ConnectorStatistics = AggregateConnectorStatistics(emptyArray())

    override fun open() {
        if (_state.compareAndSet(ConnectionControl.State.CLOSED, ConnectionControl.State.OPEN)) {
            undertow.start()
            listenerInfo = listenerInfo.filter { it.connectorStatistics.activeConnections > 0 } +
                    undertow.listenerInfo.filter { (it.address as? InetSocketAddress)?.port == port }.first()
            connectorStatistics = AggregateConnectorStatistics(listenerInfo.map { it.connectorStatistics }.toTypedArray())
        }
    }

    override fun close() {
        if (_state.compareAndSet(ConnectionControl.State.OPEN, ConnectionControl.State.CLOSING)) {
            undertow.stop()
            //TODO: Start to send response with "Connection: close"
            if (connectorStatistics.activeConnections == 0L) {
                if (_state.compareAndSet(ConnectionControl.State.CLOSING, ConnectionControl.State.CLOSED)) {
                }
            }
        }
    }

    override fun getEstablished(): Long = connectorStatistics.activeConnections
}


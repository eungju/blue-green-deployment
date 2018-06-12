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
    private val listeners = mutableListOf<ConnectionControl.Listener>()

    override fun addListener(listener: ConnectionControl.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ConnectionControl.Listener) {
        listeners.remove(listener)
    }

    override fun open() {
        if (_state.compareAndSet(ConnectionControl.State.CLOSED, ConnectionControl.State.OPEN)) {
            undertow.start()
            listenerInfo = listenerInfo.filter { it.connectorStatistics.activeConnections > 0 } +
                    undertow.listenerInfo.filter { (it.address as? InetSocketAddress)?.port == port }.first()
            connectorStatistics = AggregateConnectorStatistics(listenerInfo.map { it.connectorStatistics }.toTypedArray())
            listeners.forEach { it.onChange(ConnectionControl.State.OPEN) }
        }
    }

    override fun close() {
        if (_state.compareAndSet(ConnectionControl.State.OPEN, ConnectionControl.State.CLOSED)) {
            undertow.stop()
            listeners.forEach { it.onChange(ConnectionControl.State.CLOSED) }
        }
    }

    override fun getEstablished(): Long = connectorStatistics.activeConnections
}


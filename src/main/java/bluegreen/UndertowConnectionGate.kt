package bluegreen

import io.undertow.Undertow
import io.undertow.server.AggregateConnectorStatistics
import io.undertow.server.ConnectorStatistics
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class UndertowConnectionGate(private val undertow: Undertow, override val port: Int) : ConnectionGate {
    private val accepting = AtomicBoolean(false)
    private var listenerInfo = emptyList<Undertow.ListenerInfo>()
    private var connectorStatistics: ConnectorStatistics = AggregateConnectorStatistics(emptyArray())

    override fun open() {
        if (accepting.compareAndSet(false, true)) {
            undertow.start()
            listenerInfo = listenerInfo.filter { it.connectorStatistics.activeConnections > 0 } +
                    undertow.listenerInfo.filter { (it.address as? InetSocketAddress)?.port == port }.first()
            connectorStatistics = AggregateConnectorStatistics(listenerInfo.map { it.connectorStatistics }.toTypedArray())
        }
    }

    override fun halfClose() {
        if (accepting.compareAndSet(true, false)) {
            undertow.stop()
        }
    }

    override fun isAccepting(): Boolean = accepting.get()

    override fun getEstablished(): Long = connectorStatistics.activeConnections
}

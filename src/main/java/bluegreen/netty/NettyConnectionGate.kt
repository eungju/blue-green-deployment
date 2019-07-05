package bluegreen.netty

import bluegreen.ConnectionGate
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import java.util.concurrent.atomic.AtomicReference

class NettyConnectionGate(override val name: String, private val bootstrap: ServerBootstrap) : ConnectionGate {
    private val state = AtomicReference(ConnectionGate.State.CLOSED)
    private var channel: Channel? = null

    override fun open() {
        if (state.compareAndSet(ConnectionGate.State.CLOSED, ConnectionGate.State.OPEN)) {
            channel = bootstrap.bind().sync().channel()
        }
    }

    override fun close() {
        if (state.compareAndSet(ConnectionGate.State.OPEN, ConnectionGate.State.CLOSED)) {
            channel?.close()?.sync()
            channel?.deregister()?.sync()
        }
    }

    override fun getState() = state.get()
}

package bluegreen.netty

import bluegreen.AbstractConnectionGate
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel

class NettyConnectionGate(name: String, private val bootstrap: ServerBootstrap) : AbstractConnectionGate(name) {
    private var channel: Channel? = null

    override fun doOpen() {
        channel = bootstrap.bind().sync().channel()
    }

    override fun doClose() {
        channel?.run {
            close().sync()
            deregister().sync()
        }
    }
}

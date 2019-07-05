package bluegreen.netty

import bluegreen.reuseport.ReusePort
import io.netty.channel.socket.nio.NioServerSocketChannel

class ReuseNioServerSocketChannel : NioServerSocketChannel() {
    init {
        ReusePort.DEFAULT.set(javaChannel(), true)
    }
}
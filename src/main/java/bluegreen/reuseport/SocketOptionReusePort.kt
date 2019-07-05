package bluegreen.reuseport

import java.net.SocketOption
import java.net.StandardSocketOptions
import java.nio.channels.ServerSocketChannel

class SocketOptionReusePort : ReusePort {
    private val option: SocketOption<Boolean>

    init {
        try {
            val field = StandardSocketOptions::class.java.getField("SO_REUSEPORT")
            option = field.get(StandardSocketOptions::class.java) as SocketOption<Boolean>
        } catch (e: NoSuchFieldException) {
            throw UnsupportedOperationException("StandardSocketOptions.SO_REUSEPORT is not available")
        }
    }

    override fun set(channel: ServerSocketChannel, value: Boolean) {
        channel.setOption(option, value)
    }
}

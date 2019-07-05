package bluegreen.reuseport

import java.nio.channels.ServerSocketChannel

interface ReusePort {
    fun set(channel: ServerSocketChannel, value: Boolean)

    companion object {
        @JvmField
        val DEFAULT = try {
            SocketOptionReusePort()
        } catch (e: UnsupportedOperationException) {
            SunReusePort()
        }
    }
}
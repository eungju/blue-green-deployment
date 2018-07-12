package bluegreen

import java.nio.channels.ServerSocketChannel

abstract class ReusePort {
    abstract fun set(channel: ServerSocketChannel, value: Boolean)

    companion object {
        @JvmField
        val DEFAULT = try {
            JdkReusePort()
        } catch (e: UnsupportedOperationException) {
            SunReusePort()
        }
    }
}
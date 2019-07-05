package bluegreen.reuseport

import java.io.FileDescriptor
import java.nio.channels.ServerSocketChannel

class SunReusePort : ReusePort {
    private val SO_REUSEPORT: Int
    private val SOL_SOCKET: Int

    init {
        val osName = System.getProperty("os.name").toLowerCase()
        when {
            isMac(osName) -> {
                SOL_SOCKET = 0xffff
                SO_REUSEPORT = 0x0200
            }
            isLinux(osName) -> {
                SOL_SOCKET = 1
                SO_REUSEPORT = 15
            }
            else -> {
                SOL_SOCKET = 0xffff
                SO_REUSEPORT = 0
            }
        }
    }

    private fun isMac(osName: String) = osName.startsWith("mac")

    private fun isLinux(osName: String) = osName.startsWith("linux")

    override fun set(channel: ServerSocketChannel, value: Boolean) {
        val fieldFd = channel.javaClass.getDeclaredField("fd")
        fieldFd.isAccessible = true
        val fd = fieldFd.get(channel) as FileDescriptor
        val netClass = Class.forName("sun.nio.ch.Net")
        val intValue = if (value) 1 else 0
        try {
            val methodSetIntOption0 = netClass.getDeclaredMethod(
                "setIntOption0", FileDescriptor::class.java, java.lang.Boolean.TYPE, Integer.TYPE, Integer.TYPE,
                Integer.TYPE, java.lang.Boolean.TYPE)
            methodSetIntOption0.isAccessible = true
            methodSetIntOption0.invoke(null, fd, false, SOL_SOCKET, SO_REUSEPORT, intValue, true)
        } catch (e: NoSuchMethodException) {
            val methodSetIntOption0 = netClass.getDeclaredMethod(
                "setIntOption0", FileDescriptor::class.java, java.lang.Boolean.TYPE, Integer.TYPE, Integer.TYPE,
                Integer.TYPE)
            methodSetIntOption0.isAccessible = true
            methodSetIntOption0.invoke(null, fd, false, SOL_SOCKET, SO_REUSEPORT, intValue)
        }
    }
}
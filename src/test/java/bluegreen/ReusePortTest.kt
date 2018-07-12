package bluegreen

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.channels.ServerSocketChannel

class ReusePortTest {
    @Test
    fun jdk() {
        assumeTrue(System.getProperty("java.version").split(".")[0].toInt() >= 9)
        val dut = JdkReusePort()
        ServerSocketChannel.open().use { socket ->
            dut.set(socket, true)
        }
    }

    @Test
    fun sun() {
        val dut = SunReusePort()
        ServerSocketChannel.open().use { socket ->
            dut.set(socket, true)
        }
    }
}

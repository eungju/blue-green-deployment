package org.xnio.nio

import bluegreen.reuseport.ReusePort
import org.xnio.*
import org.xnio.channels.AcceptingChannel
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.TimeUnit

class ReusePortNioXnioWorker
private constructor(private val reusePort: Boolean, xnio: NioXnio, optionMap: OptionMap)
    : XnioWorker(xnio, null, optionMap, null) {

    private val delegation = NioXnioWorker(xnio, null, optionMap, null)

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return delegation.awaitTermination(timeout, unit)
    }

    override fun awaitTermination() {
        return delegation.awaitTermination()
    }

    override fun getIoThreadCount(): Int {
        return delegation.ioThreadCount
    }

    override fun isTerminated(): Boolean {
        return delegation.isTerminated
    }

    override fun shutdownNow(): MutableList<Runnable> {
        return delegation.shutdownNow()
    }

    override fun isShutdown(): Boolean {
        return delegation.isShutdown
    }

    override fun getIoThread(hashCode: Int): XnioIoThread {
        return delegation.getIoThread(hashCode)
    }

    override fun shutdown() {
        delegation.shutdown()
    }

    override fun chooseThread(): XnioIoThread {
        return delegation.chooseThread()
    }

    override fun taskPoolTerminated() {
        delegation.taskPoolTerminated()
    }

    override fun createTcpConnectionServer(bindAddress: InetSocketAddress, acceptListener: ChannelListener<in AcceptingChannel<StreamConnection>>, optionMap: OptionMap): AcceptingChannel<StreamConnection> {
        delegation.checkShutdown()
        var ok = false
        val channel = ServerSocketChannel.open()
        try {
            if (optionMap.contains(Options.RECEIVE_BUFFER)) channel.socket().receiveBufferSize = optionMap.get(Options.RECEIVE_BUFFER, -1)
            channel.socket().reuseAddress = optionMap.get(Options.REUSE_ADDRESSES, true)
            if (reusePort) ReusePort.DEFAULT.set(channel, true)
            channel.configureBlocking(false)
            if (optionMap.contains(Options.BACKLOG)) {
                channel.socket().bind(bindAddress, optionMap.get(Options.BACKLOG, 128))
            } else {
                channel.socket().bind(bindAddress)
            }
            return if (false) {
                NioTcpServer(delegation, channel, optionMap).apply {
                    this@apply.acceptListener = acceptListener
                    ok = true
                }
            } else {
                QueuedNioTcpServer(delegation, channel, optionMap).apply {
                    this@apply.acceptListener = acceptListener
                    ok = true
                }
            }
        } finally {
            if (!ok) {
                IoUtils.safeClose(channel)
            }
        }
    }

    fun start() {
        delegation.start()
    }

    companion object {
        @JvmStatic
        fun createWorker(reusePort: Boolean, optionMap: OptionMap): XnioWorker {
            return ReusePortNioXnioWorker(reusePort, NioXnio(), optionMap).apply { start() }
        }
    }
}

package org.xnio.nio;

import bluegreen.ReusePort;
import org.xnio.*;
import org.xnio.channels.AcceptingChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class ReuseNioXnioWorker extends NioXnioWorker {
    ReuseNioXnioWorker(NioXnio xnio, ThreadGroup threadGroup, OptionMap optionMap, Runnable terminationTask) throws IOException {
        super(xnio, threadGroup, optionMap, terminationTask);
    }

    @Override
    protected AcceptingChannel<StreamConnection> createTcpConnectionServer(final InetSocketAddress bindAddress, final ChannelListener<? super AcceptingChannel<StreamConnection>> acceptListener, final OptionMap optionMap) throws IOException {
        checkShutdown();
        boolean ok = false;
        final ServerSocketChannel channel = ServerSocketChannel.open();
        try {
            if (optionMap.contains(Options.RECEIVE_BUFFER)) channel.socket().setReceiveBufferSize(optionMap.get(Options.RECEIVE_BUFFER, -1));
            ReusePort.DEFAULT.set(channel, true);
            channel.socket().setReuseAddress(optionMap.get(Options.REUSE_ADDRESSES, true));
            channel.configureBlocking(false);
            if (optionMap.contains(Options.BACKLOG)) {
                channel.socket().bind(bindAddress, optionMap.get(Options.BACKLOG, 128));
            } else {
                channel.socket().bind(bindAddress);
            }
            if (false) {
                final NioTcpServer server = new NioTcpServer(this, channel, optionMap);
                server.setAcceptListener(acceptListener);
                ok = true;
                return server;
            } else {
                final QueuedNioTcpServer server = new QueuedNioTcpServer(this, channel, optionMap);
                server.setAcceptListener(acceptListener);
                ok = true;
                return server;
            }
        } finally {
            if (! ok) {
                IoUtils.safeClose(channel);
            }
        }
    }
}

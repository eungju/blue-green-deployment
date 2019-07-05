package bluegreen.netty

import bluegreen.ConnectionGate
import bluegreen.ConnectionGateTest
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.CharsetUtil

class NettyConnectionGateTest : ConnectionGateTest() {
    override fun server(f: (ConnectionGate) -> Unit) {
        var gate: ConnectionGate? = null
        val eventLoopGroup = NioEventLoopGroup()
        try {
            val bootstrap = ServerBootstrap()
                    .group(eventLoopGroup)
                    .handler(LoggingHandler(LogLevel.INFO))
                    .childHandler(object : ChannelInitializer<Channel>() {
                        override fun initChannel(ch: Channel) {
                            val pipeline = ch.pipeline()
                            pipeline.addLast(HttpServerCodec())
                            pipeline.addLast(object : SimpleChannelInboundHandler<HttpObject>() {
                                override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
                                    if (msg is LastHttpContent) {
                                        val content: ByteBuf = Unpooled.copiedBuffer("Hello World.", CharsetUtil.UTF_8)
                                        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content)
                                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                                        if (gate?.getState() == ConnectionGate.State.CLOSED) {
                                            response.headers().set(HttpHeaderNames.CONNECTION, "close")
                                            ctx.write(response)
                                                    .addListener(ChannelFutureListener.CLOSE)
                                        } else {
                                            ctx.write(response)
                                        }
                                    }
                                }

                                override fun channelReadComplete(ctx: ChannelHandlerContext) {
                                    ctx.flush()
                                }
                            })
                        }
                    })
                    .channel(ReuseNioServerSocketChannel::class.java)
                    .localAddress(port)
            gate = NettyConnectionGate(port.toString(), bootstrap)
            f(gate)
        } finally {
            eventLoopGroup.shutdownGracefully()
        }
    }
}

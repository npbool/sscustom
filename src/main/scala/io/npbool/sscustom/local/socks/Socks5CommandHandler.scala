package io.npbool.sscustom.local.socks

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.handler.codec.socksx.v5._
import io.npbool.sscustom.Config
import io.npbool.sscustom.util.{FixedPipelineChannelInitializer, HandlerUtils}
import org.apache.logging.log4j.scala.Logging

@Sharable
class Socks5CommandHandler extends SimpleChannelInboundHandler[Socks5CommandRequest] with Logging {
  import io.npbool.sscustom.util.JavaConverters._

  override def channelRead0(ctx: ChannelHandlerContext, msg: Socks5CommandRequest): Unit = {
    logger.info(s"Connect: ${msg.dstAddr()}:${msg.dstPort()}")

    msg.`type`() match {
      case Socks5CommandType.BIND =>
        fail(ctx, Socks5CommandStatus.COMMAND_UNSUPPORTED)
      case Socks5CommandType.UDP_ASSOCIATE =>
        fail(ctx, Socks5CommandStatus.COMMAND_UNSUPPORTED)
      case Socks5CommandType.CONNECT =>
        val bootstrap = new Bootstrap()
        bootstrap.group(ctx.channel().eventLoop())
          .channel(Config.socketChannelClass)
          .option(ChannelOption.SO_KEEPALIVE, true.asJava)
          .option(ChannelOption.AUTO_READ, false.asJava)
          .handler(new FixedPipelineChannelInitializer(Seq(
            new TcpRelayTargetHandler(ctx.channel())
          )))
          .connect(msg.dstAddr(), msg.dstPort())
          .addListener { f: ChannelFuture =>
            if (f.isSuccess) {
              val pipeline = ctx.pipeline()
              pipeline.addFirst(new TcpRelaySourceHandler(f.channel()))
              val resp = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.DOMAIN)
              ctx.writeAndFlush(resp)
            } else if (f.cause() != null) {
              logger.error(s"Error in connecting target; ${f.cause().getLocalizedMessage}")
              fail(ctx, Socks5CommandStatus.FAILURE)
            } else {
              fail(ctx, Socks5CommandStatus.FAILURE)
            }
          }
    }
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.read()
  }

  private def fail(ctx: ChannelHandlerContext, status: Socks5CommandStatus): Unit = {
    val resp = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.DOMAIN)
    ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE)
  }

}

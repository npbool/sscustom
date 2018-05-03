package io.npbool.sscustom.local.socks

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.handler.codec.socksx.v5._
import io.netty.util.concurrent.Future
import org.apache.logging.log4j.scala.Logging

@Sharable
class Socks5CommandHandler extends SimpleChannelInboundHandler[Socks5CommandRequest] with Logging {

  override def channelRead0(ctx: ChannelHandlerContext, msg: Socks5CommandRequest): Unit = {
    logger.debug(s"Connect: ${msg.dstAddr()}:${msg.dstPort()}")

    msg.`type`() match {
      case Socks5CommandType.BIND =>
        fail(ctx, Socks5CommandStatus.COMMAND_UNSUPPORTED)
      case Socks5CommandType.UDP_ASSOCIATE =>
        fail(ctx, Socks5CommandStatus.COMMAND_UNSUPPORTED)
      case Socks5CommandType.CONNECT =>
        TcpRelay.create(msg.dstAddr, msg.dstPort, ctx)
          .addListener { f: Future[ChannelInboundHandler] =>
            if (f.isSuccess) {
              ctx.pipeline().addFirst(f.get())
              ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.DOMAIN))
            } else {
              logger.error(s"Error in connecting target; ${f.cause().getLocalizedMessage}")
              fail(ctx, Socks5CommandStatus.FAILURE)
            }
          }
    }
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.read()
  }

  private def fail(ctx: ChannelHandlerContext, status: Socks5CommandStatus): Unit = {
    val resp = new DefaultSocks5CommandResponse(status, Socks5AddressType.DOMAIN)
    ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE)
  }

}

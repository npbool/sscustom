package io.npbool.sscustom.local.socks

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.util.concurrent.Future
import io.npbool.sscustom.Config
import io.npbool.sscustom.util.HandlerUtils
import org.apache.logging.log4j.scala.Logging

class TcpRelay(downstream: Channel) extends ChannelInboundHandlerAdapter with Logging {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.read()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    if (downstream.isActive) {
      downstream.writeAndFlush(msg).addListener { f: ChannelFuture =>
        if (f.isSuccess) {
          ctx.channel.read()
        } else {
          f.channel().close
        }
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error("Exception in tcp relay", cause)
    HandlerUtils.closeOnFlush(ctx.channel())
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    HandlerUtils.closeOnFlush(downstream)
    logger.debug(s"Close ${ctx.channel().remoteAddress()}")
  }
}

object TcpRelay {
  import io.npbool.sscustom.util.JavaConverters._

  def create(dstAddr: String, dstPort: Int, ctx: ChannelHandlerContext): Future[ChannelInboundHandler] =
    create(dstAddr, dstPort, ctx, ctx.channel().eventLoop())

  def create(dstAddr: String, dstPort: Int, ctx: ChannelHandlerContext, eventLoop: EventLoop): Future[ChannelInboundHandler] = {
    val upstreamHandlerPromise = eventLoop.newPromise[ChannelInboundHandler]()
    val bootstrap = new Bootstrap()
    bootstrap.group(eventLoop)
      .channel(Config.socketChannelClass)
      .option(ChannelOption.AUTO_READ, false.asJava)
      .option(ChannelOption.SO_KEEPALIVE, true.asJava)
      .handler(new TcpRelay(ctx.channel()))
      .connect(dstAddr, dstPort)
      .addListener { f: ChannelFuture =>
        if (f.isSuccess) {
          upstreamHandlerPromise.setSuccess(new TcpRelay(f.channel()))
        } else {
          upstreamHandlerPromise.setFailure(f.cause())
        }
      }
    upstreamHandlerPromise
  }

}


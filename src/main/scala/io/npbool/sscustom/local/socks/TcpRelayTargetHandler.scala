package io.npbool.sscustom.local.socks

import io.netty.channel.{Channel, ChannelFuture, ChannelHandlerContext}
import io.npbool.sscustom.util.HandlerUtils
import org.apache.logging.log4j.scala.Logging

class TcpRelayTargetHandler(sourceChannel: Channel) extends TcpRelayHandler(sourceChannel) with Logging {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.read()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    sourceChannel.writeAndFlush(msg).addListener { f: ChannelFuture =>
      if (f.isSuccess) {
        ctx.channel().read()
      } else {
        f.channel().close()
      }
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    HandlerUtils.closeOnFlush(sourceChannel)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error("Exception in tcp relay", cause)

    HandlerUtils.closeOnFlush(ctx.channel())
  }
}

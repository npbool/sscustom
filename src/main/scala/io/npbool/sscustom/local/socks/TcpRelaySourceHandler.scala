package io.npbool.sscustom.local.socks

import io.netty.channel.{Channel, ChannelFuture, ChannelHandlerContext}
import io.npbool.sscustom.util.HandlerUtils
import org.apache.logging.log4j.scala.Logging

class TcpRelaySourceHandler(targetChannel: Channel) extends TcpRelayHandler(targetChannel) with Logging {
  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    if (targetChannel.isActive) {
      targetChannel.writeAndFlush(msg).addListener { f: ChannelFuture =>
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
    HandlerUtils.closeOnFlush(targetChannel)
    logger.debug(s"Close ${ctx.channel().remoteAddress()}")
  }
}

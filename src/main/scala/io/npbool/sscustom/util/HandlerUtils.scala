package io.npbool.sscustom.util

import io.netty.buffer.Unpooled
import io.netty.channel.{Channel, ChannelFutureListener, ChannelHandlerContext}

object HandlerUtils {
  def writeAndClose(ctx: ChannelHandlerContext, data: Any): Unit = {
    ctx.writeAndFlush(data).addListener(ChannelFutureListener.CLOSE)
  }

  def closeOnFlush(ch: Channel): Unit = {
    if (ch.isActive) ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
  }
}

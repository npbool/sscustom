package io.npbool.sscustom.local

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil

class EchoHandler extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    val buf = msg.asInstanceOf[ByteBuf]
    try {
      val out = ctx.alloc().buffer(buf.readableBytes())
      while (buf.isReadable) {
        val b = buf.readByte()
        out.writeByte(b)
        print(b.asInstanceOf[Char])
      }
      Console.flush()

      ctx.writeAndFlush(out)
    } finally {
      ReferenceCountUtil.release(msg)
    }
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val addr = ctx.channel().remoteAddress()
    println(s"$addr connected")
    ctx.read()
    super.channelActive(ctx)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.read()
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    val addr = ctx.channel().remoteAddress()
    println(s"$addr disconnected")
    super.channelInactive(ctx)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}

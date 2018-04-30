package io.npbool.sscustom.local.socks

import io.netty.channel.ChannelHandler
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.socksx.v5.{DefaultSocks5InitialResponse, Socks5AuthMethod, Socks5CommandRequestDecoder, Socks5InitialRequest}
import org.apache.logging.log4j.scala.Logging

@ChannelHandler.Sharable
class Socks5InitialHandler extends SimpleChannelInboundHandler[Socks5InitialRequest] with Logging {
  override def channelRead0(ctx: ChannelHandlerContext, msg: Socks5InitialRequest): Unit = {

    if (msg.authMethods().contains(Socks5AuthMethod.NO_AUTH)) {
      val pipeline = ctx.pipeline()
      pipeline.removeFirst()
      pipeline.remove(this)
      pipeline.addFirst(new Socks5CommandRequestDecoder())
      pipeline.addLast(new Socks5CommandHandler())
      ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH))
    } else {
      ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED))
    }
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.debug(s"New connection from ${ctx.channel().remoteAddress()}")
    ctx.read()
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.read()
  }
}

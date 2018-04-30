package io.npbool.sscustom.local

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.socksx.v5.{Socks5AddressEncoder, Socks5InitialRequestDecoder, Socks5ServerEncoder}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.npbool.sscustom.Config
import io.npbool.sscustom.local.socks.Socks5InitialHandler
import io.npbool.sscustom.util.FixedPipelineChannelInitializer
import org.apache.logging.log4j.scala.Logging

class LocalProxy(port: Int) extends Logging {
  import io.npbool.sscustom.util.JavaConverters._
  def run(): Unit = {
    val bossGroup = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup()
    val bs = new ServerBootstrap()
    val socks5InitialHandler = new Socks5InitialHandler()
    val serverEncoder = new Socks5ServerEncoder(Socks5AddressEncoder.DEFAULT)
    try {
      bs.group(bossGroup, workerGroup)
        .channel(Config.serverSocketChannelClass)
        .childHandler(new FixedPipelineChannelInitializer(Seq(
          new Socks5InitialRequestDecoder(),
          serverEncoder,
          socks5InitialHandler
        )))
        .option(ChannelOption.SO_BACKLOG, 256.asJava)
        .childOption(ChannelOption.SO_KEEPALIVE, true.asJava)
        .childOption(ChannelOption.AUTO_READ, false.asJava)

      val channelFuture = bs.bind("localhost", port).sync()
      if (channelFuture.isSuccess) {
        logger.info(s"Listening at ${channelFuture.channel().localAddress()}")
        channelFuture.channel().closeFuture().sync()
      }
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}

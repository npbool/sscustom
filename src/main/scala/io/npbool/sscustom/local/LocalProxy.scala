package io.npbool.sscustom.local

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

class LocalProxy(port: Int) {
  import io.npbool.sscustom.util.JavaConverters._
  def run(): Unit = {
    val bossGroup = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup()
    val bs = new ServerBootstrap()
    try {
      bs.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer[NioSocketChannel] {
          override def initChannel(ch: NioSocketChannel): Unit = {
            ch.pipeline()
              .addLast(new EchoHandler)
          }
        })
        .option(ChannelOption.SO_BACKLOG, 128.toJava)
        .childOption(ChannelOption.SO_KEEPALIVE, true.toJava)

      bs.bind("localhost", port).sync()
        .channel().closeFuture().sync()

    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}

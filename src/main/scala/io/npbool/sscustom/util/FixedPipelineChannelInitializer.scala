package io.npbool.sscustom.util

import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelHandler, ChannelInitializer}

class FixedPipelineChannelInitializer(f: => Seq[ChannelHandler]) extends ChannelInitializer[NioSocketChannel] {
  override def initChannel(ch: NioSocketChannel): Unit = {
    val pipeline = ch.pipeline()
    f.foreach(ch => pipeline.addLast(ch))
  }
}

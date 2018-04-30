package io.npbool.sscustom


import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}

object Config {
  val socketChannelClass: Class[_ <: SocketChannel] = classOf[NioSocketChannel]
  val serverSocketChannelClass: Class[_ <: ServerSocketChannel] = classOf[NioServerSocketChannel]
}

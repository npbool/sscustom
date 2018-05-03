package io.npbool.sscustom.local

import java.net.SocketAddress

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.socksx.v5.Socks5AddressType
import io.netty.handler.codec.{DatagramPacketDecoder, DatagramPacketEncoder}
import io.npbool.sscustom.local.socks.Socks5UdpProtocol._
import org.junit.Assert._
import org.junit.Test


class RecvHandler(truth: UdpRelayPacket) extends SimpleChannelInboundHandler[UdpRelayPacket] {

  override def channelRead0(ctx: ChannelHandlerContext, msg: UdpRelayPacket): Unit = {
    try {
      assertEquals(truth.frag, msg.frag)
      assertEquals(truth.dstAddr, msg.dstAddr)

      val data = new Array[Byte](msg.data.readableBytes())
      msg.data.readBytes(data, 0, data.length)
      assertArrayEquals(truth.data.array(), data)
    } finally {
      ctx.close()
    }
  }
}

class TestUdpCodec {

  def createServer(group: EventLoopGroup, truth: UdpRelayPacket): ChannelFuture = {

    val bs = new Bootstrap()
    bs
      .group(group)
      .channel(classOf[NioDatagramChannel])
      .handler(new ChannelInitializer[DatagramChannel] {
        override def initChannel(ch: DatagramChannel): Unit = {
          ch.pipeline()
            .addLast(new DatagramPacketDecoder(new UdpRelayPacketDecoder()))
            .addLast(new RecvHandler(truth))
        }
      })
      .bind("localhost", 6666)
  }

  def createClient(group: EventLoopGroup, addr: SocketAddress): ChannelFuture = {
    val bs = new Bootstrap()
    bs
      .group(group)
      .channel(classOf[NioDatagramChannel])
      .handler(new DatagramPacketEncoder(new UdpRelayPacketEncoder()))
      .bind(0)
  }

  @Test
  def test(): Unit = {
    val data = Unpooled.buffer(7)
    data.writeByte(1)
    data.writeShort(12223)
    data.writeInt(1 << 30)
    val truth = UdpRelayPacket(0, Socks5AddressType.IPv4, "1.2.3.254", 12345, data)

    val group = new NioEventLoopGroup(3)

    val serverChannel = createServer(group, truth)
        .sync().channel()
    val serverAddr = serverChannel.localAddress()

    val clientChannel = createClient(group, serverChannel.localAddress())
      .sync().channel()

    truth.retain()
    val withAddr = new DefaultAddressedEnvelope(truth, serverAddr, clientChannel.localAddress())
    val ctx = clientChannel.pipeline()
    ctx.writeAndFlush(withAddr).sync()

    clientChannel.close().sync()
    serverChannel.closeFuture().sync()
  }
}


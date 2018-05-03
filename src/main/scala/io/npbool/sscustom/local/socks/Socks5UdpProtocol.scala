package io.npbool.sscustom.local.socks

import java.util

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel._
import io.netty.handler.codec.socksx.v5.{Socks5AddressDecoder, Socks5AddressEncoder, Socks5AddressType}
import io.netty.handler.codec.{MessageToMessageDecoder, MessageToMessageEncoder}
import io.netty.util.ReferenceCounted

object Socks5UdpProtocol {

  case class UdpRelayPacket(frag: Int,
                            dstAddrType: Socks5AddressType, dstAddr: String, dstPort: Int,
                            data: ByteBuf
                           ) extends ReferenceCounted {
    override def refCnt(): Int = data.refCnt()

    override def retain(): ReferenceCounted = {
      data.retain()
      this
    }

    override def retain(increment: Int): ReferenceCounted = {
      data.retain(increment)
      this
    }

    override def touch(): ReferenceCounted = {
      data.touch()
      this
    }

    override def touch(hint: scala.Any): ReferenceCounted = {
      data.touch(hint)
      this
    }

    override def release(): Boolean = {
      data.release()
    }

    override def release(decrement: Int): Boolean = {
      data.release(decrement)
    }
  }


  class UdpRelayPacketDecoder extends MessageToMessageDecoder[ByteBuf] {

    override def decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: util.List[AnyRef]): Unit = {
      msg.readBytes(2)

      val frag = msg.readUnsignedByte()
      val dstAddrType = Socks5AddressType.valueOf(msg.readByte())
      val dstAddr = Socks5AddressDecoder.DEFAULT.decodeAddress(dstAddrType, msg)
      val dstPort = msg.readUnsignedShort()

      out.add(UdpRelayPacket(frag, dstAddrType, dstAddr, dstPort, msg.retain()))
    }
  }

  class UdpRelayPacketEncoder extends MessageToMessageEncoder[UdpRelayPacket] {

    override def acceptOutboundMessage(msg: scala.Any): Boolean =
      msg.isInstanceOf[UdpRelayPacket]

    override def encode(ctx: ChannelHandlerContext, msg: UdpRelayPacket, out: util.List[AnyRef]): Unit = {
      val buf = Unpooled.buffer()

      buf.writeShort(0)

      buf.writeByte(msg.frag)
      buf.writeByte(msg.dstAddrType.byteValue())
      Socks5AddressEncoder.DEFAULT.encodeAddress(msg.dstAddrType, msg.dstAddr, buf)
      buf.writeShort(msg.dstPort)
      buf.writeBytes(msg.data)

      out.add(buf)
    }
  }

}

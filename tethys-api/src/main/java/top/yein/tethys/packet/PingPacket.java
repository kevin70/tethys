package top.yein.tethys.packet;

import static top.yein.tethys.packet.Namespaces.NS_PING;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 心跳请求消息.
 *
 * @author KK (kzou227@qq.com)
 */
@JsonTypeName(NS_PING)
public class PingPacket implements Packet {

  @Override
  public String getNs() {
    return NS_PING;
  }
}

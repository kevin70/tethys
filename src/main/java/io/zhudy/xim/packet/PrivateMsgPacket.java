package io.zhudy.xim.packet;

import lombok.Value;

/**
 * 私人消息.
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Value
public class PrivateMsgPacket implements Packet {

  /** 发送消息者. */
  String from;
  /** 接收消息者. */
  String to;
  /** 消息文本. */
  String text;
  /** 消息扩展参数. */
  String extraArgs;

  @Override
  public String getNs() {
    return Namespaces.PRIVATE_MSG;
  }
}

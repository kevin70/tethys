package top.yein.tethys.im.handler;

import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import top.yein.tethys.core.MessageProperties;
import top.yein.tethys.entity.PrivateMessage;
import top.yein.tethys.id.MessageIdGenerator;
import top.yein.tethys.im.handler.internal.MessagePacketChecker;
import top.yein.tethys.im.server.PacketHandler;
import top.yein.tethys.packet.PrivateMessagePacket;
import top.yein.tethys.repository.PrivateMessageRepository;
import top.yein.tethys.session.Session;
import top.yein.tethys.session.SessionManager;

/**
 * 私聊处理器.
 *
 * @author KK (kzou227@qq.com)
 */
@Log4j2
@Component
public class PrivateMessageHandler implements PacketHandler<PrivateMessagePacket> {

  private final SessionManager sessionManager;
  private final PrivateMessageRepository privateMessageRepository;
  private final MessageProperties messageProperties;
  private final MessageIdGenerator messageIdGenerator;

  /**
   * 构造函数.
   *
   * @param sessionManager 会话管理器
   * @param privateMessageRepository 私聊消息存储器
   * @param messageProperties 聊天消息静态配置
   * @param messageIdGenerator 消息 ID 生成器
   */
  public PrivateMessageHandler(
      SessionManager sessionManager,
      PrivateMessageRepository privateMessageRepository,
      MessageProperties messageProperties,
      MessageIdGenerator messageIdGenerator) {
    this.sessionManager = sessionManager;
    this.privateMessageRepository = privateMessageRepository;
    this.messageProperties = messageProperties;
    this.messageIdGenerator = messageIdGenerator;
  }

  @Override
  public Mono<Void> handle(@Nonnull Session session, @Nonnull PrivateMessagePacket packet) {
    if (packet.getMsgId() == null && messageProperties.isAutofillId()) {
      packet.setMsgId(messageIdGenerator.nextId());
      log.debug("自动填充私聊消息 ID, packet={}, session={}", packet, session);
    }

    // 校验消息
    MessagePacketChecker.check(packet);
    var from =
        Optional.ofNullable(packet.getFrom())
            .orElseGet(
                () -> {
                  packet.setFrom(session.uid());
                  return session.uid();
                });
    var to = packet.getTo();

    var p1 = sessionManager.findByUid(to).delayUntil(toSession -> toSession.sendPacket(packet));

    // 存储的消息实体
    var entity =
        PrivateMessage.builder()
            .id(packet.getMsgId())
            .senderId(from)
            .receiverId(to)
            .kind(packet.getKind())
            .content(packet.getContent())
            .url(packet.getUrl())
            .customArgs(packet.getCustomArgs())
            .build();
    var p2 = privateMessageRepository.insert(entity);
    return p2.thenMany(p1).then();
  }
}

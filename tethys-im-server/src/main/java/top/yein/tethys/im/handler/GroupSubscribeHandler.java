package top.yein.tethys.im.handler;

import javax.annotation.Nonnull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import top.yein.tethys.im.server.PacketHandler;
import top.yein.tethys.packet.GroupSubscribePacket;
import top.yein.tethys.session.Session;
import top.yein.tethys.session.SessionGroupManager;

/**
 * 订阅群组消息处理器.
 *
 * @author KK (kzou227@qq.com)
 */
@Component
public class GroupSubscribeHandler implements PacketHandler<GroupSubscribePacket> {

  private final SessionGroupManager sessionGroupManager;

  public GroupSubscribeHandler(SessionGroupManager sessionGroupManager) {
    this.sessionGroupManager = sessionGroupManager;
  }

  @Override
  public Mono<Void> handle(@Nonnull Session session, @Nonnull GroupSubscribePacket packet) {
    return sessionGroupManager.subGroups(session, packet.getGroupIds());
  }
}

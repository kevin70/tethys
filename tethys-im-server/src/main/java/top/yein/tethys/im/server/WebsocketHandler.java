package top.yein.tethys.im.server;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.io.DataInput;
import java.io.IOException;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.channel.AbortedException;
import reactor.netty.http.HttpInfos;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.util.context.Context;
import top.yein.chaos.biz.BizCodeException;
import top.yein.tethys.auth.AuthService;
import top.yein.tethys.core.BizCodes;
import top.yein.tethys.core.session.DefaultSession;
import top.yein.tethys.packet.ErrorPacket;
import top.yein.tethys.packet.Packet;
import top.yein.tethys.session.Session;
import top.yein.tethys.session.SessionManager;
import top.yein.tethys.util.JsonUtils;

/**
 * Websocket 处理器.
 *
 * @author KK (kzou227@qq.com)
 */
@Log4j2
@Component
public class WebsocketHandler {

  private static final String BEARER_TOKEN_PREFIX = "Bearer ";
  /** 认证令牌在 query 参数中的名称. */
  private static final String ACCESS_TOKEN_QUERY_NAME = "access_token";
  /** 认证服务. */
  private final AuthService authService;
  /** 会话管理器器. */
  private final SessionManager sessionManager;

  private final PacketDispatcher packetDispatcher;

  private final ObjectReader objectReader;

  /**
   * 构造函数.
   *
   * @param authService 认证服务
   * @param sessionManager 会话管理
   * @param packetDispatcher 包分发器
   */
  public WebsocketHandler(
      AuthService authService, SessionManager sessionManager, PacketDispatcher packetDispatcher) {
    this.authService = authService;
    this.sessionManager = sessionManager;
    this.packetDispatcher = packetDispatcher;
    this.objectReader = JsonUtils.objectMapper().readerFor(Packet.class);
  }

  /**
   * Websocket 处理器.
   *
   * @param in 输入
   * @param out 输出
   * @return RS
   */
  public Mono<Void> handle(WebsocketInbound in, WebsocketOutbound out) {
    final Connection[] connVal = new Connection[1];
    in.withConnection(conn -> connVal[0] = conn);
    final Channel channel = connVal[0].channel();
    final Supplier<Mono<Void>> s =
        () -> {
          String accessToken;
          try {
            accessToken = getAuthorization(in);
          } catch (IllegalArgumentException e) {
            log.debug("[连接关闭] - 错误的认证参数 {} {}", in, e.getMessage());
            return out.sendClose(WebSocketCloseStatus.INVALID_PAYLOAD_DATA.code(), e.getMessage());
          }

          return authService
              .authenticate(accessToken)
              .flatMap(
                  ac -> {
                    // 将会话添加至会话管理器
                    var session =
                        new DefaultSession(
                            sessionManager.sessionIdGenerator().nextId(), in, out, ac);

                    log.info(
                        "channelId: {} > session add to session manager [sessionId={}, uid={}]",
                        channel.id(),
                        session.sessionId(),
                        session.uid());

                    // 会话清理
                    session
                        .onClose()
                        .doFinally(
                            signalType -> {
                              log.debug("会话关闭 session={}, signalType={}", session, signalType);
                              sessionManager.remove(session).subscribe();
                            })
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe();
                    return sessionManager.add(session).thenReturn(session);
                  })
              .flatMap(
                  session -> {
                    receiveFrames(in, session);
                    return out.neverComplete();
                  });
        };
    return Mono.defer(s).doOnError(e -> log.error("未处理的异常 {}", in, e));
  }

  private void receiveFrames(final WebsocketInbound in, final Session session) {
    in.aggregateFrames()
        .receiveFrames()
        .doOnError(
            e -> {
              // TODO: 异常处理需要进一步完善
              // 异常处理
              if (AbortedException.isConnectionReset(e)) {
                return;
              }
              if ((e instanceof IOException
                  && e.getMessage() != null
                  && e.getMessage().contains("远程主机强迫关闭了一个现有的连接"))) {
                return;
              }

              // 业务逻辑异常处理
              if (e instanceof BizCodeException) {
                log.debug("业务异常 session={}", session, e);
              }
            })
        .doOnTerminate(
            () -> {
              // 连接终止、清理
              if (!session.isClosed()) {
                log.debug("会话终止 session={}", session);
                session.close().subscribe();
              }
            })
        .flatMap(frame -> handleFrame(session, frame))
        .subscribe();
  }

  private Mono<Void> handleFrame(final Session session, final WebSocketFrame frame) {
    if (!(frame instanceof BinaryWebSocketFrame || frame instanceof TextWebSocketFrame)) {
      var ep =
          ErrorPacket.builder()
              .code(BizCodes.C400.getCode())
              .message("不支持 的 ws frame 类型")
              .details("当前仅支持 binary/text frame 类型")
              .build();
      return session.sendPacket(ep).then(session.close());
    }

    // 解析包内容
    final ByteBuf content = frame.content();
    final DataInput input = new ByteBufInputStream(content);
    final Packet packet;

    try {
      packet = objectReader.readValue(input);
    } catch (UnrecognizedPropertyException e) {
      var ep =
          ErrorPacket.builder()
              .code(BizCodes.C912.getCode())
              .message("未知的属性")
              .details(e.getPropertyName())
              .build();
      return session.sendPacket(ep).then(session.close());
    } catch (InvalidTypeIdException e) {
      String message;
      if (e.getTypeId() == null) {
        message = "缺少 @ns 命名空间";
      } else {
        message = "非法的 @ns 命名空间 [" + e.getTypeId() + "]";
      }
      log.debug(message, e);
      var ep =
          ErrorPacket.builder()
              .code(BizCodes.C912.getCode())
              .message(message)
              .details(e.getOriginalMessage())
              .build();
      return session.sendPacket(ep).then(session.close());
    } catch (IOException e) {
      // JSON 解析失败
      log.warn("JSON 解析失败 session={}", e);
      var ep = ErrorPacket.builder().code(BizCodes.C912.getCode()).message("解析请求包错误").build();
      return session.sendPacket(ep).then(session.close());
    }

    // 包处理
    return Mono.defer(() -> packetDispatcher.dispatch(session, packet))
        .contextWrite(Context.of(ByteBuf.class, packet))
        .onErrorResume(
            t -> {
              // 业务逻辑异常处理
              if (t instanceof BizCodeException) {
                log.debug("业务异常 session={}", session, t);
                var ex = (BizCodeException) t;
                var ep =
                    ErrorPacket.builder()
                        .code(ex.getBizCode().getCode())
                        .message(ex.getBizCode().getMessage())
                        .details(t.getMessage())
                        .build();
                return session.sendPacket(ep);
              }
              log.error("未处理的异常 session={}, packet={}", session, packet, t);
              return Mono.error(t);
            });
  }

  private String getAuthorization(WebsocketInbound in) throws IllegalArgumentException {
    var bearer = in.headers().get(HttpHeaderNames.AUTHORIZATION);
    if (bearer != null) {
      if (!bearer.startsWith(BEARER_TOKEN_PREFIX)) {
        throw new IllegalArgumentException("header认证必须使用Bearer模式");
      }
      var token = bearer.substring(BEARER_TOKEN_PREFIX.length());
      return token;
    }

    final var httpInfos = (HttpInfos) in;
    final var queryParams = new QueryStringDecoder(httpInfos.uri());
    final var params = queryParams.parameters().get(ACCESS_TOKEN_QUERY_NAME);
    if (params == null || params.isEmpty()) {
      throw new IllegalArgumentException("QUERY中缺少\"access_token\"认证参数");
    }
    return params.get(0);
  }
}

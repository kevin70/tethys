/*
 * Copyright 2019-2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.yein.tethys.core.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import top.yein.tethys.core.auth.NoneAuthContext;
import top.yein.tethys.packet.ErrorPacket;

/**
 * {@link DefaultSession} 单元测试.
 *
 * @author KK (kzou227@qq.com)
 */
class DefaultSessionTest {

  private volatile DisposableServer disposableServer;
  private volatile WebsocketInbound inbound;
  private volatile WebsocketOutbound outbound;
  private volatile Connection webSocketClient;

  @BeforeEach
  void before() throws InterruptedException {
    final var boundQueue = new ArrayBlockingQueue<Tuple2<WebsocketInbound, WebsocketOutbound>>(1);
    disposableServer =
        HttpServer.create()
            .port(0)
            .route(
                routes -> {
                  routes.ws(
                      "/im",
                      (inbound, outbound) -> {
                        boundQueue.offer(Tuples.of(inbound, outbound));
                        return outbound.neverComplete();
                      });
                })
            .wiretap(true)
            .bindNow();

    webSocketClient =
        HttpClient.create()
            .baseUrl("ws://" + disposableServer.host() + ":" + disposableServer.port())
            .websocket()
            .uri("/im")
            .connect()
            .block();

    // 获取 inbound/output
    var tuple = boundQueue.take();
    this.inbound = tuple.getT1();
    this.outbound = tuple.getT2();
  }

  @AfterEach
  void after() {
    disposableServer.disposeNow();
    webSocketClient.disposeNow();
  }

  @Test
  void newSession() {
    var sessionId = UUID.randomUUID().toString();
    var authContext = NoneAuthContext.INSTANCE;
    var session = new DefaultSession(sessionId, inbound, outbound, authContext);

    assertThat(session)
        .hasFieldOrPropertyWithValue("sessionId", sessionId)
        .hasFieldOrPropertyWithValue("inbound", inbound)
        .hasFieldOrPropertyWithValue("outbound", outbound)
        .hasFieldOrPropertyWithValue("authContext", NoneAuthContext.INSTANCE);

    assertThat(session.sessionId()).as("sessionId()").isEqualTo(sessionId);
    assertThat(session.isClosed()).as("isClosed()").isFalse();
    assertThat(session.isAnonymous()).as("isAnonymous()").isEqualTo(authContext.isAnonymous());
  }

  @Test
  void sendPacket() throws InterruptedException {
    var sessionId = UUID.randomUUID().toString();
    var authContext = NoneAuthContext.INSTANCE;
    var session = new DefaultSession(sessionId, inbound, outbound, authContext);

    var queue = new LinkedBlockingQueue<>();
    webSocketClient.inbound().receiveObject().doOnNext(o -> queue.offer(o)).subscribe();

    session
        .sendPacket(ErrorPacket.builder().message("test message").details("test message").build())
        .subscribe();

    var o = queue.poll(5, TimeUnit.SECONDS);
    assertThat(o).isInstanceOf(WebSocketFrame.class);
  }

  @Test
  void closeSession() throws InterruptedException {
    var sessionId = UUID.randomUUID().toString();
    var authContext = NoneAuthContext.INSTANCE;
    var session = new DefaultSession(sessionId, inbound, outbound, authContext);

    // 监听会话 close 事件
    var cdl = new CountDownLatch(2);
    session.onClose().doFinally(s -> cdl.countDown()).subscribe();
    session.onClose().doFinally(s -> cdl.countDown()).subscribe();

    // 关闭会话
    session.close().then(session.close()).subscribe();
    assertThat(cdl.await(5, TimeUnit.SECONDS)).isTrue();

    assertThat(cdl.getCount()).as("onClose() count").isZero();
    assertThat(session.isClosed()).as("isClosed()").isTrue();
    outbound.withConnection(
        connection -> {
          // 判断会话连接是否关闭
          assertThat(connection.channel().isActive()).isFalse();
        });
  }
}

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
package top.yein.tethys.core.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.PrematureJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import top.yein.chaos.biz.BizCodeException;
import top.yein.tethys.auth.AuthContext;
import top.yein.tethys.auth.AuthService;
import top.yein.tethys.auth.NoneAuthContext;
import top.yein.tethys.core.BizCodes;

/**
 * <a href="https://tools.ietf.org/html/rfc7515">JWS</a> 用户认证服务实现.
 *
 * @author KK (kzou227@qq.com)
 */
@Log4j2
public class JwsAuthService implements AuthService {

  private final JwtParser jwtParser;
  private final boolean anonymousEnabled;
  private final Map<String, SecretKey> jwtSecrets;

  @Inject
  public JwsAuthService(boolean anonymousEnabled, Map<String, SecretKey> jwtSecrets) {
    this.anonymousEnabled = anonymousEnabled;
    this.jwtParser =
        Jwts.parserBuilder()
            .setSigningKeyResolver(new DefaultSigningKeyResolver(jwtSecrets))
            .build();
    this.jwtSecrets = jwtSecrets;
  }

  @Override
  public Mono<Boolean> anonymousEnabled() {
    return Mono.just(anonymousEnabled);
  }

  @Override
  public Mono<AuthContext> authorize(String token) {
    if (token == null || token.isEmpty()) {
      if (anonymousEnabled) {
        return Mono.just(NoneAuthContext.INSTANCE);
      } else {
        return Mono.error(new BizCodeException(BizCodes.C401, "缺少访问令牌"));
      }
    }

    return Mono.create(
        sink -> {
          try {
            var jws = jwtParser.parseClaimsJws(token);
            var authContext = new JwsAuthContext(token, jws.getBody());
            sink.success(authContext);
          } catch (MalformedJwtException e) {
            sink.error(new BizCodeException(BizCodes.C3300, e.getMessage()));
          } catch (ExpiredJwtException e) {
            sink.error(new BizCodeException(BizCodes.C3301, e.getMessage()));
          } catch (PrematureJwtException e) {
            sink.error(new BizCodeException(BizCodes.C3302, e.getMessage()));
          } catch (Exception e) {
            sink.error(new BizCodeException(BizCodes.C3305, e.getMessage()));
          }
        });
  }

  @Override
  public Mono<String> generateToken(long uid) {
    var keys = new ArrayList<>(jwtSecrets.entrySet());
    Collections.shuffle(keys);

    var entry = keys.get(0);
    Map<String, Object> header = Jwts.jwsHeader().setKeyId(entry.getKey());
    var claims = Jwts.claims().setId(String.valueOf(uid));
    var token =
        Jwts.builder()
            .signWith(entry.getValue(), SignatureAlgorithm.HS512)
            .setHeader(header)
            .setClaims(claims)
            .compact();

    log.info("生成访问令牌 [uid={}, access_token={}]", uid, token);
    return Mono.just(token);
  }
}

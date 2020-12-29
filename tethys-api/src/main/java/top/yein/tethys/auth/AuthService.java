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
package top.yein.tethys.auth;

import reactor.core.publisher.Mono;

/**
 * 用户认证服务.
 *
 * @author KK (kzou227@qq.com)
 */
public interface AuthService {

  /**
   * 返回是否启用匿名认证.
   *
   * @return 是否启用匿名认证
   */
  Mono<Boolean> anonymousEnabled();

  /**
   * 用户认证.
   *
   * @param token 认证令牌
   * @return 认证上下文
   */
  Mono<AuthContext> authorize(String token);

  /**
   * 生成访问令牌.
   *
   * @param uid 用户 ID
   * @return 访问令牌
   */
  Mono<String> generateToken(long uid);
}

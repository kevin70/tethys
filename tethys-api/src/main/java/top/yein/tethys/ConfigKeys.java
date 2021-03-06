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
package top.yein.tethys;

/**
 * 应用配置键名称定义.
 *
 * @author KK (kzou227@qq.com)
 */
public final class ConfigKeys {

  private ConfigKeys() {}

  /**
   * JWT HMAC 密钥配置.
   *
   * <p>e.g:
   *
   * <ul>
   *   <li>jwt.secrets.[key1]=This is secret of key1
   *   <li>jwt.secrets.[keyX]=This is secret of keyX
   * </ul>
   */
  public static final String JWT_SECRETS = "jwt.secrets";

  /**
   * IM 服务开放访问的地址.
   *
   * <p>地址中可包含 `IP` 及 `PORT`.
   *
   * <p>配置示例:
   *
   * <ul>
   *   <li>:8888
   *   <li>192.168.1.5:8888
   * </ul>
   */
  public static final String IM_SERVER_ADDR = "im-server.addr";

  /** 是否启用匿名连接. */
  public static final String IM_SERVER_ENABLED_ANONYMOUS = "im-server.enabled-anonymous";

  /** JWT 密钥配置前缀. */
  public static final String IM_SERVER_AUTH_JWT_SECRETS = "im-server.auth-jwt-secrets";

  /**
   * REST 服务开放访问的地址.
   *
   * <p>地址中可包含 `IP` 及 `PORT`.
   *
   * <p>配置示例:
   *
   * <ul>
   *   <li>:8888
   *   <li>192.168.1.5:8888
   * </ul>
   */
  public static final String REST_SERVER_ADDR = "rest-server.addr";

  /**
   * 消息存储的数据库链接.
   *
   * <p>示例：
   *
   * <p>{@code r2dbc:postgresql://[<username>:<password>@]<host>:5432/<database>}
   */
  public static final String MESSAGE_STORAGE_R2DBC_URL = "message-storage.r2dbc.url";
}

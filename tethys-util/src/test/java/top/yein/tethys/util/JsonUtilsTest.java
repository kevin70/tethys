package top.yein.tethys.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link JsonUtils} 单元测试.
 *
 * @author KK (kzou227@qq.com)
 */
class JsonUtilsTest {

  @Test
  void objectMapper() {
    assertThat(JsonUtils.objectMapper()).isNotNull();
  }
}

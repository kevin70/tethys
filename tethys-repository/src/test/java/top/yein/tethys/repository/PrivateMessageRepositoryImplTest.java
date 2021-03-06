package top.yein.tethys.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import top.yein.tethys.constants.MessageKind;
import top.yein.tethys.entity.PrivateMessage;
import top.yein.tethys.query.PrivateMessageQuery;

/**
 * {@link PrivateMessageRepositoryImpl} 单元测试.
 *
 * @author KK (kzou227@qq.com)
 */
class PrivateMessageRepositoryImplTest extends AbstractTestRepository {

  Faker faker = new Faker(Locale.SIMPLIFIED_CHINESE);

  @Test
  void insert() {
    var repo = new PrivateMessageRepositoryImpl(dc);

    var entity = new PrivateMessage();
    entity.setId(TestUtils.newMessageId());
    entity.setSenderId("TEST-SENDER");
    entity.setReceiverId("TEST-RECEIVER");
    entity.setKind(MessageKind.TEXT.getCode());
    entity.setContent("unit test");
    entity.setUrl("https://via.placeholder.com/150");
    entity.setCustomArgs("{}");

    var tuple =
        super.transactional(
                repo.insert(entity)
                    .zipWith(
                        findOne(
                            "select * from t_private_message where id=:id",
                            Map.of("id", entity.getId()))))
            .block();

    // 校验数据库存储数据
    var dbRow = tuple.getT2();
    assertSoftly(
        s -> {
          s.assertThat(dbRow.get("id")).as("id").isEqualTo(entity.getId());
          s.assertThat(dbRow.get("sender_id")).as("sender_id").isEqualTo(entity.getSenderId());
          s.assertThat(dbRow.get("receiver_id"))
              .as("receiver_id")
              .isEqualTo(entity.getReceiverId());
          s.assertThat(dbRow.get("kind")).as("kind").isEqualTo((short) entity.getKind());
          s.assertThat(dbRow.get("content")).as("content").isEqualTo(entity.getContent());
          s.assertThat(dbRow.get("url")).as("url").isEqualTo(entity.getUrl());
          s.assertThat(dbRow.get("custom_args"))
              .as("custom_args")
              .isEqualTo(entity.getCustomArgs());
          s.assertThat(dbRow.get("unread")).as("unread").isEqualTo((short) 1);
          s.assertThat(dbRow.get("create_time")).as("create_time").isNotNull();
          s.assertThat(dbRow.get("update_time")).as("update_time").isNotNull();
        });
  }

  @Test
  void readMessage() {
    var repo = new PrivateMessageRepositoryImpl(dc);

    var entity = new PrivateMessage();
    entity.setId(TestUtils.newMessageId());
    entity.setSenderId("TEST-SENDER");
    entity.setReceiverId("TEST-RECEIVER");
    entity.setKind(MessageKind.TEXT.getCode());
    entity.setContent("unit test");
    entity.setUrl("https://via.placeholder.com/150");
    entity.setCustomArgs("{}");

    var fo = findOne("select * from t_private_message where id=:id", Map.of("id", entity.getId()));
    var tuple =
        super.transactional(repo.insert(entity).then(repo.readMessage(entity.getId()).zipWith(fo)))
            .block();

    // 校验数据库存储数据
    var dbRow = tuple.getT2();
    assertSoftly(
        s -> {
          s.assertThat(dbRow.get("id")).as("id").isEqualTo(entity.getId());
          s.assertThat(dbRow.get("unread")).as("unread").isEqualTo((short) 0);
          s.assertThat(dbRow.get("update_time")).as("update_time").isNotNull();
        });
  }

  @Test
  void batchReadMessage() {
    var repo = new PrivateMessageRepositoryImpl(dc);

    var list = new ArrayList<PrivateMessage>();
    var receiverId = "TEST-RECEIVER";
    var max = 100;
    for (int i = 0; i < max; i++) {
      var entity = new PrivateMessage();
      entity.setId(TestUtils.newMessageId());
      entity.setSenderId("TEST-SENDER");
      entity.setReceiverId(receiverId);
      entity.setKind(MessageKind.TEXT.getCode());
      entity.setContent("unit test");
      entity.setUrl("https://via.placeholder.com/150");
      entity.setCustomArgs("{}");
      list.add(entity);
    }
    var ids = list.stream().map(PrivateMessage::getId).collect(Collectors.toList());
    var p =
        super.transactional(
            Flux.fromIterable(list)
                .flatMap(repo::insert)
                .then(repo.batchReadMessage(ids, receiverId)));
    StepVerifier.create(p).expectNext(max).expectComplete().verify();
  }

  @Test
  void findById() {
    var repo = new PrivateMessageRepositoryImpl(dc);

    var entity = new PrivateMessage();
    entity.setId(TestUtils.newMessageId());
    entity.setSenderId("TEST-SENDER");
    entity.setReceiverId("TEST-RECEIVER");
    entity.setKind(MessageKind.TEXT.getCode());
    entity.setContent("unit test");
    entity.setUrl("https://via.placeholder.com/150");
    entity.setCustomArgs("{}");

    var p = super.transactional(repo.insert(entity).thenMany(repo.findById(entity.getId())));
    StepVerifier.create(p)
        .assertNext(
            dbRow ->
                assertSoftly(
                    s -> {
                      s.assertThat(dbRow.getId()).as("id").isEqualTo(entity.getId());
                      s.assertThat(dbRow.getSenderId())
                          .as("sender_id")
                          .isEqualTo(entity.getSenderId());
                      s.assertThat(dbRow.getReceiverId())
                          .as("receiver_id")
                          .isEqualTo(entity.getReceiverId());
                      s.assertThat(dbRow.getKind()).as("kind").isEqualTo(entity.getKind());
                      s.assertThat(dbRow.getContent()).as("content").isEqualTo(entity.getContent());
                      s.assertThat(dbRow.getUrl()).as("url").isEqualTo(entity.getUrl());
                      s.assertThat(dbRow.getCustomArgs())
                          .as("custom_args")
                          .isEqualTo(entity.getCustomArgs());
                      s.assertThat(dbRow.getUnread()).as("unread").isEqualTo(1);
                      s.assertThat(dbRow.getCreateTime()).as("create_time").isNotNull();
                      s.assertThat(dbRow.getUpdateTime()).as("update_time").isNotNull();
                    }))
        .verifyComplete();
  }

  @Test
  void findMessages() {
    var repo = new PrivateMessageRepositoryImpl(dc);
    var list = new ArrayList<PrivateMessage>();
    var max = 100;
    for (int i = 0; i < max; i++) {
      var entity = new PrivateMessage();
      entity.setId(TestUtils.newMessageId());
      entity.setSenderId("TEST-SENDER");
      entity.setReceiverId("TEST-RECEIVER");
      entity.setKind(MessageKind.TEXT.getCode());
      entity.setContent("unit test");
      entity.setUrl("https://via.placeholder.com/150");
      entity.setCustomArgs("{}");
      list.add(entity);
    }
    var query = new PrivateMessageQuery();
    query.setReceiverId("TEST-RECEIVER");
    query.setCreateTime(LocalDateTime.now().minusMinutes(5));
    query.setLimit(faker.random().nextInt(1, max));

    var p =
        super.transactional(
            Flux.fromIterable(list).flatMap(repo::insert).thenMany(repo.findMessages(query)));
    var messages = new ArrayList<PrivateMessage>();
    StepVerifier.create(p)
        .recordWith(() -> messages)
        .thenConsumeWhile(unused -> true)
        .expectComplete()
        .verify();
    assertThat(messages).hasSize(query.getLimit());
  }
}

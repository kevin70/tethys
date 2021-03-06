create table t_message
(
    id           char(14)                not null,
    sender_id    bigint,
    receiver_id  bigint,
    group_id     bigint,
    kind         smallint                not null,
    content      varchar(4096),
    content_kind smallint  default 1     not null,
    url          varchar(1024),
    custom_args  varchar(1024),
    unread       smallint  default 1,
    create_time  timestamp default now() not null,
    update_time  timestamp default now() not null
);

comment on table t_message is '消息表';

comment on column t_message.id is '全局消息 ID';

comment on column t_message.sender_id is '发送人 ID';

comment on column t_message.receiver_id is '接收人 ID';

comment on column t_message.group_id is '群 ID';

comment on column t_message.kind is '数据类型
1: 系统消息
2: 私聊消息
3: 群聊消息';

comment on column t_message.content is '消息内容
1：普通文本消息
2：图片消息
3：音频消息
4：视频消息';

comment on column t_message.content_kind is '消息内容类型';

comment on column t_message.url is '统一资源定位器';

comment on column t_message.custom_args is '自定义参数';

comment on column t_message.create_time is '创建时间';

comment on column t_message.update_time is '更新时间';

create unique index t_message_id_uindex on t_message (id);
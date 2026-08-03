message-service 消息通知服务详细开发文档
1 模块定位与业务职责
1.1 模块定位
系统统一消息推送中台，独立数据库 fresh_message，全平台所有通知统一收口，基于 RocketMQ 异步消费各类业务消息，统一封装小程序订阅消息、短信、站内信推送能力，解耦业务服务与第三方推送接口。
1.2 核心业务职责
消息模板管理：小程序订阅消息模板、短信模板后台配置；
消费全业务 Topic 消息（订单创建 / 支付 / 取消、拼团成团 / 过期、AI 理赔完成、积分变动）；
多渠道消息分发：微信小程序订阅消息、短信、用户站内消息；
推送记录持久化存储，记录发送状态、失败原因，支持失败重试；
站内信统一管理，用户可查询、标记已读；
对外 Feign 接口，供后台查看消息推送日志、管理模板；
失败消息定时重试机制，保证通知触达可靠性。
1.3 依赖中间件
Nacos 注册配置、Redis（消息模板缓存、限流）、RocketMQ、MySQL (fresh_message)、MyBatis-Plus、Sentinel、common-core
1.4 依赖远程服务
user-service（获取用户 openid、手机号）、order-service（查询订单详情填充消息内容）


2 技术栈 & Maven 核心依赖
xml


<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Nacos 注册配置中心 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<!-- Sentinel 限流熔断 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
<!-- Redis 缓存消息模板、发送限流 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- RocketMQ 消息消费者核心 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-rocketmq</artifactId>
</dependency>
<!-- MySQL + MyBatis-Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- OpenFeign 远程调用用户、订单服务 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<!-- 公共基础模块 -->
<dependency>
    <groupId>com.fresh</groupId>
    <artifactId>common-core</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- 微信消息SDK、短信第三方SDK、lombok、json工具省略 -->




3 Nacos 配置 message-service-dev.yaml
yaml




spring:
  application:
    name: message-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
    sentinel:
      transport:
        dashboard: 127.0.0.1:8080
        port: 8726
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:9876
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
# 消息专属数据库 fresh_message
datasource:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://127.0.0.1:3306/fresh_message?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
  username: root
  password: root
# 业务自定义配置
fresh:
  message:
    # 消息模板缓存过期时间
    template-cache-ttl: 1800
    # 消息发送单用户每日上限（防骚扰）
    user-daily-limit: 50
    # 失败消息重试间隔（分钟）
    retry-interval: 5
    # 最大重试次数
    max-retry: 3
# 微信小程序配置
wx:
  appid: xxx
  secret: xxx
# 短信服务商配置
sms:
  access-key: xxx
  secret-key: xxx
  sign-name: 社区生鲜团购
# MyBatis-Plus 全局逻辑删除
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.fresh.message.entity
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
      
      
      
4 核心业务流程时序
4.1 业务消息统一推送全流程
order/ai/user 等业务服务发送各类业务 Topic 消息；
message-service 统一消费消息，解析消息体：用户 ID、业务类型、业务 ID、消息参数；
Feign 调用 user-service 获取用户 openid、绑定手机号；
查询 Redis 缓存消息模板，填充模板占位符（订单号、商品名称、理赔金额等）；
校验用户当日消息发送次数，超出上限直接丢弃，记录日志；
并行执行三类推送：
小程序订阅消息推送微信接口；
手机号短信推送第三方短信平台；
写入站内消息表 user_inner_msg；
推送结果存入 msg_send_log 发送记录表；
发送失败标记状态 = 2，加入定时重试任务；发送成功状态 = 1。
4.2 失败消息定时重试流程
定时任务每 5 分钟执行一次；
查询 msg_send_log 中 send_status=2、重试次数 < 3 的记录；
重新执行推送逻辑，重试次数 + 1；
三次仍失败，标记终止重试，记录失败原因日志。
4.3 AI 售后识别完成通知流程
ai-service 发送 AI_RECOGNIZE_FINISH_TOPIC；
message-service 消费消息，携带售后工单 ID、理赔金额；
Feign 调用 order-service 获取售后商品信息；
填充售后通知模板，推送小程序 + 短信；
生成站内消息存入用户消息列表。
5 Redis 缓存 Key 设计（message-service 专属）
表格
Key 格式	存储内容	过期时间	使用场景
msg:template:{templateId}	消息模板完整 JSON	1800s(30min)	推送时快速读取模板
msg:user:daily:{userId}:{date}	用户当日消息发送计数	当日零点自动过期	用户防骚扰限流
6 RocketMQ Topic 收发清单
生产者（message-service 发送）
无业务生产 Topic，仅消费、不对外发送业务消息；
消费者（message-service 监听全部业务通知 Topic）
ORDER_CREATE_TOPIC：订单创建通知；
ORDER_SUCCESS_TOPIC：支付成功通知；
ORDER_UNPAID_TOPIC：订单超时取消通知；
GROUP_EXPIRE_TOPIC：拼团过期解散通知；
INTEGRAL_CHANGE_TOPIC：积分变动通知；
AI_RECOGNIZE_FINISH_TOPIC：AI 坏果识别完成、理赔通知。
7 数据库操作规范（fresh-message-service.sql）
7.1 数据表清单
msg_template、msg_send_log、user_inner_msg
7.2 约束规范
msg_template 模板支持逻辑删除，修改模板后主动清除对应 Redis 缓存；
msg_send_log 推送日志永久留存，用于对账、用户投诉追溯；
user_inner_msg 站内信仅标记已读，不删除，用户可长期查看；
user_id 建立索引，快速查询单个用户全部站内消息；
第三方推送返回原始报文存入 TEXT 字段，便于排查发送失败问题。
8 核心接口定义
8.1 小程序 C 端接口
GET /inner/list 查询用户站内消息列表
PUT /inner/read/{msgId} 标记消息已读
8.2 后台管理接口
CRUD /template/** 消息模板新增、编辑、启用停用
GET /send/log/page 消息推送记录分页查询、筛选失败记录
POST /send/retry/{logId} 手动重试失败推送消息
8.3 Feign 对外接口（仅后台统计调用）
java
运行
@FeignClient("message-service")
public interface MessageFeignClient {
    // 查询用户近期消息推送数量
    @GetMapping("/feign/user/count/{userId}")
    Result<Long> getUserMsgCount(@PathVariable Long userId);
}
9 Sentinel 限流熔断规则
消息批量推送接口限流，QPS 上限 300，防止大量消息压垮第三方短信 / 微信接口；
单用户消息发送限流，配合 Redis 每日计数双重防护；
Feign 调用用户、订单服务超时熔断，仅记录日志，不阻断消息消费。


10 Docker 部署配置


Dockerfile
dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/message-service-1.0.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=dev"]


docker-compose.yml 片段
yaml



message-service:
  build: ./message-service
  ports:
    - "8085:8085"
  depends_on:
    - nacos
    - redis
    - rocketmq
    - mysql
    
    
    
11 跨服务联调规范
所有业务服务禁止直接调用推送接口，统一发送 RocketMQ 消息解耦；
仅后台管理模块通过 Feign 查询消息日志、模板配置；
用户 openid、手机号只能通过 Feign 调用 user-service 获取，不直连用户库；
订单、售后详情通过 Feign 查询 order-service 填充消息内容；
第三方推送密钥统一配置在 Nacos，不在代码硬编码。
12 统一异常处理
400：模板不存在、用户无手机号 / 未授权小程序订阅消息；
429：用户当日消息达到上限，停止推送；
500：微信 / 短信第三方接口调用失败，自动加入重试队列；
熔断降级：消息消费失败，RocketMQ 重试投递，不丢失通知事件。
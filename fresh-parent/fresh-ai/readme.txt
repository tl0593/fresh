ai-service AI 智能服务详细开发文档
1 模块定位与业务职责
1.1 模块定位
项目核心创新独立微服务，专属数据库 fresh_ai，解耦大模型、图像识别能力，支持单独扩容；不参与下单、库存等核心交易流程，纯智能辅助业务，所有 AI 任务依靠 RocketMQ 异步执行，避免同步阻塞前端。
1.2 核心业务职责
LLM 大模型对话能力：7×24 小时 AI 智能客服、用户个性化菜谱生成；
团购活动宣传文案自动生成；
生鲜坏果图像识别：自动判定腐烂 / 磕碰等级、计算理赔金额；
AI 对话上下文 Redis 缓存，维持连续问答会话；
后台 AI 问答知识库 CRUD、对话日志、图片识别记录持久化；
消费售后图片 MQ 消息，异步完成图像识别并回写售后工单定损数据；
对外提供 Feign 接口，供商品、后台管理调用文案生成、智能问答；
识别记录、对话记录持久入库，用于后台数据查看、问题追溯。
1.3 依赖中间件
Nacos 注册配置、Redis（AI 会话上下文、知识库缓存）、RocketMQ、MySQL (fresh_ai)、MyBatis-Plus、Sentinel、common-core
1.4 依赖远程服务
order-service（读取售后工单、回写 AI 定损结果）、goods-service（商品信息用于菜谱 / 文案生成）


2 技术栈 & Maven 核心依赖
xml



<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Nacos注册配置 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<!-- Sentinel限流熔断 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
<!-- Redis缓存会话、知识库 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- RocketMQ消息队列（异步图片识别任务） -->
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
<!-- OpenFeign远程调用订单、商品服务 -->
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
<!-- 大模型调用SDK、图像识别工具、lombok、OSS图片读取工具省略 -->



3 Nacos 配置 ai-service-dev.yaml
yaml




spring:
  application:
    name: ai-service
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
        port: 8725
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:9876
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
# AI专属数据库 fresh_ai
datasource:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://127.0.0.1:3306/fresh_ai?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
  username: root
  password: root
# AI业务自定义配置
fresh:
  ai:
    # AI对话会话缓存过期时间 30分钟
    session-ttl: 1800
    # 知识库缓存刷新间隔 10分钟
    knowledge-refresh-cron: "0 */10 * * * ?"
    # 图片识别单次最大并发
    image-rec-max-concurrent: 20
    # 大模型接口超时时间
    llm-timeout: 5000
# MyBatis-Plus全局逻辑删除
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.fresh.ai.entity
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
# 大模型第三方接口密钥配置
llm:
  api-key: xxx
  base-url: https://xxx.llm.com/api
# 图像识别接口配置
image-rec:
  api-key: xxx
  base-url: https://xxx.vision.com/api
  
  
  
  
4 核心业务流程时序
4.1 AI 智能客服连续问答流程
小程序 AI 客服页面发送用户提问，携带 userId、sessionKey；
查询 Redis ai:session:{userId}:{sessionKey} 获取历史对话上下文；
拼接历史对话 + 当前提问，调用 LLM 大模型接口；
保存本次用户提问、AI 回复至 Redis 会话上下文，30 分钟过期；
异步写入 ai_chat_record 对话记录表；
返回 AI 回答文本给前端；
定时任务清理过期会话缓存，归档对话记录。
4.2 售后图片异步识别定损流程（核心 MQ 异步）
用户小程序上传坏果图片发起售后，order-service 发送 AFTER_SALE_IMAGE_TOPIC 消息；
ai-service 监听该 Topic，消费消息获取 afterSaleId、图片 URL；
调用图像识别接口，解析损坏等级、损坏比例、建议理赔金额；
组装识别结果，Feign 调用 order-service 接口，更新 after_sale 工单 AI 定损字段；
将原始识别 JSON、图片、定损数据存入 ai_image_recognize 表；
发送消息至 message-service，推送售后 AI 识别完成通知给用户。
4.3 团购文案智能生成流程
管理员后台创建团购活动，输入商品名称、规格、产地；
后台调用 ai-service /ai/group/text/generate 接口；
拼接商品信息 Prompt 请求大模型，生成宣传文案；
记录输入输出至 ai_group_text 日志表；
返回多套宣传文案供管理员选择。
4.4 个性化菜谱生成流程
用户首页点击 AI 菜谱，携带 userId；
Feign 调用 user-service 获取用户历史下单商品、偏好；
大模型根据用户生鲜消费记录生成一周采购清单 + 配套菜谱；
缓存本次菜谱会话，用户可追问做法、搭配。
5 Redis 缓存 Key 设计（ai-service 专属）
表格
Key 格式	存储内容	过期时间	使用场景
ai:session:{userId}:{sessionKey}	用户对话上下文数组 JSON	1800s(30min)	AI 连续问答会话
ai:knowledge:list	后台配置问答知识库全量数据	600s(10min)	客服快速匹配标准问答
ai:cook:{userId}	用户生成的菜谱缓存	3600s(1h)	菜谱页面重复查询加速
6 RocketMQ Topic 收发清单
生产者（ai-service 发送）
AI_RECOGNIZE_FINISH_TOPIC：图片识别完成，推送消息通知服务；
AI_CHAT_BEHAVIOR_TOPIC：用户 AI 对话行为埋点，供 data-service 统计。
消费者（ai-service 监听）
AFTER_SALE_IMAGE_TOPIC：售后图片异步识别任务（核心消费 Topic）。
7 数据库操作规范（fresh-ai-service.sql）
7.1 数据表清单
ai_chat_record、ai_image_recognize、ai_group_text、ai_knowledge
7.2 约束规范
ai_knowledge 知识库支持逻辑删除，修改后自动清空 Redis 知识库缓存；
ai_chat_record、ai_image_recognize、ai_group_text 为日志流水表，只新增不修改；
图片识别原始返回 JSON 存入 TEXT 字段，完整留存用于问题复盘；
用户 ID 建立普通索引，快速查询单用户全部对话、理赔识别记录；
大模型返回长文本统一使用 TEXT，不限制 VARCHAR 长度。
8 核心接口定义
8.1 小程序 C 端接口
POST /ai/chat/send AI 客服对话提问
GET /ai/cook/generate 根据用户偏好生成个性化菜谱
GET /ai/cook/history 查询历史生成菜谱
8.2 后台管理接口
CRUD /ai/knowledge/** AI 问答知识库维护
GET /ai/chat/log/page 用户 AI 对话日志分页查询
GET /ai/image/rec/log/page 图片理赔识别记录
GET /ai/group/text/log/page 团购文案生成日志
8.3 Feign 对外接口（供 goods-service、后台调用）
java
运行
@FeignClient("ai-service")
public interface AiFeignClient {
    // 根据商品信息生成团购宣传文案
    @PostMapping("/feign/group/text")
    Result<String> generateGroupText(@RequestBody GoodsInfoDTO dto);
    // 批量生成商品配套菜谱
    @PostMapping("/feign/cook/batch")
    Result<List<String>> batchGenerateCook(@RequestBody List<Long> goodsIdList);
}
9 Sentinel 限流熔断规则
AI 对话接口限流：单用户每分钟最多 30 次提问，防止刷大模型消耗；
图片识别接口单 IP 每分钟 20 次，限制恶意上传图片；
LLM 大模型调用超时熔断，返回 “AI 服务繁忙，请稍后再试”；
文案生成接口 QPS 上限 100，避免大模型接口过载。


10 Docker 部署配置
Dockerfile
dockerfile



FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/ai-service-1.0.0.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=dev"]



docker-compose.yml 片段
yaml



ai-service:
  build: ./ai-service
  ports:
    - "8084:8084"
  depends_on:
    - nacos
    - redis
    - rocketmq
    - mysql
  # AI服务支持单独扩容，可配置多副本
  deploy:
    replicas: 2
    
    
    
11 跨服务联调规范
售后图片识别全程 MQ 异步，不使用同步 Feign 调用，避免页面超时；
订单服务仅通过 Feign 回写 AI 定损结果，不直接操作 fresh_ai 库；
商品服务创建团购时同步调用 AI 文案生成 Feign 接口；
用户服务提供用户历史订单数据，用于 AI 菜谱偏好分析；
data-service 消费 AI 行为 MQ 消息，统计 AI 使用频次、售后识别量；
所有 AI 第三方接口密钥统一放在 Nacos 配置，不硬编码。
12 统一异常处理
400：图片格式非法、提问内容违规、商品信息缺失无法生成文案；
429：AI 接口调用频繁触发限流；
500：大模型接口调用失败、图像识别服务异常；
熔断降级：AI 对话返回兜底标准问答，不阻断页面。

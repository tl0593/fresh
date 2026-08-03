order-service 订单服务详细开发文档
1 模块定位与业务职责
1.1 模块定位
系统交易核心微服务，独立数据库 fresh_order，承载下单、支付、拼团、售后、订单超时自动处理全交易链路；全链路异步化，重度依赖 RocketMQ 延时消息解耦，是业务数据核心沉淀模块。
1.2 核心业务职责
用户下单结算、生成主订单 + 订单项；
社区拼团创建、参团、成团状态流转管理；
微信支付回调接收、订单支付状态更新；
30 分钟未支付订单自动取消、库存归还（RocketMQ 延时消息）；
拼团到期延时消息消费，自动解散未成团订单、退款；
用户发起售后理赔工单，推送图片识别消息至 AI 服务；
售后人工审核、退款处理；
对外 Feign 接口，供后台、数据统计服务查询订单数据；
订单创建、支付、售后、成团等事件发送 MQ，推送消息通知、用户积分、行为埋点；
订单、售后数据持久化，所有交易流水永久留存，支持对账追溯。
1.3 依赖中间件
Nacos 注册配置、Redis（临时订单缓存、拼团热点数据）、RocketMQ（延时消息、业务异步消息）、MySQL (fresh_order)、MyBatis-Plus、Sentinel、common-core
1.4 依赖远程服务
user-service（用户 / 地址 / 积分）、goods-service（库存扣减归还）、ai-service（售后定损）



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
<!-- Redis 缓存 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- RocketMQ 消息队列（支持延时消息） -->
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
<!-- OpenFeign 远程调用用户/商品服务 -->
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
<!-- 微信支付SDK、lombok、json工具省略 -->





3 Nacos 配置 order-service-dev.yaml
yaml




spring:
  application:
    name: order-service
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
        port: 8724
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:9876
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
# 订单专属数据库 fresh_order
datasource:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://127.0.0.1:3306/fresh_order?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
  username: root
  password: root
# 业务自定义配置
fresh:
  order:
    # 订单未支付自动取消延时时间 30分钟（单位秒）
    unpaid_timeout: 1800
    # 拼团过期时长 24小时
    group_expire_second: 86400
    # 拼团列表缓存过期时间
    group_cache_ttl: 600
# MyBatis-Plus 全局逻辑删除配置
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.fresh.order.entity
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
      
      
      
      
      
4 核心业务流程时序
4.1 正常下单支付全流程
小程序提交结算订单，携带购物车选中商品、地址 ID；
Feign 调用 user-service 获取用户信息、收货地址；
Feign 调用 goods-service 批量校验商品规格库存；
若使用积分，Feign 调用 user-service 冻结积分（frozen_integral）；
开启本地事务，生成唯一订单号，写入 order_main（含地址快照）、order_item（含activity_type+activity_id）；
Feign 调用 goods-service 扣减对应商品 / 团购 / 秒杀 Redis 库存；
发送延时 RocketMQ 消息 ORDER_UNPAID_TOPIC，延时 30 分钟；
发送 ORDER_CREATE_TOPIC 业务消息，用于消息推送、用户行为埋点；
返回订单号、支付链接给前端；
用户完成微信支付，微信回调支付接口；
更新订单状态为待自提，写入 pay_log 支付记录；
发送 ORDER_SUCCESS_TOPIC，触发积分结算（冻结转扣减）、成团校验、订单通知。
4.2 30 分钟未支付自动取消流程（延时消息消费）
RocketMQ 延时消息到达，order-service 消费；
查询订单状态，若仍为待支付：
更新订单状态为已取消，标记 timeout_cancel=1；
Feign 调用 user-service 返还冻结积分（unfreeze）；
根据 order_item.activity_type 判断库存类型（1普通 2团购 3秒杀），Feign 调用 goods-service 归还对应库存；
发送订单取消通知消息给 message-service；
订单已支付 / 已取消则直接丢弃消息。
4.3 拼团业务流程
用户发起拼团：创建 group_record 团长记录，生成订单；
其他用户参团：新增 group_join 记录，更新拼团当前人数；
人数达到成团目标：修改拼团状态为已成团，推送成团通知；
未达到人数：发送 24 小时延时消息 GROUP_EXPIRE_TOPIC；
消息到期消费：解散拼团，自动退款所有参团订单，归还团购库存。
4.4 售后理赔 AI 识别流程
用户在订单中心发起售后，上传损坏图片；
新增 after_sale 售后工单，状态待审核；
发送 AFTER_SALE_IMAGE_TOPIC MQ 消息，携带工单 ID、图片地址；
ai-service 异步消费消息，图像识别损坏等级，回写 AI 定损数据至 after_sale；
后台管理员查看 AI 识别结果，人工审核通过执行退款。
5 Redis 缓存 Key 设计（order-service 专属）
表格
Key 格式	存储内容	过期时间	使用场景
order:group:hot:{userId}	用户参与中拼团列表	10min	个人中心拼团展示
order:temp:{orderNo}	下单临时订单信息	30min	下单过程临时缓存
order:user:list:{userId}	用户最近订单分页缓存	5min	订单中心列表加速
6 RocketMQ Topic 收发清单
生产者（order-service 发送）
ORDER_CREATE_TOPIC：订单创建，推送通知、用户行为埋点；
ORDER_UNPAID_TOPIC：30 分钟延时消息，处理超时未支付订单；
ORDER_SUCCESS_TOPIC：支付成功，发放积分、推送通知；
GROUP_EXPIRE_TOPIC：24 小时延时消息，解散过期拼团；
AFTER_SALE_IMAGE_TOPIC：售后图片消息，交由 AI 识别定损；
ORDER_CANCEL_STOCK_TOPIC：订单取消，通知商品服务归还库存。
消费者（order-service 监听）
无外部业务 Topic 消费，仅消费自身发送的两类延时消息。
7 数据库操作规范（fresh-order-service.sql）
7.1 数据表清单
order_main、order_item、group_record、group_join、after_sale、pay_log
7.2 约束规范
订单、售后、支付记录永久保存，逻辑删除仅用于前端隐藏，不物理删除；
订单号 order_no 唯一索引，防止重复下单；
支付回调日志 pay_log 只新增不修改，作为对账凭证；
拼团记录拆分 group_record（团长）+group_join（参团人），避免单条数据过大；group_join 增加 (group_record_id, user_id) 唯一约束，防止同一用户重复参团；
after_sale 工单关联订单项，AI 识别结果回写 ai_refund_money，管理员审核结果写入 actual_refund_money；
order_item.activity_type 标记库存来源（1普通 2团购 3秒杀），配合 activity_id 关联具体活动记录，取消订单时据此归还对应库存池；
order_main 存储收货地址快照（receiver_name/receiver_phone/community/detail_address），避免用户修改地址影响历史订单；
order_main.integral_used_count 记录使用积分数量，integral_deduct_amount 记录积分抵扣金额；
order_main.seckill_activity_id 关联秒杀活动，便于追溯秒杀订单；order_main.group_activity_id 关联团购活动（原 group_id 已重命名），语义更明确；
order_item.is_commented 标记订单项是否已评价，避免跨库查询；
order_main.status 默认值为 0（待支付），防止漏填导致插入失败；
group_record.group_activity_id、group_record.leader_user_id、group_join.order_id 均建立索引，加速拼团查询；
order_main.group_activity_id、order_main.seckill_activity_id 建立索引，加速活动关联查询；
高并发场景禁止多表长事务，库存、积分操作通过 MQ 异步拆分。
8 核心接口定义
8.1 小程序 C 端接口
POST /order/create 提交订单结算
GET /order/list 查询用户全部订单（待支付 / 待自提 / 已完成 / 售后）
GET /order/{orderNo} 订单详情
POST /group/create 发起拼团
POST /group/join/{groupId} 参与拼团
POST /afterSale/apply 发起售后理赔（上传坏果图片）
GET /afterSale/list 用户售后工单列表
8.2 后台管理接口
GET /admin/order/page 全平台订单分页查询、筛选
GET /admin/group/page 拼团记录管理
GET /admin/afterSale/page 售后工单列表
POST /admin/afterSale/audit 审核售后理赔（通过 / 驳回）
8.3 Feign 对外接口（供 data-service、message-service 调用）
java
运行
@FeignClient("order-service")
public interface OrderFeignClient {
    // 根据订单号查询订单信息
    @GetMapping("/feign/order/{orderNo}")
    Result<OrderMainVO> getOrderByNo(@PathVariable String orderNo);
    // 批量查询订单统计数据
    @PostMapping("/feign/order/batchStat")
    Result<OrderStatVO> getOrderStat(@RequestBody StatQueryDTO dto);
    // 查询售后工单详情
    @GetMapping("/feign/afterSale/{afterSaleId}")
    Result<AfterSaleVO> getAfterSaleById(@PathVariable Long afterSaleId);
}
9 Sentinel 限流熔断规则
下单接口限流：单用户每分钟最多 5 单，防止恶意刷单；
支付回调接口单独隔离限流，保证支付不丢失；
Feign 调用 goods 库存扣减、user 积分发放超时熔断，抛出友好提示；
售后图片上传接口限制单 IP 每分钟 30 次，防止图片恶意上传。



10 Docker 部署配置
Dockerfile
dockerfile



FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/order-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=dev"]




docker-compose.yml 片段
yaml
order-service:
  build: ./order-service
  ports:
    - "8083:8083"
  depends_on:
    - nacos
    - redis
    - rocketmq
    - mysql
    
    
    
    
    
11 跨服务联调规范
库存操作必须 Feign 调用 goods-service，禁止直连商品库；
积分变更通过 Feign 调用 user-service，不操作 fresh_user；
售后图片识别依赖 RocketMQ 异步推送 ai-service，不同步调用；
订单状态变更自动发 MQ，message-service 监听推送通知；
data-service 消费订单行为 MQ，统计销量，不直接查询订单库；
所有退款、积分扣回通过标准化 Feign 接口调用，保证数据一致性。
12 统一异常处理
400：商品库存不足、拼团已解散、订单已支付不可重复提交；
403：非本人订单无法发起售后；
429：下单过于频繁触发限流；
500：支付回调处理失败、MQ 消息发送失败；
熔断降级：查询订单列表返回缓存兜底数据，不阻断页面。
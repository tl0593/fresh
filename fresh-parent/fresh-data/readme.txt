data-service 数据统计服务详细开发文档
1 模块定位与业务职责
1.1 模块定位
平台数据中台微服务，独立数据库 fresh_data，不参与任何交易业务，只负责全链路用户行为埋点采集、定时聚合统计，为后台数据可视化大屏提供指标数据源；全程异步消费 MQ，无同步阻塞业务流程。
1.2 核心业务职责
消费全链路用户行为 MQ 消息，异步写入用户行为原始埋点日志；
定时任务（每日凌晨）聚合订单、商品、用户数据，生成日统计报表；
商品销量按日汇总，支撑热销榜单、商品经营分析；
提供分页、图表统计 Feign 接口，供 Vue3 后台大屏、运营报表调用；
长期冷数据归档策略，按月归档行为日志，减轻主表查询压力；
缓存今日实时统计指标，降低数据库聚合查询压力；
不写入任何业务库，仅读取其他服务 Feign 接口做定时校对。
1.3 依赖中间件
Nacos 注册配置、Redis（实时统计指标缓存）、RocketMQ、MySQL (fresh_data)、MyBatis-Plus、Sentinel、common-core
1.4 依赖远程服务
user-service、goods-service、order-service、ai-service（定时校对统计数据）



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
<!-- Redis缓存实时统计指标 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- RocketMQ消费行为埋点消息 -->
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
<!-- OpenFeign远程调用各业务服务校对数据 -->
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
<!-- 定时任务、lombok、json工具省略 -->




3 Nacos 配置 data-service-dev.yaml
yaml




spring:
  application:
    name: data-service
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
        port: 8727
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:9876
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
# 数据统计专属库 fresh_data
datasource:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://127.0.0.1:3306/fresh_data?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
  username: root
  password: root
# 自定义业务配置
fresh:
  data:
    # 实时指标缓存过期时间 1天
    real-stat-ttl: 86400
    # 每日统计定时任务 凌晨1点执行
    daily-stat-cron: "0 0 1 * * ?"
    # 行为日志归档周期 30天
    archive-day: 30
# MyBatis-Plus全局逻辑删除
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.fresh.data.entity
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
      
      
      
      
      
4 核心业务流程时序
4.1 用户行为埋点采集流程
小程序 / 后台操作（浏览商品、加购、下单、AI 对话）发送 USER_BEHAVIOR_TOPIC；
data-service 持续消费消息，解析用户 ID、行为类型、商品 ID；
批量写入 user_behavior_log 行为原始日志表；
同步更新 Redis 实时今日统计指标（新增用户、浏览量、下单量）。
4.2 每日凌晨自动统计流程
凌晨 1 点定时任务触发；
Feign 批量调用 order、user、goods 服务获取昨日原始业务数据；
聚合计算：新增用户、活跃用户、订单总量、成交金额、成团数、售后量；
写入 daily_stat 日汇总统计表；
按商品维度聚合销量、销售额，写入 goods_sales_stat；
刷新 Redis 全量统计缓存，供大屏快速读取。
4.3 数据大屏指标查询流程
Vue 后台大屏请求统计接口；
优先读取 Redis 缓存的日 / 实时指标；
缓存无数据则查询 daily_stat、goods_sales_stat 统计表；
组装折线、柱状图数据返回前端；
不直接查询订单、商品原始业务表，避免高耗时聚合。
4.4 冷数据归档流程
每日定时任务筛选 30 天前行为日志；
将旧日志迁移至归档备份表，主表删除对应数据；
归档数据仅支持历史报表查询，不参与实时统计。
5 Redis 缓存 Key 设计（data-service 专属）
表格
Key 格式	存储内容	过期时间	使用场景
data:today:stat	当日实时平台总指标 JSON	86400s (1 天)	首页大屏今日数据
data:goods:sales:{date}	指定日期商品销量排行	86400s	商品销量榜单
data:user:active:{date}	每日活跃用户数据	86400s	用户增长图表
6 RocketMQ Topic 收发清单
生产者（data-service）
无业务产出 Topic，仅做数据消费与统计；
消费者（data-service 监听）
USER_BEHAVIOR_TOPIC：用户全行为埋点（浏览、加购、下单、AI 咨询）。
7 数据库操作规范（fresh-data-service.sql）
7.1 数据表清单
user_behavior_log、daily_stat、goods_sales_stat
7.2 约束规范
user_behavior_log 为流水日志，只新增不修改，超过 30 天归档；
daily_stat 以 stat_date 为唯一索引，每日仅生成一条汇总记录；
goods_sales_stat 联合唯一索引 (stat_date,goods_id)，保证单日单品一条统计；
行为日志建立联合索引 idx_user_time (user_id,create_time)，加速用户行为查询；
统计表不做逻辑删除，历史数据永久留存用于经营复盘；
禁止关联查询其他业务库，数据差异通过定时 Feign 校对修正。
8 核心接口定义
8.1 后台大屏接口
GET /stat/today 获取今日实时核心指标
GET /stat/daily/list 多日平台统计（折线图）
GET /stat/goods/sales 商品销量排行
GET /stat/user/trend 用户增长趋势图
GET /stat/group/rate 团购成团率、售后占比
8.2 Feign 对外接口（仅后台权限模块调用）
java
运行
@FeignClient("data-service")
public interface DataFeignClient {
    // 查询指定日期区间成交总额
    @GetMapping("/feign/stat/amount")
    Result<BigDecimal> getRangeAmount(@RequestParam String startDate, @RequestParam String endDate);
    // 查询商品周期销量
    @GetMapping("/feign/goods/sales")
    Result<Integer> getGoodsSales(@RequestParam Long goodsId, @RequestParam String startDate, @RequestParam String endDate);
}
9 Sentinel 限流熔断规则
大屏图表查询接口限流 QPS 200，防止运营人员频繁刷新压库；
MQ 消费批量入库限流，单次最多处理 1000 条埋点消息；
Feign 调用业务服务超时熔断，使用昨日缓存指标兜底展示。



10 Docker 部署配置
Dockerfile
dockerfile


FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/data-service-1.0.0.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=dev"]




docker-compose.yml 片段
yaml



data-service:
  build: ./data-service
  ports:
    - "8086:8086"
  depends_on:
    - nacos
    - redis
    - rocketmq
    - mysql
    
    
    
    
11 跨服务联调规范
所有业务行为统一发 MQ，禁止各服务同步调用 data-service；
统计数据仅后台大屏读取，小程序 C 端不访问该服务；
数据校对仅凌晨定时任务执行，不占用业务高峰；
不直接操作 user/goods/order/ai 库，全部通过 Feign 接口获取数据；
统计指标缓存统一由 data-service 维护，其他服务不读写统计 Redis Key。
12 统一异常处理
400：日期参数格式错误、查询区间过大；
429：大屏高频刷新触发限流；
500：定时任务聚合失败、MQ 消息批量入库异常；
熔断降级：返回昨日缓存统计数据，保证大屏页面正常展示。
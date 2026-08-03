gateway-service 网关微服务详细开发文档
1 模块定位与业务职责
1.1 定位
系统统一流量入口，所有小程序、Vue 后台前端请求全部经过 Gateway，是前后端与微服务之间唯一中转层，无业务数据库读写，仅做路由、鉴权、限流、日志拦截。
1.2 核心职责
全局路由转发：根据请求路径匹配，转发至 user/goods/order/ai/message/data 各个微服务；
跨域统一处理：小程序、PC 后台跨域配置统一在网关，下游服务无需重复处理；
双端鉴权拦截：区分小程序端 Token、后台管理员 Token，校验登录状态、权限；
Redis 分布式限流：基于 IP / 账号 / 接口路径做访问频率限制，拦截刷单、高频爬虫；
IP 黑名单拦截：读取黑名单配置，直接拒绝恶意 IP 请求；
全局请求日志记录：异步保存请求 IP、接口、耗时、用户 ID 至 gateway_access_log；
Sentinel 熔断降级：下游微服务宕机 / 超时自动熔断，返回友好提示；
请求参数清洗、统一返回格式封装。
1.3 依赖中间件
Nacos（注册 + 配置）、Redis（限流、黑名单缓存、Token 校验缓存）、Sentinel、MySQL（gateway 库配置表）
2 技术栈 & Maven 核心依赖
2.1 技术版本
Spring Boot 2.7.x、Spring Cloud Alibaba 2021.0.1.0、Spring Cloud Gateway、Nacos Discovery/Config、Sentinel Gateway、RedisTemplate、MyBatis-Plus（仅读取 gateway 库配置表）
2.2 pom.xml 核心依赖
xml



<!-- Spring Cloud Gateway 网关核心 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<!-- Nacos 注册中心+配置中心 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<!-- Sentinel 网关限流熔断 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
<!-- Redis 限流缓存、黑名单缓存 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- MySQL + MyBatis-Plus 读取限流、黑名单配置表 -->
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
<!-- 工具类 common-core 公共模块 -->
<dependency>
    <groupId>com.fresh</groupId>
    <artifactId>common-core</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- lombok、webflux、fastjson2等基础依赖省略 -->



3 Nacos 完整配置（gateway-service-dev.yaml）
yaml



# 服务基础信息
spring:
  application:
    name: gateway-service
  cloud:
    # Nacos注册配置
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
    # Gateway路由配置
    gateway:
      routes:
        # 用户服务路由
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
        # 商品服务路由
        - id: goods-service
          uri: lb://goods-service
          predicates:
            - Path=/api/goods/**
          filters:
            - StripPrefix=1
        # 订单服务路由
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=1
        # AI智能服务路由
        - id: ai-service
          uri: lb://ai-service
          predicates:
            - Path=/api/ai/**
          filters:
            - StripPrefix=1
        # 消息服务路由
        - id: message-service
          uri: lb://message-service
          predicates:
            - Path=/api/message/**
          filters:
            - StripPrefix=1
        # 数据统计服务路由
        - id: data-service
          uri: lb://data-service
          predicates:
            - Path=/api/data/**
          filters:
            - StripPrefix=1
      # 全局跨域
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins: "*"
            allowed-methods: GET,POST,PUT,DELETE,OPTIONS
            allowed-headers: "*"
            allow-credentials: true
    # Sentinel网关限流
    sentinel:
      transport:
        dashboard: 127.0.0.1:8080
        port: 8719
  # Redis配置
  redis:
    host: 127.0.0.1
    port: 6379
    password:
    database: 0
# MySQL配置（gateway专属库 fresh_gateway）
datasource:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://127.0.0.1:3306/fresh_gateway?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
  username: root
  password: root
# 自定义网关业务配置
fresh:
  gateway:
    # Token请求头key
    token-header: Authorization
    # 限流统计周期 秒
    limit-time-second: 60
    # 日志异步写入开关
    access-log-enable: true
    
    
    
    
4 核心业务流程时序（文字版）
4.1 完整请求处理流程
用户小程序 / Vue 后台发起 HTTP 请求，携带 Token 放入请求头；
Gateway 全局过滤器拦截请求，第一步校验 IP 黑名单：
查询 Redis black:ip:{ip}，存在则直接返回 403 禁止访问；
读取 Redis 限流计数器 limit:api:{path}:{ip}，判断是否超过阈值：
超限直接返回 “访问过于频繁，请稍后再试”；
解析 Header 中的 Token，去 Redis 查询 user:token:{token}：
无数据 / 过期 → 返回 401 未登录，拦截；
存在则解析用户 ID、角色类型（小程序用户 / 管理员）存入上下文；
匹配 Gateway 路由规则，转发请求至对应微服务；
下游微服务执行业务逻辑，返回结果；
Gateway 统一封装返回体（调用 common-core 统一 Result 工具类）；
异步线程记录网关访问日志，插入 gateway_access_log；
Sentinel 监控下游响应耗时、异常，触发熔断则返回降级提示。
4.2 限流规则加载流程
项目启动时，从 fresh_gateway.limit_config 全量读取限流配置；
将配置写入 Redis 缓存，定时 30s 刷新一次数据库规则；
每次请求根据接口路径匹配限流规则，原子递增 Redis 计数器。
5 Redis 缓存设计（网关专属 Key）
5.1 黑名单缓存
Key：black:ip:{ip地址}
Value：1
过期时间：IP 黑名单配置 expire_time 为空则永久，否则设置对应过期时间
读写逻辑：启动加载 DB 黑名单、定时同步 DB 变更，请求前置拦截判断
5.2 接口限流计数器
Key：limit:api:{requestPath}:{clientIp}
Value：访问次数（数字）
过期时间：配置文件 limit-time-second（默认 60s）
读写逻辑：每次请求 INCR，首次设置过期，超过 limit_count 直接拦截
5.3 Token 校验缓存（复用 user-service 写入的 Token）
Key：user:token:{token字符串}
Value：JSON（userId、roleType、openId、adminId）
过期时间：登录时设置（小程序 7 天，后台管理员 2 小时）
读写逻辑：网关仅读取，不写入 / 更新，Token 生成销毁由 user-service 处理
6 RocketMQ 使用说明
网关不生产、不消费任何 MQ 消息，仅做流量转发，无 MQ 相关逻辑。
7 数据库操作规范（对应 fresh_gateway.sql）
7.1 涉及三张表
limit_config：限流规则配置，启动全量加载，定时同步更新；只查不写，新增 / 修改规则由后台管理端调用 user-service 接口操作；
black_ip：IP 黑名单，仅查询缓存，新增拉黑记录由后台操作；
gateway_access_log：请求日志，异步批量插入，禁止同步单条插入，避免 IO 阻塞网关；
7.2 数据库操作约束
网关禁止提供修改限流、黑名单的 CRUD 接口，统一由 user-service 后台权限模块管理；
访问日志采用线程池批量插入，每 50 条或 10s 触发一次入库；
日志表按月归档，历史日志不参与查询。
8 核心过滤器 & 接口定义
8.1 三大全局过滤器
BlackIpFilter：黑名单拦截（最高优先级）
LimitFilter：Redis 分布式限流（次优先级）
AuthTokenFilter：Token 鉴权、用户信息存入上下文
8.2 网关对外无业务接口，仅提供一个健康检查接口
路径：/gateway/health
返回：{"code":200,"msg":"网关服务正常","data":{"service":"gateway-service","time":"2026-07-28"}}
9 Sentinel 熔断 & 限流规则
9.1 熔断规则（下游微服务）
慢调用比例阈值：50%，RT 超过 1000ms 判定慢请求
熔断时长：10s，熔断期所有请求直接降级
最小请求数：10
9.2 网关限流规则（Sentinel 网关流控）
按路由维度限流：单服务 QPS 上限 500
按 IP 限流：单 IP 单接口 1 分钟最多 100 次


10 Docker 部署配置 Dockerfile
dockerfile


FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/gateway-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=dev"]


docker-compose.yml 片段
yaml


gateway-service:
  build: ./gateway-service
  ports:
    - "8080:8080"
  depends_on:
    - nacos
    - redis
    - mysql
  environment:
    SPRING_PROFILES_ACTIVE: dev
    
    
11 与其他微服务联调规范
网关只转发请求，不 Feign 调用任何下游服务；
用户登录、Token 生成、权限校验数据源头全部在 user-service；
限流、黑名单配置的 CRUD 接口统一放在 user-service 后台权限模块；
下游服务无需处理跨域、Token 解析，直接从请求上下文获取 userId。
12 异常统一处理
401：Token 不存在 / 过期 / 非法 → 统一返回：{"code":401,"msg":"登录已失效，请重新登录","data":null}
403：IP 黑名单、无访问权限 → {"code":403,"msg":"禁止访问","data":null}
429：限流超限 → {"code":429,"msg":"访问过于频繁，请稍后重试","data":null}
503：下游服务熔断降级 → {"code":503,"msg":"服务器繁忙，请稍后再试","data":null}
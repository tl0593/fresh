user-service 用户服务详细开发文档
1 模块定位与业务职责
1.1 模块定位
全系统用户权限核心服务，独立库 fresh_user，负责两类主体：小程序 C 端用户、平台管理员；提供 RBAC 权限、登录鉴权、购物车、收货地址、会员积分全链路能力。
1.2 核心业务职责
微信小程序授权登录、手机号验证码登录、Token 签发与失效；
后台管理员账号 CRUD、角色、菜单 RBAC 权限管控；
用户收货地址管理；
用户购物车 CRUD（Redis 为主、数据库兜底持久化）；
会员积分发放、扣减、积分流水记录；
后台限流黑名单、网关规则配置管理（提供 CRUD 接口，网关仅读取）；
对外 Feign 接口，给订单、商品、AI 服务提供用户基础信息查询；
发送用户相关 RocketMQ 消息（积分变更、注册通知）。
1.3 依赖中间件
Nacos 注册配置、Redis（会话、购物车、验证码、积分缓存）、RocketMQ、MySQL (fresh_user)、MyBatis-Plus、Sentinel、common-core 公共模块


2 技术栈 & Maven 核心依赖
xml



<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Nacos 注册+配置 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<!-- Sentinel 熔断限流 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
<!-- Redis缓存 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- RocketMQ消息队列 -->
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
<!-- Feign远程调用 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<!-- 公共工具模块 -->
<dependency>
    <groupId>com.fresh</groupId>
    <artifactId>common-core</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- lombok、json、微信工具包省略 -->



3 Nacos 配置 user-service-dev.yaml
yaml



spring:
  application:
    name: user-service
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
        port: 8722
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:9876
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
# 数据库 fresh_user
datasource:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://127.0.0.1:3306/fresh_user?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
  username: root
  password: root
# 自定义业务配置
fresh:
  user:
    # 小程序token有效期7天，单位秒
    mini-token-expire: 604800
    # 后台管理员token 2小时
    admin-token-expire: 7200
    # 验证码有效期5分钟
    code-expire: 300
    # 购物车定时同步DB间隔（分钟）
    cart-sync-cron: "0 */5 * * * ?"
# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.fresh.user.entity
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
      
      
4 核心业务流程时序
4.1 小程序微信一键登录
前端传递微信 code 调用 /api/user/mini/login；
后端请求微信开放平台接口，获取 openid；
查询app_user表：
无记录 → 自动新建小程序用户；
存在记录 → 读取用户积分、基础信息；
生成唯一 Token，存入 Redis user:token:{token}，设置 7 天过期；
返回 Token、用户昵称、头像、积分给小程序；
发送用户注册 MQ 消息，data-service 消费记录用户行为。
4.2 购物车读写流程（Redis 优先）
查询购物车：直接读取 Redis user:cart:{userId} JSON；无数据再查 DB 回填缓存；
新增 / 修改商品数量：先更新 Redis，5 分钟定时任务批量同步至user_cart表兜底；
结算清空选中商品：删除 Redis 内选中条目，同步 DB；
用户退出登录不删除购物车缓存，过期自动失效。
4.3 订单积分冻结与结算流程
用户下单使用积分时：order-service Feign 调用积分预扣接口，增加 app_user.frozen_integral；
订单超时取消：order-service 发送取消消息，user-service 消费后返还冻结积分至 app_user.integral；
订单完成支付：order-service 发送【订单完成】Topic 消息；
user-service 消费消息，将 frozen_integral 转为已扣减，计算下单积分奖励；
更新 app_user.integral 总积分（增加奖励），插入 user_integral_log 流水；
发送积分变动消息，message-service 推送小程序通知。
4.4 后台 RBAC 权限校验流程
管理员登录生成 Admin Token 存入 Redis；
后台请求携带 Token，网关鉴权后透传 userId；
接口内部根据 userId 查询角色、角色绑定菜单权限；
无对应权限直接返回 403。
5 Redis 缓存 Key 设计（user-service 专属）
表格
Key 格式	存储内容	过期时间	使用场景
user:token:{token}	JSON(userId,openid,roleType)	小程序 7d / 管理员 2h	登录会话校验
user:cart:{userId}	购物车商品数组 JSON	永久，主动更新	购物车高速读写
user:code:{phone}	短信验证码字符串	5min	登录 / 绑定手机验证码
user:integral:hot:{userId}	用户当前积分	1h，积分变动主动更新	个人中心积分查询缓存
user:role:menu:{adminId}	管理员菜单权限列表	30min，修改角色后清除	后台权限校验缓存
6 RocketMQ Topic 收发清单
生产者（user-service 发送）
USER_REGISTER_TOPIC：用户新注册，推送行为埋点给 data-service；
INTEGRAL_CHANGE_TOPIC：积分增减，推送消息通知服务；
消费者（user-service 监听）
ORDER_SUCCESS_TOPIC：消费订单完成消息，发放下单积分；
AFTER_SALE_REFUND_TOPIC：售后退款，扣回对应积分。
7 数据库操作规范（对应 fresh-user-service.sql）
7.1 全部数据表清单
sys_admin、sys_role、sys_menu、sys_role_menu、app_user、user_address、user_cart、user_integral_log
7.2 操作约束
所有删除操作执行逻辑删除 del_flag=1，禁止 DELETE；
user_cart：Redis 为主，DB 仅做数据兜底，定时任务批量同步，不实时写库；user_cart 增加 (user_id, goods_id, spec_id) 唯一约束，防止同规格重复加购；
user_integral_log 积分流水只新增不修改，用于对账追溯；
角色、菜单修改后，主动清除对应 Redis 权限缓存，避免脏数据；
小程序用户 openid 唯一索引，防止重复创建账号；
user_address.user_id、user_cart.user_id、sys_menu.parent_id、user_integral_log.user_id、integral_exchange_log.template_id 均建立索引，加速联表查询。
8 核心接口定义（RESTful）
8.1 小程序 C 端接口
POST /mini/login 微信一键登录
请求：{code:"微信临时code"}
响应：{token:"xxx",userInfo:{nickName,avatar,integral}}
POST /mini/bindPhone 绑定手机号 + 验证码校验
GET /address/list 查询用户自提地址
POST /address/save 新增 / 编辑地址
GET /cart/list 查询购物车
POST /cart/update 修改购物车商品数量 / 选中状态
GET /integral/log 查询积分流水
8.2 后台管理接口
POST /admin/login 管理员账号密码登录
GET /admin/list 管理员账号列表
CRUD /role/** 角色管理
CRUD /menu/** 菜单权限
GET /mini/user/list 小程序用户分页查询
CRUD /gateway/limit 网关限流规则配置
CRUD /gateway/blackIp IP 黑名单管理


8.3 Feign 对外提供接口（供其他服务调用）
java


@FeignClient("user-service")
public interface UserFeignClient {
    // 根据用户ID查询小程序用户基础信息
    @GetMapping("/feign/user/{userId}")
    Result<AppUserVO> getUserById(@PathVariable Long userId);
    // 查询用户收货地址
    @GetMapping("/feign/address/{addressId}")
    Result<UserAddressVO> getAddressById(@PathVariable Long addressId);
    // 积分预扣（下单冻结积分）
    @PostMapping("/feign/integral/freeze")
    Result<Void> freezeIntegral(@RequestBody IntegralDTO dto);
    // 积分返还（取消订单解冻）
    @PostMapping("/feign/integral/unfreeze")
    Result<Void> unfreezeIntegral(@RequestBody IntegralDTO dto);
    // 积分实际扣减或增加（订单完成结算）
    @PostMapping("/feign/integral/change")
    Result<Void> changeIntegral(@RequestBody IntegralDTO dto);
}
9 Sentinel 限流熔断规则
登录接口限流：单 IP 1 分钟最多 10 次请求，防止暴力刷验证码；
Feign 调用降级：订单 / AI 服务远程查询用户超时，返回空用户信息，不阻断主流程；
批量查询用户列表 QPS 阈值 200，超出触发限流。


10 Docker 部署配置
Dockerfile
dockerfile



FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/user-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=dev"]



docker-compose 片段
yaml
user-service:
  build: ./user-service
  ports:
    - "8081:8081"
  depends_on:
    - nacos
    - redis
    - rocketmq
    - mysql
    
    
11 跨服务联调规范
对外提供 Feign 接口给 order-service、ai-service、message-service；
购物车、Token 数据仅本服务写入，其他服务只读；
网关只读取 Redis Token 缓存，不能操作用户库；
积分变更必须通过 Feign 调用，禁止其他服务直接操作 fresh_user 库；
用户行为数据通过 RocketMQ 异步发送，不主动调用 data-service。
12 统一异常处理
400 参数错误：手机号格式错误、验证码过期；
401 后台登录密码错误、Token 失效；
403 无菜单访问权限；
429 登录接口请求频繁；
500 数据库异常、微信授权接口调用失败。
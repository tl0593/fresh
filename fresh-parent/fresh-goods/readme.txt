goods-service 商品服务完整开发文档
（已集成商品评价、优惠券、满减、积分兑换券、整点抢券、积分抽奖奖品全套营销模块，多数据源：fresh_goods + fresh_comment + fresh_promotion）
1 模块定位与业务职责
1.1 模块定位
生鲜商品核心微服务，项目流量最高模块；采用多数据源架构，承载三大数据库：
fresh_goods：商品、分类、规格、团购、秒杀基础业务
fresh_comment：商品评价、评价图片、管理员回复
fresh_promotion：优惠券、满减活动、积分兑券、整点抢券、抽奖奖品池
重度依赖 Redis 缓存、RocketMQ 异步解耦，统一管理商品全生命周期 + 完整营销活动 + 用户口碑评价体系；所有优惠、评价数据仅本服务对外提供接口，其他微服务禁止直连三张业务库。
1.2 核心业务职责
基础商品业务
生鲜多级分类树维护、分类持久缓存；
商品、规格、轮播图新增 / 编辑 / 上下架；
库存分层隔离：普通商品库存、团购专属库存、秒杀预热库存；
Redis 分布式锁实现秒杀高并发防超卖；
社区团购活动创建，远程调用 AI 服务生成宣传文案；
定时任务刷新首页热销、团购榜单缓存。
评价口碑模块（新增）
用户图文评价、星级打分、多图上传存储；
商品评价分页查询、平均星级 / 好评率统计缓存；
管理员后台隐藏差评、评价回复功能；
新增评价发送 MQ 通知订单、消息、数据服务。
完整营销体系（优惠券 / 满减 / 积分活动完整版）
优惠券模板管理：无门槛券、满减券、品类券、团购专用券；
用户领券、单人限领、总库存防超领；
全场 / 分类满减活动，支持配置是否与优惠券叠加；
积分兑换优惠券配置、库存管控、单人每日兑换上限；
整点限时抢券，Redis Lua 原子脚本防超领、防重复领取；
积分抽奖奖品池配置，提供权重随机开奖能力给用户服务；
所有领券、兑券、抢券统一生成用户优惠券记录；
每日定时清理过期优惠券、清理活动缓存。
公共能力
数据更新自动清理对应 Redis 热点缓存，保障缓存一致性；
对外统一 Feign 接口供给订单、用户、AI、消息、数据服务；
库存变更、团购过期、新增评价、领券事件异步发送 RocketMQ。
1.3 依赖中间件
Nacos 注册配置中心、Redis（缓存 + 分布式锁 + 活动库存）、RocketMQ、MySQL 多数据源、MyBatis-Plus、Sentinel 流量控制、common-core 公共模块
1.4 依赖远程服务
order-service：校验订单是否可评价、结算查询商品信息
ai-service：团购活动 AI 文案生成
user-service：积分兑换、积分抽奖远程交互


2 技术栈 & Maven 核心依赖
xml



<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Nacos 注册+配置中心 -->
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
<!-- Redis缓存、分布式锁、Lua脚本执行 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- RocketMQ消息队列 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-rocketmq</artifactId>
</dependency>
<!-- MySQL多数据源 + MyBatis-Plus -->
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
<!-- OpenFeign远程调用 -->
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
<!-- OSS文件上传、lombok、fastjson2、工具包省略 -->


3 Nacos 配置 goods-service-dev.yaml
yaml




spring:
  application:
    name: goods-service
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
        port: 8723
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:9876
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
# 三数据源配置：商品主库、评价库、营销优惠库
datasource:
  primary:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/fresh_goods?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
  comment:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/fresh_comment?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
  promotion:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/fresh_promotion?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
# 自定义业务配置
fresh:
  goods:
    # 商品基础缓存过期时间 30分钟
    goods-cache-ttl: 1800
    # 分类树永久缓存，修改主动清key
    category-cache-ttl: -1
    # 秒杀分布式锁参数
    lock-wait-time: 3
    lock-hold-time: 10
    # 热销榜单刷新定时 每10分钟
    hot-list-cron: "0 */10 * * * ?"
    # 评价列表缓存30分钟
    comment-cache-ttl: 1800
    # 商品好评率缓存刷新 每15分钟
    comment-rate-cron: "0 */15 * * * ?"
    # 优惠券缓存配置
    coupon-template-ttl: 900
    user-coupon-ttl: 600
    # 每日凌晨清理过期优惠券、统计活动数据
    promotion-cron: "0 0 1 * * ?"
    # 每小时59分预热下一小时整点抢券库存
    seckill-coupon-cron: "0 59 * * * ?"
# MyBatis-Plus全局逻辑删除
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.fresh.goods.entity
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
      
      
      
      
4 核心业务流程时序
4.1 首页商品查询（原有不变）
小程序请求首页爆款 / 分类商品；
优先读取 Redis 缓存，命中直接返回；
未命中查询 fresh_goods 库，组装数据写入缓存；
定时任务自动刷新热销榜单。
4.2 秒杀防超卖（原有不变）
下单时 order-service Feign 调用库存扣减接口；
获取 Redis 分布式锁，校验预缓存库存；
Redis 原子扣减库存，发送 MQ 异步同步数据库真实库存；
释放分布式锁，返回库存扣减结果。
4.3 团购活动生命周期（原有不变）
后台创建团购，Feign 调用 ai-service 生成宣传文案；
活动到期发送延时 MQ；
order-service 自动解散拼团、归还库存；
本服务监听团购过期消息，更新活动状态、清理团购缓存。
4.4 商品更新缓存清理（原有不变）
商品 / 分类 / 团购修改、上下架后，批量删除对应 Redis 热点 key，实现缓存主动失效。
4.5 用户提交图文评价（评价模块）
小程序传入订单项 ID、星级、文字、图片数组；
Feign 调用 order-service checkCanComment 校验：订单已完成、未评价；
多数据源事务操作 fresh_comment：插入评价主表、批量插入评价图片；
发送COMMENT_ADD_TOPIC消息；
删除商品评价、好评率 Redis 缓存；
返回评价成功。
4.6 商品详情加载评价（评价模块）
根据 goodsId 查询 Redis 评价缓存；
无缓存关联查询 goods_comment、comment_image、comment_reply；
过滤正常展示评价，组装图文与管理员回复写入 Redis；
返回评价列表、平均星级、好评率。
4.7 管理员回复评价（评价模块）
后台传入评价 ID、回复内容、管理员 ID；
插入 comment_reply 回复表；
清理该商品评价 Redis 缓存。
4.8 用户普通领取优惠券（营销模块）
小程序传入 templateId、userId；
Redis 分布式锁控制并发领券；
校验活动进行中、总库存充足、单人未达领取上限；
DB 新增 user_coupon、template 已领数量 + 1；
更新 Redis 券库存、用户领券限制；
发送COUPON_RECEIVE_TOPIC消息。
4.9 整点限时抢券（营销高并发）
每小时 59 分定时任务预热下一小时抢券库存到 Redis；
用户发起抢请求；
执行 Lua 脚本原子校验：活动状态、剩余库存、用户今日是否已抢；
Redis 扣减库存成功，发送 MQ 异步生成用户优惠券；
前端直接返回抢购结果。
4.10 积分兑换优惠券（营销 + user 联动）
user-service Feign 发起兑换请求；
校验积分兑换券总库存、用户每日兑换上限；
生成 user_coupon 兑换券记录；
更新 integral_coupon 已兑换数量；
清理兑换券缓存，发送领券 MQ 消息。
4.11 提供抽奖奖品池（营销 + user 联动）
user-service 抽奖时远程调用；
查询全部启用的抽奖奖品；
根据权重算法随机返回奖品信息给用户服务。
5 Redis 缓存 Key 完整设计
表格
Key 格式	存储内容	过期时间	业务用途
goods:category:tree	全部分类树 JSON	永久	首页分类导航
goods:detail:{goodsId}	商品基础 + 规格信息	1800s	商品详情基础模块
goods:hot:list	首页热销生鲜榜单	600s	首页推荐
group:hot:list	进行中团购列表	600s	团购专区
goods:seckill:stock:{seckillId}	秒杀可售库存	活动结束删除	秒杀防超卖
lock:seckill:{seckillId}	秒杀分布式锁	10s	并发控库存
goods:spec:{specId}	商品规格价格库存	1800s	下单规格校验
goods:comment:{goodsId}	商品分页评价（含图片、管理员回复）	1800s	商品详情评价 Tab
goods:score:{goodsId}	商品平均星级、好评率、总评价数	900s	榜单、头部星级展示
coupon:stock:{templateId}	普通优惠券剩余库存	活动结束删除	领券防超领
coupon:user:limit:{userId}:{templateId}	用户单券已领数量	活动周期	单人领券上限控制
coupon:user:valid:{userId}	用户全部可用优惠券列表	600s	个人优惠券页面
promotion:fullreduce:list	生效满减活动缓存	900s	结算优惠计算
integral:coupon:stock:{id}	积分兑换券库存	永久	积分商城
seckill:coupon:stock:{actId}	整点抢券库存	当日过期	限时抢活动
seckill:coupon:user:{actId}:{userId}	用户当日已抢标记	当日过期	限制单人每日抢券
6 RocketMQ Topic 收发清单
生产者（goods-service 发送）
STOCK_CHANGE_TOPIC：库存变动，同步 DB 真实库存；
GROUP_ACTIVITY_EXPIRE_TOPIC：团购到期延时消息；
GOODS_BEHAVIOR_TOPIC：商品曝光、点击埋点；
COMMENT_ADD_TOPIC【评价】：用户新增评价事件；
COUPON_RECEIVE_TOPIC【营销】：领券 / 积分兑券 / 整点抢券成功统一消息。
消费者（goods-service 监听）
ORDER_CANCEL_STOCK_TOPIC：订单取消归还库存；
GROUP_EXPIRE_STATUS_TOPIC：团购过期更新活动状态；
无评价、优惠券消费逻辑，对应消息由 order/message/data-service 消费。
7 数据库操作规范
7.1 三大数据库完整表清单
fresh_goods：goods_category、goods、goods_spec、goods_image、group_activity、seckill_activity
fresh_comment：goods_comment、comment_image、comment_reply
fresh_promotion：coupon_template、user_coupon、full_reduce_activity、coupon_use_log、integral_coupon、seckill_coupon、integral_lottery_prize
7.2 通用约束
所有业务表统一 del_flag 逻辑删除，禁止物理 DELETE；
商品上下架仅修改 status 字段，不删除数据；
秒杀、团购、优惠券采用 Redis 预操作 + MQ 异步落库，保障高并发；
商品定价规则：goods.sale_price 为展示基准价，goods_spec.spec_price 为实际交易价格；无规格商品使用 goods.sale_price，多规格商品以 goods_spec 价格为准；
goods_spec.is_default 标记默认规格，前端商品详情默认展示此规格价格；
group_activity 和 seckill_activity 均关联 goods_spec（spec_id），支持同一商品不同规格独立设置活动价格和库存；
integral_lottery_prize 奖品记录拆分 reward_integral（积分奖品数量）和 reward_coupon_id（优惠券奖品ID），避免 reward_id 多义；
goods.cat_id、goods_spec.goods_id、goods_image.goods_id、group_activity.goods_id、seckill_activity.goods_id 均建立索引，加速商品联表查询；
7.3 fresh_comment 评价库约束
评价三张表仅 goods-service 读写，其他服务禁止直连；
评价图片仅存 OSS 访问地址，不存储二进制；
status 字段控制前端是否展示，差评可后台隐藏；
管理员回复只新增不修改，永久留存；
7.4 fresh_promotion 营销库约束
所有优惠券、活动库存变更优先 Redis，MQ 异步同步 DB；
user_coupon 记录用户持券，use_status 区分未使用 / 已核销 / 过期；
integral_coupon、seckill_coupon 独立库存，和普通优惠券隔离；
抽奖奖品权重配置，用于随机开奖逻辑；
coupon_use_log 建立 user_id、user_coupon_id、template_id 索引，加速核销查询与统计；
8 核心接口定义
8.1 小程序 C 端接口
基础商品
GET /category/tree 获取全部分类树
GET /goods/hot 首页热销商品
GET /goods/list 分类选购商品分页（catId/keyword/sortType=sale|price/sortOrder/pageNum/pageSize）
GET /goods/{goodsId} 商品基础详情
GET /group/list 团购专区列表
GET /seckill/list 限时秒杀列表
评价模块
POST /comment/submit 提交图文评价
GET /comment/list/{goodsId} 获取商品评价分页
GET /comment/user/list 当前用户全部历史评价
营销优惠券
GET /coupon/template/list 首页可领优惠券
POST /coupon/receive 领取普通优惠券
GET /coupon/seckill/list 整点抢券活动列表
GET /integral/coupon/list 积分商城可兑换券
8.2 后台管理接口
基础商品
CRUD /category/** 分类管理
CRUD /goods/** 商品增删改上下架
CRUD /group/** 团购活动配置
CRUD /seckill/** 秒杀配置
评价管理
GET /admin/comment/page 全平台评价分页
PUT /admin/comment/hide/{commentId} 隐藏评价
POST /admin/comment/reply 管理员回复评价
营销活动
CRUD /admin/coupon 优惠券模板管理
CRUD /admin/fullreduce 满减活动配置
CRUD /admin/integralCoupon 积分兑换券配置
CRUD /admin/seckillCoupon 整点抢券活动
CRUD /admin/lotteryPrize 积分抽奖奖品池
GET /admin/coupon/log 优惠券领取 & 核销记录
8.3 Feign 对外接口
java
运行
@FeignClient("goods-service")
public interface GoodsFeignClient {
    // 原有库存、商品批量接口省略

    // 评价相关
    @GetMapping("/feign/comment/rate/{goodsId}")
    Result<CommentRateVO> getGoodsCommentRate(@PathVariable Long goodsId);

    // 积分兑换优惠券
    @PostMapping("/feign/integral/exchange")
    Result<Void> exchangeCoupon(@RequestBody IntegralExchangeDTO dto);

    // 获取抽奖奖品池
    @GetMapping("/feign/lottery/prize")
    Result<List<LotteryPrizeVO>> getLotteryPrize();

    // 整点抢券发放
    @PostMapping("/feign/seckill/coupon/receive")
    Result<Void> seckillReceive(@RequestBody SeckillCouponDTO dto);

    // 结算查询可用优惠（优惠券+满减）
    @PostMapping("/feign/promotion/calc")
    Result<PromotionCalcVO> getAvailablePromotion(@RequestBody PromotionQueryDTO dto);
}
9 Sentinel 限流熔断规则
商品详情查询 QPS 上限 800，首页列表 500；
秒杀下单单 IP 每分钟最多 20 次；
评价提交单用户每分钟最多 5 次，防灌水；
后台评价批量查询 QPS 限制 200；
整点抢券单 IP 每分钟 15 次；
积分兑换接口单用户每分钟 10 次；
积分抽奖接口单用户每分钟 20 次；
Feign 远程调用超时统一熔断，返回空兜底数据。


10 Docker 部署配置
Dockerfile
dockerfile



FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/goods-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=dev"]



docker-compose 片段
yaml



goods-service:
  build: ./backend/goods-service
  ports:
    - "8082:8082"
  depends_on:
    - nacos
    - redis
    - rocketmq
    - mysql
    
    
部署补充说明
MySQL 容器初始化依次执行：02-fresh-goods-service.sql、09-fresh-comment.sql、09-fresh-promotion.sql；
服务启动自动加载三数据源配置，无需新增容器；
内置定时任务自动执行榜单刷新、优惠券清理、抢券库存预热；
打包、构建镜像逻辑无改动，仅初始化 SQL 增加营销、评价脚本。
11 跨服务联调规范
库存扣减 / 归还必须 Feign 调用，禁止跨库直连；
评价、优惠券、积分兑券所有读写仅由 goods-service 提供接口，其他服务禁止直连 fresh_comment、fresh_promotion；
新增评价COMMENT_ADD_TOPIC下游消费分工：
order-service：更新订单项已评价标记
message-service：推送管理员新评价通知
data-service：采集口碑统计数据
领券统一发送COUPON_RECEIVE_TOPIC，message 推送用户通知，data 统计发放核销；
积分兑换、抽奖由 user-service 主动 Feign 调用 goods 服务，库存校验逻辑统一在商品服务；
AI 文案生成同步 Feign 调用 ai-service；
商品、评价、营销缓存仅 goods-service 维护，外部服务只读不写 Redis。
12 统一异常处理
400：商品不存在、规格库存不足、活动未开始 / 已结束；
400 评价相关：订单不可评价、重复提交、图片格式非法；
400 营销相关：优惠券库存耗尽、单人领取达到上限、积分兑换券已售罄；
429：秒杀、评价、抢券、抽奖接口访问频繁触发限流；
500：Redis 分布式锁获取失败、多数据源事务异常、Lua 脚本执行异常；
熔断降级：商品列表、评价列表、优惠券列表返回空兜底数据，页面不崩溃。
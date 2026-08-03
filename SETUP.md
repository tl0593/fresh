# Fresh 生鲜团购 — 本地开发指南

> 准备上传 GitHub？先看 [docs/GITHUB-CHECKLIST.md](docs/GITHUB-CHECKLIST.md)。

## 1. 环境要求

- JDK 17+
- Maven 3.8+
- Docker Desktop（推荐，一键启动中间件）
- 可选：本地 MySQL 8.0 + Redis 7

## 2. 数据库初始化

### 方式 A：Docker 自动初始化（推荐）

```bash
cd fresh-parent
docker compose up -d mysql redis nacos
```

首次启动 MySQL 容器会自动执行 `SQL/` 目录下全部脚本（挂载到 `/docker-entrypoint-initdb.d`）。

- MySQL 端口：`3307`（容器内 3306）
- 账号/密码：`root` / `root`

### 方式 B：手动导入

Windows 双击运行项目根目录 `init-db.bat`，或：

```bash
mysql -h127.0.0.1 -P3307 -uroot -proot < SQL/00-nacos-init.sql
# ... 依次执行 01~10
```

## 3. 各服务数据库与端口

| 服务 | 端口 | 数据库 |
|------|------|--------|
| fresh-gateway | 8080 | fresh_gateway |
| fresh-user | 8081 | fresh_user |
| fresh-goods | 8082 | fresh_goods |
| fresh-order | 8083 | fresh_order |
| fresh-ai | 8084 | fresh_ai |
| fresh-message | 8085 | fresh_message |
| fresh-data | 8086 | fresh_data |

本地开发配置在各自模块的 `application-dev.properties`：
- MySQL: `127.0.0.1:3307`，用户 `root`，密码 `root`
- Redis: `127.0.0.1:6380`
- Nacos: `127.0.0.1:8848`

## 4. 编译与启动

```bash
cd fresh-parent
mvn clean package -DskipTests
```

按依赖顺序启动（需先启动 Redis、MySQL、Nacos）：

1. fresh-user
2. fresh-gateway
3. fresh-goods、fresh-order
4. fresh-ai、fresh-message、fresh-data

或使用 Docker 一键启动全部：

```bash
cd fresh-parent
docker compose up -d --build
```

## 5. 启动微服务

**仅启动中间件不够**，8080 网关需要单独启动微服务：

```powershell
# 方式 A：Docker 启动全部微服务（需先 mvn package）
cd fresh-parent
mvn clean package -DskipTests
docker compose up -d fresh-user fresh-gateway
# 或一键：docker compose up -d --build

# 方式 B：本地 IDEA / Maven 启动（开发推荐）
# 终端1：fresh-user（8081）
cd fresh-parent\fresh-user
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 终端2：fresh-gateway（8080）
cd fresh-parent\fresh-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 6. 快速验证（PowerShell）

PowerShell 中 `curl` 是 `Invoke-WebRequest` 的别名，请用以下写法：

```powershell
# 网关健康检查
Invoke-RestMethod http://localhost:8080/gateway/health

# 小程序登录
Invoke-RestMethod -Method POST http://localhost:8080/api/user/mini/login `
  -ContentType "application/json" `
  -Body '{"code":"test001"}'
```

也可使用真正的 curl.exe：

```powershell
curl.exe http://localhost:8080/gateway/health
curl.exe -X POST http://localhost:8080/api/user/mini/login -H "Content-Type: application/json" -d "{\"code\":\"test001\"}"
```

## 7. 已实现的核心能力

- **common-core**：统一返回 Result、异常、常量、Redis/JSON 工具、`WechatUtil`
- **user**：微信登录（mock / 真实 code2session）、管理员登录、地址/购物车/积分、Feign 接口
- **gateway**：路由转发、Token 鉴权、IP 黑名单、Sentinel 网关限流
- **goods**：分类树、商品详情、热销、团购/秒杀、评价/优惠券（多数据源）、OSS 图片上传
- **order**：下单、预支付、支付回调、售后申请、RocketMQ 超时取消
- **ai**：AI 客服对话（知识库匹配 + 模拟回复）、知识库 CRUD
- **message**：站内消息、RocketMQ 消费业务通知
- **data**：今日统计、日汇总、商品销量排行、行为埋点消费

## 8. 已扩展能力与接口说明

### 8.1 RocketMQ 异步消息全链路

| Topic | 生产者 | 消费者 | 用途 |
|-------|--------|--------|------|
| `ORDER_CREATE_TOPIC` | order | message | 下单通知 |
| `ORDER_UNPAID_TOPIC` | order（延时 30min） | order、message | 超时取消 / 通知 |
| `ORDER_SUCCESS_TOPIC` | order | user、message | 支付成功发积分 / 通知 |
| `AFTER_SALE_IMAGE_TOPIC` | order | ai | 售后图片识别 |
| `COMMENT_ADD_TOPIC` | goods | order、message、data | 评价联动 |
| `COUPON_RECEIVE_TOPIC` | goods | message、data | 领券通知 / 统计 |
| `USER_REGISTER_TOPIC` | user | data | 注册埋点 |
| `INTEGRAL_CHANGE_TOPIC` | user | message | 积分变动通知 |

本地 NameServer：`127.0.0.1:9876`（Docker 宿主机映射多为 `9877:9876`，以 `docker-compose.yml` 为准）。

### 8.2 商品评价 / 营销优惠券（多数据源）

`fresh-goods` 连接三库：`fresh_goods` + `fresh_comment` + `fresh_promotion`。

| 方法 | 路径（经网关加前缀 `/api/goods`） | 说明 |
|------|----------------------------------|------|
| POST | `/comment/submit` | 提交图文评价 |
| GET | `/comment/list/{goodsId}` | 商品评价分页 |
| GET | `/comment/user/list` | 我的评价 |
| GET | `/coupon/template/list` | 可领优惠券 |
| POST | `/coupon/receive` | 领取普通券 |
| GET | `/coupon/seckill/list` | 整点抢券列表 |
| GET | `/integral/coupon/list` | 积分兑券列表 |
| POST | `/upload/image` | 图片上传（multipart `file`） |

### 8.3 微信支付 / Sentinel / OSS / 微信登录

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/mini/login` | 微信登录；`fresh.wechat.mock-enabled=true` 时 openid=`wx_{code}` |
| POST | `/api/order/order/pay/prepay` | 创建预支付参数（body: `{"orderNo":"..."}`） |
| POST | `/api/order/order/pay/callback` | 模拟/开发支付成功回调 |
| POST | `/api/order/order/pay/notify` | 支付回调（与 callback 等价，mock 可用） |
| POST | `/api/goods/upload/image` | OSS/本地图片上传 |

关键配置（`application-dev.properties`）：

```properties
# 登录 mock（默认开）
fresh.wechat.mock-enabled=true
fresh.wechat.app-id=
fresh.wechat.app-secret=

# 支付 mock（默认开）
fresh.order.wx-pay-mock=true

# 图片本地存储（默认）
fresh.goods.oss.enabled=false
fresh.goods.oss.local-path=./uploads
```

### 8.4 微信预支付本地联调（推荐开发阶段）

> 完整支付开发清单（测试号、商户号、分阶段任务）见：[docs/PAYMENT-DEV.md](docs/PAYMENT-DEV.md)

**不需要微信支付沙箱也能先验证业务链路。** 当前默认 `wx-pay-mock=true`：后端返回模拟 `prepayId`/`paySign`，再用 callback 把订单置为已支付。

```powershell
# 前提：已登录拿到 token；已创建待支付订单，记下 orderNo

# 1）预支付（mock）
Invoke-RestMethod -Method POST http://localhost:8080/api/order/order/pay/prepay `
  -ContentType "application/json" `
  -Headers @{ Authorization = "你的token" } `
  -Body '{"orderNo":"你的订单号"}'

# 2）模拟支付成功回调（无需真实微信）
Invoke-RestMethod -Method POST http://localhost:8080/api/order/order/pay/callback `
  -ContentType "application/json" `
  -Body '{"orderNo":"你的订单号","transactionId":"mock_tx_001"}'

# 3）查订单状态（应为待自提 status=1）
Invoke-RestMethod http://localhost:8080/api/order/order/你的订单号 `
  -Headers @{ Authorization = "你的token" }
```

也可直连 order 服务（绕过网关）：`http://localhost:8083/order/pay/prepay`。

### 8.5 小程序未完成时怎么开发（仅有测试号）

推荐分三层推进，**先后端 mock，再测试号联调，最后商户号真支付**：

| 阶段 | 做什么 | 是否需要沙箱/商户号 |
|------|--------|---------------------|
| A. 纯后端 | mock 登录 + mock 预支付 + callback | 不需要 |
| B. 测试号联调登录 | 配真实 AppId/Secret，关 mock，用开发者工具拿真实 code | 需要小程序测试号，不需要支付商户号 |
| C. 真支付 | 开通微信支付商户号，关 `wx-pay-mock`，接 JSAPI | 需要商户号；微信已弱化旧「支付沙箱」，一般用真实小额 / 体验版验证 |

**阶段 A（现在就能做）**

1. 保持 `fresh.wechat.mock-enabled=true`、`fresh.order.wx-pay-mock=true`。
2. 用 Postman / PowerShell / Apifox 测登录、下单、预支付、回调。
3. 小程序模板可先写死：登录传任意 `code`，支付成功页直接调 `/order/pay/callback`（仅开发包，正式环境禁止）。

**阶段 B（测试号换真实 openid）**

1. 微信公众平台 → 小程序测试号 / 正式号，拿到 AppId、AppSecret。
2. 微信开发者工具打开商城模板，详情里填 AppId，勾选「不校验合法域名」（开发阶段）。
3. 后端配置：

```properties
fresh.wechat.mock-enabled=false
fresh.wechat.app-id=你的AppId
fresh.wechat.app-secret=你的AppSecret
```

4. 小程序调用 `wx.login` → 把 `code` 发给 `/api/user/mini/login` → 后端换 openid。
5. 开发者工具可预览；真机调试需在公众平台配置 request 合法域名（可用内网穿透如 ngrok / 花生壳，或临时关域名校验）。

**阶段 C（真微信支付，小程序做好后再做）**

1. 商户平台开通「小程序支付」，绑定小程序 AppId。
2. 配置 `fresh.order.wx-pay-mock=false` 及商户号、APIv3 密钥、证书（需在 `WxPayService` 中补齐真实统一下单调用）。
3. 支付结果以微信异步通知 `/order/pay/notify` 为准；公网 HTTPS 回调地址必配。
4. 开发期可用体验版 + 0.01 元真实订单验证，一般不再依赖旧版「支付沙箱」。

**商城模板改造最小接入点**

1. 登录页：`wx.login` → `POST /api/user/mini/login` → 存 `token`。
2. 请求头：`Authorization: {token}`。
3. 结算页：`POST /api/order/order/create` → `POST /api/order/order/pay/prepay`。
4. 支付：mock 阶段调 callback；真支付阶段用返回参数调 `wx.requestPayment`。

网关前缀约定：`/api/user/**`、`/api/goods/**`、`/api/order/**` 分别转发到 8081 / 8082 / 8083。

# Fresh 微信支付开发文档

面向当前阶段：**小程序还是商城模板、只有测试号、后端已具备 mock 预支付**。按阶段推进，不要一上来就接真商户号。

相关入口：

- 后端：`fresh-order` → `WxPayService`、`OrderController`
- 小程序模板：`frontend/fresh-mini-wx/pages/order/order-confirm/pay.js`（已接 Fresh 预支付 / mock 回调）
- 联调脚本：`fresh-parent/test-pay.ps1`
- 本地指南：根目录 `SETUP.md` 第 8 节

---

## 0. 你现在处在哪一步

| 阶段 | 目标 | 是否需要测试号 | 是否需要支付商户号 | 是否需要真钱 |
|------|------|----------------|--------------------|--------------|
| **P0 Mock 联调** | 验证「下单 → 预支付 → 支付成功 → 订单变待自提」 | 否 | 否 | 否 |
| **P1 测试号登录** | 真实 `code` 换 `openid` | **是** | 否 | 否 |
| **P2 小程序接支付 UI** | 结算页调后端预支付；mock 下用 callback 模拟付款 | 建议有 | 否 | 否 |
| **P3 真实微信支付** | `wx.requestPayment` + 微信异步通知 | 正式/已认证小程序更稳 | **是** | 体验版可付 0.01 |

**当前后端状态：**

- `fresh.order.wx-pay-mock=true`：预支付返回模拟 `prepayId` / `paySign`
- `POST /order/pay/callback`（或 `/order/pay/notify`）：mock 下可直接把订单标为已支付（幂等）
- `/api/order/order/pay/notify` 已加入网关白名单
- 非 mock 分支尚未对接微信 APIv3 统一下单（关闭 mock 会明确报错）

**当前小程序状态：**

- `config.useFreshPay=true`：登录 / 下单 / 支付走 Fresh 网关
- `wechatPayOrder`：prepay → mock 时调 callback；非 mock 时调 `wx.requestPayment`
- 结算页仍可用模板商品展示；提交时 mock 商品会映射为种子商品 `goodsId=1` 以便扣库存

---

## 1. 你需要准备 / 办理的事项（清单）

按勾选顺序做即可。

### 1.1 立刻可做（不申请账号也能做）

- [x] 本机启动 MySQL、Redis、Nacos（及可选 RocketMQ）
- [x] 启动 `fresh-user`、`fresh-goods`、`fresh-order`、`fresh-gateway`
- [x] 用 PowerShell / Apifox 跑通：登录 → 下单 → 预支付 → 回调 → 查订单（`.\test-pay.ps1`）
- [x] 确认配置：`fresh.order.wx-pay-mock=true`、`fresh.wechat.mock-enabled=true`

### 1.2 小程序测试号（P1 需要）

- [ ] 打开 [微信公众平台](https://mp.weixin.qq.com/) 登录（个人微信即可）
- [ ] 进入「小程序」→ 可用 **测试号** 或已有小程序账号
  - 测试号入口常见路径：开发 → 开发管理 / 开发设置；或搜索「小程序测试号」
  - 若已有正式小程序：开发管理 → 开发设置 同样能看到 AppID、AppSecret
- [ ] 记下并保管：
  - [ ] **AppID（小程序 ID）**
  - [ ] **AppSecret（小程序密钥）**（只显示一次/需重置，勿提交到 Git）
- [ ] 安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
- [ ] 用 AppID 导入/打开 `frontend/fresh-mini-wx`
- [ ] 开发阶段勾选：**不校验合法域名、web-view、TLS 版本以及 HTTPS 证书**
- [ ] （可选）添加体验成员 / 开发者，方便真机预览

### 1.3 域名与内网穿透（真机或关「不校验域名」时需要）

- [ ] 准备可 HTTPS 访问的后端地址（本地开发常用：ngrok、花生壳、cpolar 等）
- [ ] 在公众平台配置：
  - [ ] **request 合法域名**（如 `https://xxx.ngrok-free.app`）
  - [ ] 支付上线后还需配置 **支付授权目录 / 业务域名**（以商户平台与公众平台最新要求为准）
- [ ] 开发者工具本地调试可先不配域名（勾选「不校验」）

### 1.4 微信支付商户号（仅 P3 真支付需要）

- [ ] 主体具备开通资质（个体工商户 / 企业等；**个人测试号通常无法正式收款**）
- [ ] 注册 [微信支付商户平台](https://pay.weixin.qq.com/)
- [ ] 开通 **JSAPI 支付 / 小程序支付**
- [ ] 将小程序 AppID 与商户号绑定
- [ ] 拿到并妥善保存：
  - [ ] **商户号 mch_id**
  - [ ] **APIv3 密钥**
  - [ ] **商户 API 证书**（私钥、证书序列号）
- [ ] 配置支付结果通知 URL（公网 HTTPS），例如：  
  `https://你的域名/api/order/order/pay/notify`  
  （以网关实际 StripPrefix / 路由为准，需联调确认最终路径）
- [ ] 后端关闭 mock：`fresh.order.wx-pay-mock=false`，并实现真实统一下单与验签

> **说明：** 微信旧版「支付沙箱」基本不再作为主路径。开发期用 **mock**；上线前用 **体验版 + 0.01 元真实支付** 验证。

### 1.5 代码改造事项（你 / 开发同学要做的）

#### 后端

- [x] P0：保持 mock，补齐联调脚本与异常日志（`test-pay.ps1`、幂等回调、notify 白名单）
- [ ] P1：配置真实 `fresh.wechat.app-id` / `app-secret`，`mock-enabled=false`
- [ ] P3：实现微信 APIv3：
  - [ ] JSAPI 统一下单（openid + 金额 + 商户订单号）
  - [ ] 返回小程序 `wx.requestPayment` 所需字段
  - [ ] 支付回调验签、幂等更新订单、写 `pay_log`
  - [ ] 失败重试 / 查单补偿（建议）

#### 小程序模板

- [x] 统一 `baseURL` 指向网关 `http://localhost:8080`（真机改为内网穿透域名）
- [x] 启动时：`wx.login` → `POST /api/user/mini/login` → 本地存 `token`
- [x] 业务请求 Header：`Authorization: {token}`（`utils/request.js`）
- [x] 结算：对接 `POST /api/order/order/create`
- [x] 支付：对接 `POST /api/order/order/pay/prepay`
- [x] P2 mock：预支付返回 `mock=true` 时，改调 `POST /api/order/order/pay/callback`，再跳结果页
- [x] P3 分支：已接入 `wx.requestPayment`（需关 mock + 商户号后才走真支付）
- [x] 关闭「直接 paySuccess」捷径（`useFreshPay=true` 时）

配置：`frontend/fresh-mini-wx/config/index.js` 中 `useFreshPay: true`、`baseURL`。
---

## 2. 支付业务链路（目标形态）

```text
小程序结算
  → POST /api/order/order/create          生成待支付订单（扣库存等）
  → POST /api/order/order/pay/prepay      获取支付参数
      ├─ mock=true  → 前端调 /pay/callback 模拟成功
      └─ mock=false → wx.requestPayment → 用户付款
            → 微信服务器 POST /order/pay/notify
            → order 更新状态、写 pay_log、发 ORDER_SUCCESS_TOPIC
  → 跳转支付结果页 / 订单列表
```

网关约定：

| 服务 | 网关前缀 | 直连端口 |
|------|----------|----------|
| user | `/api/user/**` | 8081 |
| goods | `/api/goods/**` | 8082 |
| order | `/api/order/**` | 8083 |

订单接口示例（经网关）：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/order/order/create` | 下单，需登录 |
| POST | `/api/order/order/pay/prepay` | body: `{"orderNo":"..."}` |
| POST | `/api/order/order/pay/callback` | mock / 开发回调 |
| POST | `/api/order/order/pay/notify` | 正式回调入口 |
| GET | `/api/order/order/{orderNo}` | 查详情 |

---

## 3. P0：Mock 支付联调步骤（本周优先）

### 3.1 环境

```powershell
cd fresh-parent
docker compose up -d mysql redis nacos
# 可选：rocketmq-nameserver rocketmq-broker

# IDEA 或命令行启动：user → gateway → goods → order
```

确认：

```properties
# fresh-user
fresh.wechat.mock-enabled=true

# fresh-order
fresh.order.wx-pay-mock=true
```

### 3.2 接口脚本

```powershell
# 1. 登录
$login = Invoke-RestMethod -Method POST http://localhost:8080/api/user/mini/login `
  -ContentType "application/json" -Body '{"code":"pay-dev-001"}'
$token = $login.data.token
Write-Host "token=$token"

# 2. 准备：先保证有地址、商品库存；再创建订单（字段按你们实际 OrderCreateDTO）
# 得到 $orderNo 后继续：

# 3. 预支付
$prepay = Invoke-RestMethod -Method POST http://localhost:8080/api/order/order/pay/prepay `
  -ContentType "application/json" `
  -Headers @{ Authorization = $token } `
  -Body (@{ orderNo = $orderNo } | ConvertTo-Json)
$prepay | ConvertTo-Json -Depth 5
# 期望：mock=true，含 prepayId / timeStamp / nonceStr / package / paySign

# 4. 模拟支付成功
Invoke-RestMethod -Method POST http://localhost:8080/api/order/order/pay/callback `
  -ContentType "application/json" `
  -Body (@{ orderNo = $orderNo; transactionId = "mock_tx_001" } | ConvertTo-Json)

# 5. 查订单：status 应为 1（待自提）
Invoke-RestMethod http://localhost:8080/api/order/order/$orderNo `
  -Headers @{ Authorization = $token }
```

### 3.3 验收标准

- [ ] 预支付返回 `mock=true`
- [ ] 回调后 `order_main.status = 1`，`pay_time` 有值
- [ ] `pay_log` 有成功记录
- [ ] （若 RocketMQ 正常）用户侧可能收到积分奖励 / 站内信

---

## 4. P1：测试号真实登录

### 4.1 你要做的事

1. 拿到 AppID、AppSecret（见 §1.2）
2. 改 `fresh-user` 配置（勿提交密钥到仓库，可用本地 `application-local.properties` 或环境变量）：

```properties
fresh.wechat.mock-enabled=false
fresh.wechat.app-id=wx你的AppId
fresh.wechat.app-secret=你的Secret
```

3. 小程序登录页示例逻辑：

```javascript
wx.login({
  success: async (res) => {
    const data = await request.post('/api/user/mini/login', { code: res.code });
    wx.setStorageSync('token', data.token);
  },
});
```

### 4.2 验收标准

- [ ] 同一微信用户多次登录，后端 `app_user.openid` 稳定不变
- [ ] 错误的 code 返回明确失败，而不是生成随机 `wx_xxx` 用户

---

## 5. P2：小程序接上 Mock 支付

### 5.1 改造 `pay.js` 建议逻辑

```text
调用 createOrder → 得到 orderNo
调用 prepay(orderNo)
如果 data.mock === 'true':
    调用 /order/pay/callback
    跳转支付成功页
否则:
    wx.requestPayment({ ...后端返回字段 })
```

### 5.2 验收标准

- [ ] 开发者工具里从结算到支付结果页跑通
- [ ] 订单列表显示「待自提 / 已支付」状态正确
- [ ] 正式环境不会误走「直接 paySuccess」

---

## 6. P3：真实微信支付（最后做）

### 6.1 前置条件全部满足

- [ ] 已认证小程序（或可绑定支付的主体）
- [ ] 商户号开通且绑定 AppID
- [ ] 后端实现 APIv3 下单 + 回调验签
- [ ] 公网 HTTPS 通知地址可达
- [ ] `fresh.order.wx-pay-mock=false`

### 6.2 联调注意

1. 必须用真机或体验版测 `wx.requestPayment`（开发者工具对支付支持有限）
2. **不要信任前端「支付成功」回调作为入账依据**，以微信异步通知 / 查单为准
3. 回调要做：**验签、幂等、金额核对、订单状态机校验**
4. 先付 **0.01 元**，确认退款流程（若已做退款）

### 6.3 关于「沙箱」

| 方式 | 建议 |
|------|------|
| 后端 `wx-pay-mock` | **开发主路径，优先用** |
| 微信旧支付沙箱 | 不推荐作为主方案 |
| 体验版真实小额支付 | **上线前必做** |

---

## 7. 配置对照表

| 配置项 | 模块 | 开发默认 | 含义 |
|--------|------|----------|------|
| `fresh.wechat.mock-enabled` | user | `true` | true：openid=`wx_{code}` |
| `fresh.wechat.app-id` | user | 空 | 关 mock 后必填 |
| `fresh.wechat.app-secret` | user | 空 | 关 mock 后必填 |
| `fresh.order.wx-pay-mock` | order | `true` | true：假预支付 + 可用 callback |
| 商户号 / APIv3 / 证书 | order | 未接 | 仅 P3 |

---

## 8. 建议排期（可直接当任务板）

| 周次 | 任务 | 负责人动作 |
|------|------|------------|
| 第 1 步 | P0 接口联调通过 | 你：启动服务 + 跑脚本；不申请商户号 |
| 第 2 步 | 申请/拿到小程序测试号 | 你：AppID/Secret；配进本地配置 |
| 第 3 步 | P1 登录接通 | 改小程序登录页 + 关 wechat mock |
| 第 4 步 | P2 结算/支付接后端 mock | 改 `order-confirm` / `pay.js` |
| 第 5 步起 | 评估主体资质，开商户号 | 仅当真要收款时再做 P3 |

---

## 9. 常见问题

**Q：只有测试号，能不能先做支付开发？**  
能。用 P0/P2 的 mock 即可把业务做完；测试号主要用于 P1 换真实 openid。收款必须商户号。

**Q：开发阶段要不要微信支付沙箱？**  
一般不需要。用本项目 `wx-pay-mock` 即可。

**Q：为什么开发者工具里支付总是怪怪的？**  
`wx.requestPayment` 建议真机调试；mock 阶段用 callback，不依赖支付控件。

**Q：密钥怎么放？**  
不要写进 Git。用本地 profile、环境变量或 Nacos 加密配置。

**Q：回调地址配哪个？**  
经网关对外统一入口时配网关 HTTPS 路径；本地 mock 可直接打 `8083` 或网关的 callback，不必公网。

---

## 10. 文档维护

- 后端支付实现变更时，同步更新本文 §0、§2、§6
- 小程序正式接好后，在本文增加「页面路径 ↔ 接口」对照
- 总览仍以 `SETUP.md` 为准；支付专项以本文为准

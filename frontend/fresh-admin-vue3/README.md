# Fresh 管理后台 (Vue3)

基于 Vue3 + Vite + Element Plus 的简易管理端，优先对接已实现的后端接口，页面以跑通功能为主。

## 启动

```bash
cd frontend/fresh-admin-vue3
npm install
npm run dev
```

浏览器访问：http://localhost:5173

需先启动网关 `8080` 及依赖微服务（见根目录 `SETUP.md`）。

## 账号

- 用户名：`admin`
- 密码：`admin123`

Token 放在请求头 `Authorization`（不加 Bearer），与小程序端一致。

## 已对接模块

| 菜单 | 说明 |
|------|------|
| 数据看板 | 今日统计 / 销量排行 / 成团售后率 |
| 分类 / 商品 / 团购 / 秒杀 | goods 管理端 CRUD |
| 评价管理 | 分页、回复、隐藏 |
| 营销中心 | 券模板、满减、积分兑券、整点抢券、抽奖、用券记录 |
| AI 中心 | 知识库 CRUD、对话/识图/文案日志 |
| 消息模板 | 列表只读（后端 CRUD 未齐） |

## 暂未对接（后端接口未实现）

- 订单 / 售后审核
- 管理员 RBAC（角色菜单）
- 网关限流 / IP 黑名单

## 代理

开发环境 Vite 将 `/api` 代理到 `http://localhost:8080`。

# GitHub 开源前检查清单

上传公开仓库前请确认：

1. **不要提交** `backup/` 下的 SQL 备份、`fresh-parent/.env`、任意真实 API Key。
2. AI 密钥：复制 `fresh-parent/.env.example` 为 `.env`，填写 `DASHSCOPE_API_KEY`；或在本地 `application-dev-local.properties` 中配置（已 gitignore）。
3. 小程序：`project.config.json` 默认 `touristappid`，请改成你自己的测试号 AppID；真机调试时把 `frontend/fresh-mini-wx/config/index.js` 的 `baseURL` 改成电脑局域网 IP（勿把个人 IP 再提交回去）。
4. 演示账号：`admin` / `admin123`、MySQL `root`/`root` 仅用于本地 Docker，生产务必修改。

本地 AI（可选）：

```bash
cd fresh-parent
copy .env.example .env
# 编辑 .env，填入 DASHSCOPE_API_KEY=sk-你的密钥
```

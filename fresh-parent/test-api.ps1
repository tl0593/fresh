# Fresh API 测试（PowerShell 专用，不要用 curl 别名）
# 用法: cd fresh-parent; .\test-api.ps1

Write-Host "=== 1. 网关健康检查 ===" -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri "http://localhost:8080/gateway/health" -TimeoutSec 10
$health | ConvertTo-Json

Write-Host "`n=== 2. 小程序登录（经网关） ===" -ForegroundColor Cyan
$login = Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/user/mini/login" `
    -ContentType "application/json" `
    -Body '{"code":"test001"}'
$login | ConvertTo-Json -Depth 5

if ($login.data.token) {
    Write-Host "`n登录成功! Token: $($login.data.token)" -ForegroundColor Green
}

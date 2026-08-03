# Fresh 支付联调脚本（P0 Mock）
# Usage: cd fresh-parent; .\test-pay.ps1

$Base = "http://localhost:8080"

Write-Host "=== 1. Mini Login ===" -ForegroundColor Cyan
$login = Invoke-RestMethod -Method POST "$Base/api/user/mini/login" `
  -ContentType "application/json" -Body '{"code":"pay-dev-001"}'
$token = $login.data.token
Write-Host "login ok, hasToken=$([bool]$token)"

Write-Host "=== 2. Ensure Address ===" -ForegroundColor Cyan
$headers = @{ Authorization = $token }
$addrs = Invoke-RestMethod "$Base/api/user/address/list" -Headers $headers
$list = @($addrs.data)
if ($list.Count -eq 0) {
  Invoke-RestMethod -Method POST "$Base/api/user/address/save" `
    -ContentType "application/json" -Headers $headers `
    -Body '{"name":"自提用户","phone":"13800138000","community":"阳光社区自提点","detailAddr":"默认自提","isDefault":1,"delFlag":0}' | Out-Null
  $addrs = Invoke-RestMethod "$Base/api/user/address/list" -Headers $headers
  $list = @($addrs.data)
}
$addressId = $list[0].id
Write-Host "addressId=$addressId"

Write-Host "=== 3. Create Order ===" -ForegroundColor Cyan
$orderBody = @{
  addressId = $addressId
  integralUsed = 0
  items = @(
    @{
      goodsId = 1
      specId = 1
      goodsName = "有机菠菜"
      goodsImg = "https://example.com/spinach.jpg"
      price = 6.50
      num = 1
      activityType = 1
    }
  )
} | ConvertTo-Json -Depth 5
$order = Invoke-RestMethod -Method POST "$Base/api/order/order/create" `
  -ContentType "application/json" -Headers $headers -Body $orderBody
$orderNo = $order.data.orderNo
Write-Host "orderNo=$orderNo"

Write-Host "=== 4. Prepay ===" -ForegroundColor Cyan
$prepay = Invoke-RestMethod -Method POST "$Base/api/order/order/pay/prepay" `
  -ContentType "application/json" -Headers $headers `
  -Body (@{ orderNo = $orderNo } | ConvertTo-Json)
Write-Host "mock=$($prepay.data.mock) payAmount=$($prepay.data.payAmount) prepayId=$($prepay.data.prepayId)"
if ($prepay.data.mock -ne "true") {
  Write-Host "WARN: expected mock=true" -ForegroundColor Yellow
}

Write-Host "=== 5. Mock Callback ===" -ForegroundColor Cyan
Invoke-RestMethod -Method POST "$Base/api/order/order/pay/callback" `
  -ContentType "application/json" -Headers $headers `
  -Body (@{ orderNo = $orderNo; transactionId = "mock_tx_001" } | ConvertTo-Json) | Out-Null

Write-Host "=== 6. Order Detail ===" -ForegroundColor Cyan
$detail = Invoke-RestMethod "$Base/api/order/order/$orderNo" -Headers $headers
$status = $detail.data.order.status
$payTime = $detail.data.order.payTime
Write-Host "status=$status payTime=$payTime items=$(@($detail.data.items).Count)"
if ($status -eq 1) {
  Write-Host "PASS: status=1 待自提" -ForegroundColor Green
} else {
  Write-Host "FAIL: expected status=1, got $status" -ForegroundColor Red
  exit 1
}

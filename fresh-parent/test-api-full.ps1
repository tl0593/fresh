# Fresh full API test (PowerShell)
# Usage: cd fresh-parent; .\test-api-full.ps1

$Base = "http://localhost:8080"
$script:Token = $null
$pass = 0
$fail = 0

function Invoke-FreshApi {
    param(
        [string]$Name,
        [string]$Method = "GET",
        [string]$Path,
        [string]$Body = $null,
        [switch]$NoAuth
    )
    Write-Host ""
    Write-Host "=== $Name ===" -ForegroundColor Cyan
    $uri = "$Base$Path"
    Write-Host "$Method $uri"

    $reqHeaders = @{}
    if (-not $NoAuth -and $script:Token) {
        $reqHeaders["Authorization"] = $script:Token
    }

    try {
        $params = @{
            Uri        = $uri
            Method     = $Method
            Headers    = $reqHeaders
            TimeoutSec = 15
        }
        if ($Body) {
            $params["ContentType"] = "application/json"
            $params["Body"] = $Body
        }
        $resp = Invoke-RestMethod @params
        Write-Host ($resp | ConvertTo-Json -Depth 6 -Compress) -ForegroundColor Green
        $script:pass++
        return $resp
    } catch {
        Write-Host "FAIL: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message -ForegroundColor Yellow }
        $script:fail++
        return $null
    }
}

Invoke-FreshApi -Name "Gateway Health" -Path "/gateway/health" -NoAuth

$loginResp = Invoke-FreshApi -Name "Mini Login" -Method POST -Path "/api/user/mini/login" -Body '{"code":"test001"}' -NoAuth
if (-not $loginResp) { Write-Host "Login failed, abort." -ForegroundColor Red; exit 1 }
$script:Token = $loginResp.data.token
Write-Host "Token obtained: $script:Token" -ForegroundColor Green

Invoke-FreshApi -Name "Category Tree" -Path "/api/goods/category/tree"
Invoke-FreshApi -Name "Hot Goods" -Path "/api/goods/goods/hot"
Invoke-FreshApi -Name "Goods Detail" -Path "/api/goods/goods/1"
Invoke-FreshApi -Name "Group List" -Path "/api/goods/group/list"
Invoke-FreshApi -Name "Seckill List" -Path "/api/goods/seckill/list"

Invoke-FreshApi -Name "Address List" -Path "/api/user/address/list"
Invoke-FreshApi -Name "Cart List" -Path "/api/user/cart/list"
Invoke-FreshApi -Name "Cart Update" -Method POST -Path "/api/user/cart/update" -Body '{"goodsId":1,"specId":1,"num":2,"selected":1}'
Invoke-FreshApi -Name "Integral Log" -Path "/api/user/integral/log"

$orderBody = '{"addressId":1,"integralUsed":0,"items":[{"goodsId":1,"specId":1,"goodsName":"Spinach","goodsImg":"img.jpg","price":6.50,"num":1,"activityType":1}]}'
$orderResp = Invoke-FreshApi -Name "Create Order" -Method POST -Path "/api/order/order/create" -Body $orderBody
if ($orderResp -and $orderResp.data.orderNo) {
    $orderNo = $orderResp.data.orderNo
    Invoke-FreshApi -Name "Order List" -Path "/api/order/order/list"
    Invoke-FreshApi -Name "Order Detail" -Path "/api/order/order/$orderNo"
    $payBody = "{`"orderNo`":`"$orderNo`"}"
    Invoke-FreshApi -Name "Pay Prepay" -Method POST -Path "/api/order/order/pay/prepay" -Body $payBody
    Invoke-FreshApi -Name "Pay Callback" -Method POST -Path "/api/order/order/pay/callback" -Body "{`"orderNo`":`"$orderNo`",`"transactionId`":`"mock_tx_001`"}"
    Invoke-FreshApi -Name "Order Detail After Pay" -Path "/api/order/order/$orderNo"
}

Invoke-FreshApi -Name "AI Chat" -Method POST -Path "/api/ai/ai/chat/send" -Body '{"sessionKey":"s1","userMsg":"How to store spinach?","chatType":1}'
Invoke-FreshApi -Name "AI Knowledge" -Path "/api/ai/ai/knowledge/list"
Invoke-FreshApi -Name "Inner Message" -Path "/api/message/inner/list"
Invoke-FreshApi -Name "Today Stats" -Path "/api/data/stat/today"
Invoke-FreshApi -Name "Goods Sales Stats" -Path "/api/data/stat/goods/sales"

Write-Host ""
Write-Host "========== Done: $pass passed, $fail failed ==========" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Yellow" })

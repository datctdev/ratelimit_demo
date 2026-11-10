param(
    [ValidateSet("bucket4j", "redis", "unlimited")]
    [string]$Mode = "bucket4j"
)

$baseUrl = "http://localhost:8080"
$endpoint = "$baseUrl/api/$Mode/orchids"

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ">>> KỊCH BẢN DEMO RATE LIMITER (MODE: $($Mode.ToUpper())) <<<" -ForegroundColor Yellow
Write-Host ">>> Endpoint kiểm thử: $endpoint <<<" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan

# Reset đầy lại thùng token (10/10) và bộ đếm backend trước khi bắn 20 requests
try {
    Invoke-RestMethod -Uri "$baseUrl/api/bucket4j/reset" -Method GET -ErrorAction SilentlyContinue | Out-Null
    Invoke-RestMethod -Uri "$baseUrl/api/unlimited/orchids/reset" -Method POST -ErrorAction SilentlyContinue | Out-Null
    Write-Host "[INIT] Đã nạp đầy thùng 10/10 Tokens & Reset bộ đếm Orchid-Service về 0!`n" -ForegroundColor Gray
} catch {}

$successCount = 0
$limitCount = 0

1..20 | ForEach-Object {
    $reqIndex = "{0:D2}" -f $_
    try {
        $response = Invoke-WebRequest -Uri $endpoint -Method GET -UseBasicParsing -TimeoutSec 3
        $statusCode = [int]$response.StatusCode
        $remaining = $response.Headers["X-RateLimit-Remaining"]
        if (-not $remaining) { $remaining = "-" }
        $successCount++
        Write-Host "[Request #$reqIndex] -> HTTP $statusCode OK | Token còn lại: $remaining/10 | Orchid-Service đã xử lý!" -ForegroundColor Green
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $retryAfter = $_.Exception.Response.Headers["Retry-After"]
            if (-not $retryAfter) { $retryAfter = "1" }
            if ($statusCode -eq 429) {
                $limitCount++
                Write-Host "[Request #$reqIndex] -> HTTP 429 TOO MANY REQUESTS | HẾT TOKEN (0/10)! Bị chặn tại Gateway (Chờ ${retryAfter}s)" -ForegroundColor Red
            } else {
                Write-Host "[Request #$reqIndex] -> HTTP $statusCode" -ForegroundColor Yellow
            }
        } else {
            Write-Host "[Request #$reqIndex] -> Lỗi kết nối: $($_.Exception.Message)" -ForegroundColor DarkRed
        }
    }
}

Write-Host "`n---------------- TỔNG KẾT KẾT QUẢ DEMO (20 REQUESTS) ----------------" -ForegroundColor Cyan
Write-Host "1. Số Requests ĐƯỢC QUA GATEWAY (200 OK)        : $successCount requests" -ForegroundColor Green
Write-Host "2. Số Requests BỊ GATEWAY CHẶN ĐỨNG (429 LIMIT) : $limitCount requests" -ForegroundColor Red
Write-Host "3. Kết luận: Orchid-Service (Port 8082) chỉ phải xử lý $successCount requests, hoàn toàn không bị sập!" -ForegroundColor Yellow
Write-Host "=====================================================================" -ForegroundColor Cyan

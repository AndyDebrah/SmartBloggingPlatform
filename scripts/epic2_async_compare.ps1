param(
    [string]$Profile = "local",
    [int]$Port = 8090,
    [int]$Users = 12,
    [int]$RequestsPerUser = 10,
    [string]$OutDir = "analysis/epic-tests/epic-2"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Load-DotEnv {
    param([string]$Path = ".env")
    if (-not (Test-Path $Path)) { return }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) { return }
        if ($line -notmatch "^[A-Za-z_][A-Za-z0-9_]*=") { return }
        $name, $value = $line -split "=", 2
        Set-Item -Path ("Env:" + $name) -Value $value
    }
}

function Percentile {
    param([double[]]$Values, [double]$P)
    if ($Values.Count -eq 0) { return 0.0 }
    $sorted = $Values | Sort-Object
    $rank = [math]::Ceiling(($P / 100.0) * $sorted.Count)
    if ($rank -lt 1) { $rank = 1 }
    if ($rank -gt $sorted.Count) { $rank = $sorted.Count }
    return [double]$sorted[$rank - 1]
}

function New-AuthToken {
    param([string]$BaseUrl)
    $u = "perf_epic2_" + (Get-Date -Format "yyyyMMddHHmmss")
    $email = "$u@example.com"
    $pwd = "PerfTest123!"
    $registerBody = @{ username = $u; email = $email; password = $pwd; role = "AUTHOR" } | ConvertTo-Json
    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/register" -ContentType "application/json" -Body $registerBody | Out-Null
    } catch {}
    $loginBody = @{ username = $u; password = $pwd } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType "application/json" -Body $loginBody
    if (-not $login.token) { throw "Could not get JWT token for Epic 2 load test." }
    return [string]$login.token
}

function Invoke-ParallelLoad {
    param(
        [string]$Uri,
        [string]$Token,
        [int]$UsersCount,
        [int]$ReqPerUser
    )
    $jobs = @()
    for ($u = 1; $u -le $UsersCount; $u++) {
        $jobs += Start-Job -ScriptBlock {
            param($targetUri, $jwt, $count)
            $rows = @()
            $headers = @{ Authorization = "Bearer $jwt" }
            for ($i = 1; $i -le $count; $i++) {
                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                $status = 0
                $ok = $false
                try {
                    $resp = Invoke-WebRequest -UseBasicParsing -Uri $targetUri -Headers $headers -TimeoutSec 30
                    $status = [int]$resp.StatusCode
                    $ok = ($status -ge 200 -and $status -lt 300)
                } catch {
                    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
                        $status = [int]$_.Exception.Response.StatusCode
                    }
                    $ok = $false
                } finally {
                    $sw.Stop()
                }
                $rows += [pscustomobject]@{
                    LatencyMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                    StatusCode = $status
                    Success = $ok
                }
            }
            return $rows
        } -ArgumentList $Uri, $Token, $ReqPerUser
    }

    $all = @()
    foreach ($job in $jobs) {
        Wait-Job $job | Out-Null
        $all += Receive-Job $job
        Remove-Job $job | Out-Null
    }

    return $all
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Load-DotEnv
Remove-Item Env:MAVEN_OPTS -ErrorAction SilentlyContinue
if (-not $env:SPRING_DATASOURCE_URL -and $env:DB_URL) { $env:SPRING_DATASOURCE_URL = $env:DB_URL }
if (-not $env:SPRING_DATASOURCE_USERNAME -and $env:DB_USERNAME) { $env:SPRING_DATASOURCE_USERNAME = $env:DB_USERNAME }
if (-not $env:SPRING_DATASOURCE_PASSWORD -and $env:DB_PASSWORD) { $env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD }

$appLog = Join-Path $OutDir "epic-2-app.log"
$appErrLog = Join-Path $OutDir "epic-2-app.err.log"
$rawCsv = Join-Path $OutDir "epic-2-concurrent-raw.csv"
$summaryJson = Join-Path $OutDir "epic-2-concurrent-summary.json"
$reportMd = Join-Path $OutDir "epic-2-concurrent-report.md"

$args = @(
    "spring-boot:run",
    "-Dspring-boot.run.profiles=$Profile",
    "-Dspring-boot.run.arguments=--server.port=$Port"
)

Write-Host "Starting app for Epic 2 concurrent load..."
$proc = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $args -PassThru -RedirectStandardOutput $appLog -RedirectStandardError $appErrLog

try {
    $started = $false
    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Path $appLog) {
            $tail = Get-Content $appLog -Tail 40 -ErrorAction SilentlyContinue
            if ($tail -match "Started SmartBlogApplication") {
                $started = $true
                break
            }
        }
        if ($proc.HasExited) { break }
    }
    if (-not $started) { throw "App did not start for Epic 2 test. See $appLog" }

    $baseUrl = "http://localhost:$Port"
    $token = New-AuthToken -BaseUrl $baseUrl

    $endpointMap = [ordered]@{
        post_retrieval = "$baseUrl/api/posts?page=0&size=20"
        comment_loading = "$baseUrl/api/comments/post/1?page=0&size=20"
        user_analytics = "$baseUrl/api/reviews/post/1/stats"
    }

    $rows = New-Object System.Collections.Generic.List[object]
    foreach ($name in $endpointMap.Keys) {
        Write-Host "Running concurrent load for $name ..."
        $result = Invoke-ParallelLoad -Uri $endpointMap[$name] -Token $token -UsersCount $Users -ReqPerUser $RequestsPerUser
        foreach ($r in $result) {
            $rows.Add([pscustomobject]@{
                Endpoint = $name
                LatencyMs = $r.LatencyMs
                StatusCode = $r.StatusCode
                Success = $r.Success
                TimestampUtc = (Get-Date).ToUniversalTime().ToString("o")
            })
        }
    }
    $rows | Export-Csv -NoTypeInformation -Path $rawCsv

    $summary = [ordered]@{}
    foreach ($name in $endpointMap.Keys) {
        $slice = $rows | Where-Object { $_.Endpoint -eq $name }
        $lat = @($slice | ForEach-Object { [double]$_.LatencyMs })
        $okCount = @($slice | Where-Object { $_.Success }).Count
        $summary[$name] = [ordered]@{
            requests = $slice.Count
            successCount = $okCount
            errorCount = $slice.Count - $okCount
            avgMs = [math]::Round((($lat | Measure-Object -Average).Average), 2)
            p50Ms = [math]::Round((Percentile -Values $lat -P 50), 2)
            p95Ms = [math]::Round((Percentile -Values $lat -P 95), 2)
            maxMs = [math]::Round((($lat | Measure-Object -Maximum).Maximum), 2)
        }
    }

    $baselinePath = "analysis/epic-tests/epic-1/baseline-summary.json"
    $baseline = $null
    if (Test-Path $baselinePath) {
        $baseline = Get-Content $baselinePath -Raw | ConvertFrom-Json
    }

    $comparison = [ordered]@{}
    if ($baseline -and $baseline.endpoints) {
        foreach ($name in $summary.Keys) {
            $baseAvg = [double]$baseline.endpoints.$name.avgMs
            $newAvg = [double]$summary[$name].avgMs
            $delta = [math]::Round(($newAvg - $baseAvg), 2)
            $improvement = if ($baseAvg -gt 0) { [math]::Round((($baseAvg - $newAvg) / $baseAvg) * 100, 2) } else { 0 }
            $comparison[$name] = [ordered]@{
                baselineAvgMs = $baseAvg
                epic2AvgMs = $newAvg
                deltaMs = $delta
                improvementPercent = $improvement
            }
        }
    }

    $final = [ordered]@{
        generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        profile = $Profile
        port = $Port
        users = $Users
        requestsPerUser = $RequestsPerUser
        endpoints = $summary
        comparisonVsEpic1 = $comparison
        artifacts = [ordered]@{
            appLog = $appLog
            appErrLog = $appErrLog
            rawCsv = $rawCsv
        }
    }
    $final | ConvertTo-Json -Depth 6 | Set-Content -Path $summaryJson

    $lines = @()
    $lines += "# Epic 2 Concurrent Comparison Report"
    $lines += ""
    $lines += "Generated: " + (Get-Date).ToString("u")
    $lines += ""
    $lines += "| Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | Max (ms) |"
    $lines += "|---|---:|---:|---:|---:|---:|---:|"
    foreach ($k in $summary.Keys) {
        $s = $summary[$k]
        $lines += "| $k | $($s.requests) | $($s.errorCount) | $($s.avgMs) | $($s.p50Ms) | $($s.p95Ms) | $($s.maxMs) |"
    }
    if ($comparison.Count -gt 0) {
        $lines += ""
        $lines += "## Comparison vs Epic 1 Baseline"
        $lines += "| Endpoint | Baseline Avg (ms) | Epic 2 Avg (ms) | Delta (ms) | Improvement % |"
        $lines += "|---|---:|---:|---:|---:|"
        foreach ($k in $comparison.Keys) {
            $c = $comparison[$k]
            $lines += "| $k | $($c.baselineAvgMs) | $($c.epic2AvgMs) | $($c.deltaMs) | $($c.improvementPercent) |"
        }
    }
    $lines | Set-Content -Path $reportMd

    Write-Host "Epic 2 comparison complete."
    Write-Host "Summary: $summaryJson"
    Write-Host "Report:  $reportMd"
}
finally {
    if ($proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}

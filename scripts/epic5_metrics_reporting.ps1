param(
    [string]$Profile = "local",
    [int]$Port = 8140,
    [int]$Users = 12,
    [int]$RequestsPerUser = 15,
    [int]$TomcatMaxThreads = 8,
    [string]$OutDir = "analysis/epic-tests/epic-5"
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

function Wait-ForStartup {
    param([System.Diagnostics.Process]$Proc, [string]$LogPath, [int]$TimeoutSeconds = 180)
    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Path $LogPath) {
            $tail = Get-Content $LogPath -Tail 120 -ErrorAction SilentlyContinue
            if ($tail -match "Started SmartBlogApplication") { return $true }
        }
        if ($Proc.HasExited) { return $false }
    }
    return $false
}

function New-AuthToken {
    param([string]$BaseUrl)
    $u = "perf_epic5_" + (Get-Date -Format "yyyyMMddHHmmssfff")
    $email = "$u@example.com"
    $pwd = "PerfTest123!"
    $registerBody = @{ username = $u; email = $email; password = $pwd; role = "AUTHOR" } | ConvertTo-Json
    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/register" -ContentType "application/json" -Body $registerBody | Out-Null
    } catch {}
    $loginBody = @{ username = $u; password = $pwd } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType "application/json" -Body $loginBody
    if (-not $login.token) { throw "Could not get JWT token for Epic 5 test." }
    return [string]$login.token
}

function Invoke-ConcurrentLoad {
    param(
        [string]$Uri,
        [string]$Token,
        [int]$UsersCount,
        [int]$ReqPerUser,
        [int]$TimeoutSec = 30
    )

    $pool = [runspacefactory]::CreateRunspacePool(1, $UsersCount)
    $pool.Open()
    $work = @()

    for ($u = 1; $u -le $UsersCount; $u++) {
        $ps = [powershell]::Create()
        $ps.RunspacePool = $pool
        [void]$ps.AddScript({
            param($targetUri, $jwt, $count, $timeout)
            $rows = @()
            $headers = @{ Authorization = "Bearer $jwt" }
            for ($i = 1; $i -le $count; $i++) {
                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                $status = 0
                $ok = $false
                try {
                    $resp = Invoke-WebRequest -UseBasicParsing -Uri $targetUri -Headers $headers -TimeoutSec $timeout
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
        }).AddArgument($Uri).AddArgument($Token).AddArgument($ReqPerUser).AddArgument($TimeoutSec)

        $work += [pscustomobject]@{
            Shell = $ps
            Handle = $ps.BeginInvoke()
        }
    }

    $all = @()
    foreach ($item in $work) {
        $all += $item.Shell.EndInvoke($item.Handle)
        $item.Shell.Dispose()
    }
    $pool.Close()
    $pool.Dispose()
    return $all
}

function Get-ActuatorMetricValue {
    param(
        [string]$BaseUrl,
        [hashtable]$Headers,
        [string]$MetricName,
        [string]$Statistic
    )
    try {
        $resp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/metrics/$MetricName" -Headers $Headers -TimeoutSec 20
        if (-not $resp.measurements) { return 0.0 }
        $matches = @($resp.measurements | Where-Object { $_.statistic -eq $Statistic } | ForEach-Object { [double]$_.value })
        if ($matches.Count -gt 0) {
            return [math]::Round((($matches | Measure-Object -Sum).Sum), 6)
        }
        return [math]::Round(([double]$resp.measurements[0].value), 6)
    } catch {
        return 0.0
    }
}

function Get-PrometheusSnapshot {
    param([string]$BaseUrl, [hashtable]$Headers, [string]$Path)
    try {
        $raw = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/actuator/prometheus" -Headers $Headers -TimeoutSec 20
        $raw.Content | Set-Content -Path $Path
        return $true
    } catch {
        "Prometheus scrape unavailable: $($_.Exception.Message)" | Set-Content -Path $Path
        return $false
    }
}

function Read-JsonIfExists {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return $null }
    return Get-Content -Raw -Path $Path | ConvertFrom-Json
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Load-DotEnv
Remove-Item Env:MAVEN_OPTS -ErrorAction SilentlyContinue
if (-not $env:SPRING_DATASOURCE_URL -and $env:DB_URL) { $env:SPRING_DATASOURCE_URL = $env:DB_URL }
if (-not $env:SPRING_DATASOURCE_USERNAME -and $env:DB_USERNAME) { $env:SPRING_DATASOURCE_USERNAME = $env:DB_USERNAME }
if (-not $env:SPRING_DATASOURCE_PASSWORD -and $env:DB_PASSWORD) { $env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD }

$rawCsv = Join-Path $OutDir "epic-5-metrics-raw.csv"
$summaryJson = Join-Path $OutDir "epic-5-metrics-summary.json"
$reportMd = Join-Path $OutDir "epic-5-metrics-report.md"
$promBeforePath = Join-Path $OutDir "prom-before.txt"
$promAfterPath = Join-Path $OutDir "prom-after.txt"
$timelineCsv = Join-Path $OutDir "epic-5-metrics-timeline.csv"
$appLog = Join-Path $OutDir ("epic-5-app-" + (Get-Date -Format "yyyyMMddHHmmss") + ".log")
$appErr = Join-Path $OutDir ("epic-5-app-" + (Get-Date -Format "yyyyMMddHHmmss") + ".err.log")

$runArgs = "--server.port=$Port --server.tomcat.threads.max=$TomcatMaxThreads --app.async.enabled=true --app.optimization.caching.enabled=true --app.optimization.reviewStats.singleQuery=true"
$mvnArgs = @(
    "spring-boot:run",
    "-Dspring-boot.run.profiles=$Profile",
    "-Dspring-boot.run.arguments=`"$runArgs`""
)

Write-Host "Starting optimized app for Epic 5 metrics collection on port $Port ..."
$proc = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $mvnArgs -PassThru -RedirectStandardOutput $appLog -RedirectStandardError $appErr

try {
    if (-not (Wait-ForStartup -Proc $proc -LogPath $appLog)) {
        throw "App did not start for Epic 5 run. See $appLog and $appErr"
    }

    $baseUrl = "http://localhost:$Port"
    $token = New-AuthToken -BaseUrl $baseUrl
    $headers = @{ Authorization = "Bearer $token" }

    $promBeforeOk = Get-PrometheusSnapshot -BaseUrl $baseUrl -Headers $headers -Path $promBeforePath

    $beforeHttpCount = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "http.server.requests" -Statistic "COUNT"
    $beforeHeapUsedBytes = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.memory.used" -Statistic "VALUE"
    $beforeThreads = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.threads.live" -Statistic "VALUE"

    $endpointMap = [ordered]@{
        post_retrieval = "$baseUrl/api/posts?page=0&size=20"
        comment_loading = "$baseUrl/api/comments/post/1?page=0&size=20"
        user_analytics = "$baseUrl/api/reviews/post/1/stats"
    }

    $rows = New-Object System.Collections.Generic.List[object]
    $timeline = New-Object System.Collections.Generic.List[object]

    foreach ($name in $endpointMap.Keys) {
        Write-Host "[epic5] Warmup $name ..."
        Invoke-ConcurrentLoad -Uri $endpointMap[$name] -Token $token -UsersCount 2 -ReqPerUser 5 | Out-Null

        $timeline.Add([pscustomobject]@{
            stage = "before_$name"
            timestampUtc = (Get-Date).ToUniversalTime().ToString("o")
            httpServerRequestsCount = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "http.server.requests" -Statistic "COUNT"
            jvmMemoryUsedBytes = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.memory.used" -Statistic "VALUE"
            jvmThreadsLive = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.threads.live" -Statistic "VALUE"
        })

        Write-Host "[epic5] Measuring $name ..."
        $result = Invoke-ConcurrentLoad -Uri $endpointMap[$name] -Token $token -UsersCount $Users -ReqPerUser $RequestsPerUser
        foreach ($r in $result) {
            $rows.Add([pscustomobject]@{
                Endpoint = $name
                LatencyMs = $r.LatencyMs
                StatusCode = $r.StatusCode
                Success = $r.Success
                TimestampUtc = (Get-Date).ToUniversalTime().ToString("o")
            })
        }

        $timeline.Add([pscustomobject]@{
            stage = "after_$name"
            timestampUtc = (Get-Date).ToUniversalTime().ToString("o")
            httpServerRequestsCount = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "http.server.requests" -Statistic "COUNT"
            jvmMemoryUsedBytes = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.memory.used" -Statistic "VALUE"
            jvmThreadsLive = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.threads.live" -Statistic "VALUE"
        })
    }

    $promAfterOk = Get-PrometheusSnapshot -BaseUrl $baseUrl -Headers $headers -Path $promAfterPath

    $afterHttpCount = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "http.server.requests" -Statistic "COUNT"
    $afterHeapUsedBytes = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.memory.used" -Statistic "VALUE"
    $afterThreads = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.threads.live" -Statistic "VALUE"

    $rows | Export-Csv -NoTypeInformation -Path $rawCsv
    $timeline | Export-Csv -NoTypeInformation -Path $timelineCsv

    $summary = [ordered]@{}
    foreach ($endpoint in $endpointMap.Keys) {
        $slice = $rows | Where-Object { $_.Endpoint -eq $endpoint }
        $lat = @($slice | ForEach-Object { [double]$_.LatencyMs })
        $ok = @($slice | Where-Object { $_.Success }).Count
        $summary[$endpoint] = [ordered]@{
            requests = $slice.Count
            errorCount = $slice.Count - $ok
            avgMs = [math]::Round((($lat | Measure-Object -Average).Average), 2)
            p50Ms = [math]::Round((Percentile -Values $lat -P 50), 2)
            p95Ms = [math]::Round((Percentile -Values $lat -P 95), 2)
            maxMs = [math]::Round((($lat | Measure-Object -Maximum).Maximum), 2)
        }
    }

    $allLat = @($rows | ForEach-Object { [double]$_.LatencyMs })
    $durationSec = ($timeline.Count * 2.0)
    if ($durationSec -lt 1.0) { $durationSec = 1.0 }
    $requestDelta = [math]::Max(($afterHttpCount - $beforeHttpCount), 0.0)
    $throughput = [math]::Round(($requestDelta / $durationSec), 2)

    $epic1 = Read-JsonIfExists -Path "analysis/epic-tests/epic-1/baseline-summary.json"
    $epic2 = Read-JsonIfExists -Path "analysis/epic-tests/epic-2/epic-2-async-ab-summary.json"
    $epic3 = Read-JsonIfExists -Path "analysis/epic-tests/epic-3/epic-3-threadpool-tuning-summary.json"
    $epic4 = Read-JsonIfExists -Path "analysis/epic-tests/epic-4/epic-4-optimization-summary.json"

    $final = [ordered]@{
        generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        profile = $Profile
        users = $Users
        requestsPerUser = $RequestsPerUser
        tomcatMaxThreads = $TomcatMaxThreads
        endpointMetrics = $summary
        overall = [ordered]@{
            requests = $rows.Count
            errorCount = @($rows | Where-Object { -not $_.Success }).Count
            avgMs = [math]::Round((($allLat | Measure-Object -Average).Average), 2)
            p95Ms = [math]::Round((Percentile -Values $allLat -P 95), 2)
            throughputReqPerSec = $throughput
        }
        runtimeMetrics = [ordered]@{
            httpServerRequestsCount = [ordered]@{
                before = [math]::Round($beforeHttpCount, 2)
                after = [math]::Round($afterHttpCount, 2)
                delta = [math]::Round(($afterHttpCount - $beforeHttpCount), 2)
            }
            heapUsedBytes = [ordered]@{
                before = [math]::Round($beforeHeapUsedBytes, 2)
                after = [math]::Round($afterHeapUsedBytes, 2)
                delta = [math]::Round(($afterHeapUsedBytes - $beforeHeapUsedBytes), 2)
            }
            threadsLive = [ordered]@{
                before = [math]::Round($beforeThreads, 2)
                after = [math]::Round($afterThreads, 2)
                delta = [math]::Round(($afterThreads - $beforeThreads), 2)
            }
        }
        visualization = [ordered]@{
            prometheusBeforeCaptured = $promBeforeOk
            prometheusAfterCaptured = $promAfterOk
            timelineCsv = $timelineCsv
        }
        priorEpicSummary = [ordered]@{
            epic1BaselineOverallAvgMs = if ($epic1) { [math]::Round(((
                            [double]$epic1.endpoints.post_retrieval.avgMs +
                            [double]$epic1.endpoints.comment_loading.avgMs +
                            [double]$epic1.endpoints.user_analytics.avgMs) / 3.0), 2) } else { 0.0 }
            epic2AsyncOverallAvgMs = if ($epic2) { [double]$epic2.overallAsyncVsSync.async.avgMs } else { 0.0 }
            epic3BestOverallAvgMs = if ($epic3) {
                $best = $epic3.evaluatedConfigs | Sort-Object errorCount, overallP95Ms, overallAvgMs | Select-Object -First 1
                [double]$best.overallAvgMs
            } else { 0.0 }
            epic4OptimizedOverallAvgMs = if ($epic4) { [double]$epic4.overall.optimized.avgMs } else { 0.0 }
            epic5OverallAvgMs = [math]::Round((($allLat | Measure-Object -Average).Average), 2)
        }
        artifacts = [ordered]@{
            rawCsv = $rawCsv
            summaryJson = $summaryJson
            report = $reportMd
            timelineCsv = $timelineCsv
            promBefore = $promBeforePath
            promAfter = $promAfterPath
            appLog = $appLog
            appErrLog = $appErr
        }
    }
    $final | ConvertTo-Json -Depth 8 | Set-Content -Path $summaryJson

    $lines = @()
    $lines += "# Epic 5 Metrics Collection and Reporting"
    $lines += ""
    $lines += "Generated: " + (Get-Date).ToString("u")
    $lines += ""
    $lines += "## Endpoint Latency Summary"
    $lines += "| Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | Max (ms) |"
    $lines += "|---|---:|---:|---:|---:|---:|---:|"
    foreach ($endpoint in $endpointMap.Keys) {
        $s = $summary[$endpoint]
        $lines += "| $endpoint | $($s.requests) | $($s.errorCount) | $($s.avgMs) | $($s.p50Ms) | $($s.p95Ms) | $($s.maxMs) |"
    }
    $lines += ""
    $lines += "## Runtime Metrics (Actuator)"
    $lines += "| Metric | Before | After | Delta |"
    $lines += "|---|---:|---:|---:|"
    $lines += "| http.server.requests COUNT | $($final.runtimeMetrics.httpServerRequestsCount.before) | $($final.runtimeMetrics.httpServerRequestsCount.after) | $($final.runtimeMetrics.httpServerRequestsCount.delta) |"
    $lines += "| jvm.memory.used VALUE (bytes) | $($final.runtimeMetrics.heapUsedBytes.before) | $($final.runtimeMetrics.heapUsedBytes.after) | $($final.runtimeMetrics.heapUsedBytes.delta) |"
    $lines += "| jvm.threads.live VALUE | $($final.runtimeMetrics.threadsLive.before) | $($final.runtimeMetrics.threadsLive.after) | $($final.runtimeMetrics.threadsLive.delta) |"
    $lines += ""
    $lines += "## Throughput"
    $lines += "- Estimated throughput (request delta / sampling window): **$($final.overall.throughputReqPerSec) req/s**"
    $lines += ""
    $lines += "## Cross-Epic Average Latency Comparison"
    $lines += "| Stage | Overall Avg Latency (ms) |"
    $lines += "|---|---:|"
    $lines += "| Epic 1 baseline | $($final.priorEpicSummary.epic1BaselineOverallAvgMs) |"
    $lines += "| Epic 2 async mode | $($final.priorEpicSummary.epic2AsyncOverallAvgMs) |"
    $lines += "| Epic 3 best tuned config | $($final.priorEpicSummary.epic3BestOverallAvgMs) |"
    $lines += "| Epic 4 optimized mode | $($final.priorEpicSummary.epic4OptimizedOverallAvgMs) |"
    $lines += "| Epic 5 current run | $($final.priorEpicSummary.epic5OverallAvgMs) |"
    $lines += ""
    $lines += "## Evidence Artifacts"
    $lines += "- Raw latency: $rawCsv"
    $lines += "- Summary JSON: $summaryJson"
    $lines += "- Timeline CSV: $timelineCsv"
    $lines += "- Prometheus before: $promBeforePath"
    $lines += "- Prometheus after: $promAfterPath"
    $lines += "- App logs: $appLog, $appErr"
    $lines | Set-Content -Path $reportMd

    Write-Host "Epic 5 metrics collection complete."
    Write-Host "Summary: $summaryJson"
    Write-Host "Report:  $reportMd"
} finally {
    if ($proc -and -not $proc.HasExited) {
        & cmd /c "taskkill /PID $($proc.Id) /T /F" | Out-Null
    }
}

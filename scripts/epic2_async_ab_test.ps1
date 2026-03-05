param(
    [string]$Profile = "local",
    [int]$Port = 8092,
    [int]$Users = 20,
    [int]$RequestsPerUser = 15,
    [int]$TomcatMaxThreads = 8,
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

function Get-AvailablePort {
    param([int]$RequestedPort)
    for ($p = $RequestedPort; $p -lt ($RequestedPort + 50); $p++) {
        try {
            $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $p)
            $listener.Start()
            $listener.Stop()
            return $p
        } catch {
            continue
        }
    }
    throw "Unable to find a free port near $RequestedPort"
}

function Wait-ForStartup {
    param(
        [System.Diagnostics.Process]$Proc,
        [string]$LogPath,
        [int]$TimeoutSeconds = 150
    )
    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Path $LogPath) {
            $tail = Get-Content $LogPath -Tail 80 -ErrorAction SilentlyContinue
            if ($tail -match "Started SmartBlogApplication") { return $true }
        }
        if ($Proc.HasExited) { return $false }
    }
    return $false
}

function New-AuthToken {
    param([string]$BaseUrl)
    $u = "perf_epic2_" + (Get-Date -Format "yyyyMMddHHmmssfff")
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

function Run-Mode {
    param(
        [string]$ModeName,
        [bool]$AsyncEnabled,
        [int]$RunPort,
        [string]$ProfileName,
        [int]$UsersCount,
        [int]$ReqPerUser,
        [int]$TomcatThreads,
        [string]$OutputDir
    )
    $stamp = Get-Date -Format "yyyyMMddHHmmss"
    $modeLog = Join-Path $OutputDir ("epic-2-" + $ModeName + "-app-" + $stamp + ".log")
    $modeErr = Join-Path $OutputDir ("epic-2-" + $ModeName + "-app-" + $stamp + ".err.log")

    $runArgs = "--server.port=$RunPort --app.async.enabled=$($AsyncEnabled.ToString().ToLower()) --server.tomcat.threads.max=$TomcatThreads"
    $mvnArgs = @(
        "spring-boot:run",
        "-Dspring-boot.run.profiles=$ProfileName",
        "-Dspring-boot.run.arguments=`"$runArgs`""
    )

    Write-Host "Starting mode '$ModeName' (async=$AsyncEnabled) on port $RunPort ..."
    $proc = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $mvnArgs -PassThru -RedirectStandardOutput $modeLog -RedirectStandardError $modeErr

    try {
        $started = Wait-ForStartup -Proc $proc -LogPath $modeLog -TimeoutSeconds 180
        if (-not $started) {
            throw "App did not start for mode '$ModeName'. See $modeLog"
        }

        $baseUrl = "http://localhost:$RunPort"
        $token = New-AuthToken -BaseUrl $baseUrl

        $endpointMap = [ordered]@{
            post_retrieval = "$baseUrl/api/posts?page=0&size=20"
            comment_loading = "$baseUrl/api/comments/post/1?page=0&size=20"
            user_analytics = "$baseUrl/api/reviews/post/1/stats"
        }

        $rows = New-Object System.Collections.Generic.List[object]
        foreach ($name in $endpointMap.Keys) {
            Write-Host "[$ModeName] Running $name ..."
            $result = Invoke-ConcurrentLoad -Uri $endpointMap[$name] -Token $token -UsersCount $UsersCount -ReqPerUser $ReqPerUser
            foreach ($r in $result) {
                $rows.Add([pscustomobject]@{
                    Mode = $ModeName
                    AsyncEnabled = $AsyncEnabled
                    Endpoint = $name
                    LatencyMs = $r.LatencyMs
                    StatusCode = $r.StatusCode
                    Success = $r.Success
                    TimestampUtc = (Get-Date).ToUniversalTime().ToString("o")
                })
            }
        }

        return [pscustomobject]@{
            Rows = $rows
            AppLog = $modeLog
            AppErrLog = $modeErr
            Port = $RunPort
            AsyncEnabled = $AsyncEnabled
        }
    } finally {
        if ($proc -and -not $proc.HasExited) {
            & cmd /c "taskkill /PID $($proc.Id) /T /F" | Out-Null
        }
    }
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
Load-DotEnv
Remove-Item Env:MAVEN_OPTS -ErrorAction SilentlyContinue
if (-not $env:SPRING_DATASOURCE_URL -and $env:DB_URL) { $env:SPRING_DATASOURCE_URL = $env:DB_URL }
if (-not $env:SPRING_DATASOURCE_USERNAME -and $env:DB_USERNAME) { $env:SPRING_DATASOURCE_USERNAME = $env:DB_USERNAME }
if (-not $env:SPRING_DATASOURCE_PASSWORD -and $env:DB_PASSWORD) { $env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD }

$rawCsv = Join-Path $OutDir "epic-2-async-ab-raw.csv"
$summaryJson = Join-Path $OutDir "epic-2-async-ab-summary.json"
$reportMd = Join-Path $OutDir "epic-2-async-ab-report.md"

$syncPort = Get-AvailablePort -RequestedPort $Port
$asyncPort = Get-AvailablePort -RequestedPort ($syncPort + 1)

$syncRun = Run-Mode -ModeName "sync" -AsyncEnabled:$false -RunPort $syncPort -ProfileName $Profile -UsersCount $Users -ReqPerUser $RequestsPerUser -TomcatThreads $TomcatMaxThreads -OutputDir $OutDir
$asyncRun = Run-Mode -ModeName "async" -AsyncEnabled:$true -RunPort $asyncPort -ProfileName $Profile -UsersCount $Users -ReqPerUser $RequestsPerUser -TomcatThreads $TomcatMaxThreads -OutputDir $OutDir

$allRows = New-Object System.Collections.Generic.List[object]
$syncRun.Rows | ForEach-Object { $allRows.Add($_) }
$asyncRun.Rows | ForEach-Object { $allRows.Add($_) }
$allRows | Export-Csv -NoTypeInformation -Path $rawCsv

$summary = [ordered]@{}
foreach ($mode in @("sync", "async")) {
    $summary[$mode] = [ordered]@{}
    foreach ($endpoint in @("post_retrieval", "comment_loading", "user_analytics")) {
        $slice = $allRows | Where-Object { $_.Mode -eq $mode -and $_.Endpoint -eq $endpoint }
        $lat = @($slice | ForEach-Object { [double]$_.LatencyMs })
        $okCount = @($slice | Where-Object { $_.Success }).Count
        $summary[$mode][$endpoint] = [ordered]@{
            requests = $slice.Count
            successCount = $okCount
            errorCount = $slice.Count - $okCount
            avgMs = [math]::Round((($lat | Measure-Object -Average).Average), 2)
            p50Ms = [math]::Round((Percentile -Values $lat -P 50), 2)
            p95Ms = [math]::Round((Percentile -Values $lat -P 95), 2)
            maxMs = [math]::Round((($lat | Measure-Object -Maximum).Maximum), 2)
        }
    }
}

$comparison = [ordered]@{}
foreach ($endpoint in @("post_retrieval", "comment_loading", "user_analytics")) {
    $syncAvg = [double]$summary.sync.$endpoint.avgMs
    $asyncAvg = [double]$summary.async.$endpoint.avgMs
    $delta = [math]::Round(($asyncAvg - $syncAvg), 2)
    $improvement = if ($syncAvg -gt 0) { [math]::Round((($syncAvg - $asyncAvg) / $syncAvg) * 100, 2) } else { 0 }
    $comparison[$endpoint] = [ordered]@{
        syncAvgMs = $syncAvg
        asyncAvgMs = $asyncAvg
        deltaMs = $delta
        improvementPercent = $improvement
    }
}

$syncAll = @($allRows | Where-Object { $_.Mode -eq "sync" } | ForEach-Object { [double]$_.LatencyMs })
$asyncAll = @($allRows | Where-Object { $_.Mode -eq "async" } | ForEach-Object { [double]$_.LatencyMs })
$overall = [ordered]@{
    sync = [ordered]@{
        requests = $syncAll.Count
        avgMs = [math]::Round((($syncAll | Measure-Object -Average).Average), 2)
        p50Ms = [math]::Round((Percentile -Values $syncAll -P 50), 2)
        p95Ms = [math]::Round((Percentile -Values $syncAll -P 95), 2)
    }
    async = [ordered]@{
        requests = $asyncAll.Count
        avgMs = [math]::Round((($asyncAll | Measure-Object -Average).Average), 2)
        p50Ms = [math]::Round((Percentile -Values $asyncAll -P 50), 2)
        p95Ms = [math]::Round((Percentile -Values $asyncAll -P 95), 2)
    }
}
$overallDelta = [math]::Round(($overall.async.avgMs - $overall.sync.avgMs), 2)
$overallImprovement = if ($overall.sync.avgMs -gt 0) {
    [math]::Round((($overall.sync.avgMs - $overall.async.avgMs) / $overall.sync.avgMs) * 100, 2)
} else {
    0
}
$overall["deltaMs"] = $overallDelta
$overall["improvementPercent"] = $overallImprovement

$final = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    profile = $Profile
    users = $Users
    requestsPerUser = $RequestsPerUser
    tomcatMaxThreads = $TomcatMaxThreads
    ports = [ordered]@{
        sync = $syncRun.Port
        async = $asyncRun.Port
    }
    endpointsByMode = $summary
    asyncVsSync = $comparison
    overallAsyncVsSync = $overall
    artifacts = [ordered]@{
        rawCsv = $rawCsv
        syncAppLog = $syncRun.AppLog
        syncAppErrLog = $syncRun.AppErrLog
        asyncAppLog = $asyncRun.AppLog
        asyncAppErrLog = $asyncRun.AppErrLog
    }
}
$final | ConvertTo-Json -Depth 7 | Set-Content -Path $summaryJson

$lines = @()
$lines += "# Epic 2 Async A/B Comparison Report"
$lines += ""
$lines += "Generated: " + (Get-Date).ToString("u")
$lines += "Load profile: users=$Users, requestsPerUser=$RequestsPerUser, tomcatMaxThreads=$TomcatMaxThreads"
$lines += ""
$lines += "## Per-Mode Metrics"
$lines += "| Mode | Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | Max (ms) |"
$lines += "|---|---|---:|---:|---:|---:|---:|---:|"
foreach ($mode in @("sync", "async")) {
    foreach ($endpoint in @("post_retrieval", "comment_loading", "user_analytics")) {
        $s = $summary[$mode][$endpoint]
        $lines += "| $mode | $endpoint | $($s.requests) | $($s.errorCount) | $($s.avgMs) | $($s.p50Ms) | $($s.p95Ms) | $($s.maxMs) |"
    }
}
$lines += ""
$lines += "## Async vs Sync (Average Latency)"
$lines += "| Endpoint | Sync Avg (ms) | Async Avg (ms) | Delta (ms) | Improvement % |"
$lines += "|---|---:|---:|---:|---:|"
foreach ($endpoint in @("post_retrieval", "comment_loading", "user_analytics")) {
    $c = $comparison[$endpoint]
    $lines += "| $endpoint | $($c.syncAvgMs) | $($c.asyncAvgMs) | $($c.deltaMs) | $($c.improvementPercent) |"
}
$lines += ""
$lines += "## Overall Async vs Sync"
$lines += "| Scope | Sync Avg (ms) | Async Avg (ms) | Delta (ms) | Improvement % | Sync P95 (ms) | Async P95 (ms) |"
$lines += "|---|---:|---:|---:|---:|---:|---:|"
$lines += "| All endpoints | $($overall.sync.avgMs) | $($overall.async.avgMs) | $overallDelta | $overallImprovement | $($overall.sync.p95Ms) | $($overall.async.p95Ms) |"
$lines | Set-Content -Path $reportMd

Write-Host "Epic 2 async A/B test complete."
Write-Host "Summary: $summaryJson"
Write-Host "Report:  $reportMd"

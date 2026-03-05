param(
    [string]$Profile = "local",
    [int]$Port = 8130,
    [int]$Users = 10,
    [int]$RequestsPerUser = 20,
    [int]$TomcatMaxThreads = 8,
    [string]$OutDir = "analysis/epic-tests/epic-4"
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
            $tail = Get-Content $LogPath -Tail 100 -ErrorAction SilentlyContinue
            if ($tail -match "Started SmartBlogApplication") { return $true }
        }
        if ($Proc.HasExited) { return $false }
    }
    return $false
}

function New-AuthToken {
    param([string]$BaseUrl)
    $u = "perf_epic4_" + (Get-Date -Format "yyyyMMddHHmmssfff")
    $email = "$u@example.com"
    $pwd = "PerfTest123!"
    $registerBody = @{ username = $u; email = $email; password = $pwd; role = "AUTHOR" } | ConvertTo-Json
    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/register" -ContentType "application/json" -Body $registerBody | Out-Null
    } catch {}
    $loginBody = @{ username = $u; password = $pwd } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType "application/json" -Body $loginBody
    if (-not $login.token) { throw "Could not get JWT token for Epic 4 test." }
    return [string]$login.token
}

function Invoke-ConcurrentLoad {
    param([string]$Uri, [string]$Token, [int]$UsersCount, [int]$ReqPerUser, [int]$TimeoutSec = 30)

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
        [int]$RunPort,
        [string]$ProfileName,
        [int]$UsersCount,
        [int]$ReqPerUser,
        [int]$TomcatThreads,
        [string]$OutputDir,
        [string]$CacheType,
        [bool]$SingleQueryEnabled,
        [bool]$CachingEnabled
    )
    $stamp = Get-Date -Format "yyyyMMddHHmmss"
    $modeLog = Join-Path $OutputDir ("epic-4-" + $ModeName + "-app-" + $stamp + ".log")
    $modeErr = Join-Path $OutputDir ("epic-4-" + $ModeName + "-app-" + $stamp + ".err.log")

    $runArgs = "--server.port=$RunPort --server.tomcat.threads.max=$TomcatThreads --app.async.enabled=true --spring.cache.type=$CacheType --app.optimization.reviewStats.singleQuery=$($SingleQueryEnabled.ToString().ToLower()) --app.optimization.caching.enabled=$($CachingEnabled.ToString().ToLower())"
    $mvnArgs = @(
        "spring-boot:run",
        "-Dspring-boot.run.profiles=$ProfileName",
        "-Dspring-boot.run.arguments=`"$runArgs`""
    )

    Write-Host "Starting mode '$ModeName' (cache=$CacheType, singleQuery=$SingleQueryEnabled) ..."
    $proc = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $mvnArgs -PassThru -RedirectStandardOutput $modeLog -RedirectStandardError $modeErr
    try {
        if (-not (Wait-ForStartup -Proc $proc -LogPath $modeLog)) {
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
            Write-Host "[$ModeName] Warmup $name ..."
            Invoke-ConcurrentLoad -Uri $endpointMap[$name] -Token $token -UsersCount 2 -ReqPerUser 5 | Out-Null
            Write-Host "[$ModeName] Measuring $name ..."
            $result = Invoke-ConcurrentLoad -Uri $endpointMap[$name] -Token $token -UsersCount $UsersCount -ReqPerUser $ReqPerUser
            foreach ($r in $result) {
                $rows.Add([pscustomobject]@{
                    Mode = $ModeName
                    Endpoint = $name
                    LatencyMs = $r.LatencyMs
                    StatusCode = $r.StatusCode
                    Success = $r.Success
                    TimestampUtc = (Get-Date).ToUniversalTime().ToString("o")
                })
            }
        }

        return [pscustomobject]@{
            Mode = $ModeName
            Rows = $rows
            AppLog = $modeLog
            AppErrLog = $modeErr
            Port = $RunPort
            CacheType = $CacheType
            SingleQuery = $SingleQueryEnabled
            CachingEnabled = $CachingEnabled
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

$rawCsv = Join-Path $OutDir "epic-4-optimization-raw.csv"
$summaryJson = Join-Path $OutDir "epic-4-optimization-summary.json"
$reportMd = Join-Path $OutDir "epic-4-optimization-report.md"

$baselineRun = Run-Mode -ModeName "baseline" -RunPort $Port -ProfileName $Profile -UsersCount $Users -ReqPerUser $RequestsPerUser -TomcatThreads $TomcatMaxThreads -OutputDir $OutDir -CacheType "caffeine" -SingleQueryEnabled:$false -CachingEnabled:$false
$optimizedRun = Run-Mode -ModeName "optimized" -RunPort ($Port + 1) -ProfileName $Profile -UsersCount $Users -ReqPerUser $RequestsPerUser -TomcatThreads $TomcatMaxThreads -OutputDir $OutDir -CacheType "caffeine" -SingleQueryEnabled:$true -CachingEnabled:$true

$allRows = New-Object System.Collections.Generic.List[object]
$baselineRun.Rows | ForEach-Object { $allRows.Add($_) }
$optimizedRun.Rows | ForEach-Object { $allRows.Add($_) }
$allRows | Export-Csv -NoTypeInformation -Path $rawCsv

$summary = [ordered]@{}
foreach ($mode in @("baseline", "optimized")) {
    $summary[$mode] = [ordered]@{}
    foreach ($endpoint in @("post_retrieval", "comment_loading", "user_analytics")) {
        $slice = $allRows | Where-Object { $_.Mode -eq $mode -and $_.Endpoint -eq $endpoint }
        $lat = @($slice | ForEach-Object { [double]$_.LatencyMs })
        $ok = @($slice | Where-Object { $_.Success }).Count
        $summary[$mode][$endpoint] = [ordered]@{
            requests = $slice.Count
            errorCount = $slice.Count - $ok
            avgMs = [math]::Round((($lat | Measure-Object -Average).Average), 2)
            p50Ms = [math]::Round((Percentile -Values $lat -P 50), 2)
            p95Ms = [math]::Round((Percentile -Values $lat -P 95), 2)
        }
    }
}

$comparison = [ordered]@{}
foreach ($endpoint in @("post_retrieval", "comment_loading", "user_analytics")) {
    $baseAvg = [double]$summary.baseline.$endpoint.avgMs
    $optAvg = [double]$summary.optimized.$endpoint.avgMs
    $delta = [math]::Round(($optAvg - $baseAvg), 2)
    $improvement = if ($baseAvg -gt 0) { [math]::Round((($baseAvg - $optAvg) / $baseAvg) * 100, 2) } else { 0 }
    $comparison[$endpoint] = [ordered]@{
        baselineAvgMs = $baseAvg
        optimizedAvgMs = $optAvg
        deltaMs = $delta
        improvementPercent = $improvement
    }
}

$baselineLat = @($allRows | Where-Object { $_.Mode -eq "baseline" } | ForEach-Object { [double]$_.LatencyMs })
$optimizedLat = @($allRows | Where-Object { $_.Mode -eq "optimized" } | ForEach-Object { [double]$_.LatencyMs })
$overall = [ordered]@{
    baseline = [ordered]@{
        avgMs = [math]::Round((($baselineLat | Measure-Object -Average).Average), 2)
        p95Ms = [math]::Round((Percentile -Values $baselineLat -P 95), 2)
    }
    optimized = [ordered]@{
        avgMs = [math]::Round((($optimizedLat | Measure-Object -Average).Average), 2)
        p95Ms = [math]::Round((Percentile -Values $optimizedLat -P 95), 2)
    }
}
$overallDelta = [math]::Round(($overall.optimized.avgMs - $overall.baseline.avgMs), 2)
$overallImprovement = if ($overall.baseline.avgMs -gt 0) {
    [math]::Round((($overall.baseline.avgMs - $overall.optimized.avgMs) / $overall.baseline.avgMs) * 100, 2)
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
    endpointsByMode = $summary
    optimizedVsBaseline = $comparison
    overall = $overall
    artifacts = [ordered]@{
        rawCsv = $rawCsv
        baselineLog = $baselineRun.AppLog
        optimizedLog = $optimizedRun.AppLog
    }
}
$final | ConvertTo-Json -Depth 7 | Set-Content -Path $summaryJson

$lines = @()
$lines += "# Epic 4 Data Optimization Comparison Report"
$lines += ""
$lines += "Generated: " + (Get-Date).ToString("u")
$lines += ""
$lines += "| Mode | Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) |"
$lines += "|---|---|---:|---:|---:|---:|---:|"
foreach ($mode in @("baseline", "optimized")) {
    foreach ($endpoint in @("post_retrieval", "comment_loading", "user_analytics")) {
        $s = $summary[$mode][$endpoint]
        $lines += "| $mode | $endpoint | $($s.requests) | $($s.errorCount) | $($s.avgMs) | $($s.p50Ms) | $($s.p95Ms) |"
    }
}
$lines += ""
$lines += "## Optimized vs Baseline (Average)"
$lines += "| Endpoint | Baseline Avg (ms) | Optimized Avg (ms) | Delta (ms) | Improvement % |"
$lines += "|---|---:|---:|---:|---:|"
foreach ($endpoint in @("post_retrieval", "comment_loading", "user_analytics")) {
    $c = $comparison[$endpoint]
    $lines += "| $endpoint | $($c.baselineAvgMs) | $($c.optimizedAvgMs) | $($c.deltaMs) | $($c.improvementPercent) |"
}
$lines += ""
$lines += "## Overall"
$lines += "| Baseline Avg (ms) | Optimized Avg (ms) | Delta (ms) | Improvement % | Baseline P95 (ms) | Optimized P95 (ms) |"
$lines += "|---:|---:|---:|---:|---:|---:|"
$lines += "| $($overall.baseline.avgMs) | $($overall.optimized.avgMs) | $overallDelta | $overallImprovement | $($overall.baseline.p95Ms) | $($overall.optimized.p95Ms) |"
$lines | Set-Content -Path $reportMd

Write-Host "Epic 4 optimization comparison complete."
Write-Host "Summary: $summaryJson"
Write-Host "Report:  $reportMd"

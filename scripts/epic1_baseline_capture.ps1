param(
    [string]$Profile = "local",
    [int]$Port = 8085,
    [int]$WarmupIterations = 5,
    [int]$MeasureIterations = 25,
    [string]$OutDir = "analysis/epic-tests/epic-1"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Test-PortAvailable {
    param([int]$CandidatePort)
    $listener = $null
    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $CandidatePort)
        $listener.Start()
        return $true
    } catch {
        return $false
    } finally {
        if ($listener) { $listener.Stop() }
    }
}

function Resolve-FreePort {
    param([int]$PreferredPort)
    if (Test-PortAvailable -CandidatePort $PreferredPort) { return $PreferredPort }
    for ($p = $PreferredPort + 1; $p -le ($PreferredPort + 200); $p++) {
        if (Test-PortAvailable -CandidatePort $p) { return $p }
    }
    throw "No free port found in range $PreferredPort..$($PreferredPort + 200)"
}

function Load-DotEnv {
    param([string]$Path = ".env")
    if (-not (Test-Path $Path)) {
        Write-Host "No .env file found at $Path. Continuing with current environment."
        return
    }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) { return }
        if ($line -notmatch "^[A-Za-z_][A-Za-z0-9_]*=") { return }
        $name, $value = $line -split "=", 2
        Set-Item -Path ("Env:" + $name) -Value $value
    }
}

function Percentile {
    param(
        [double[]]$Values,
        [double]$P
    )
    if ($Values.Count -eq 0) { return 0.0 }
    $sorted = $Values | Sort-Object
    $rank = [math]::Ceiling(($P / 100.0) * $sorted.Count)
    if ($rank -lt 1) { $rank = 1 }
    if ($rank -gt $sorted.Count) { $rank = $sorted.Count }
    return [double]$sorted[$rank - 1]
}

function Ensure-AuthToken {
    param([string]$BaseUrl)
    $u = "perf_epic1_" + (Get-Date -Format "yyyyMMddHHmmss")
    $email = "$u@example.com"
    $pwd = "PerfTest123!"

    $registerBody = @{
        username = $u
        email = $email
        password = $pwd
        role = "AUTHOR"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/register" -ContentType "application/json" -Body $registerBody | Out-Null
    } catch {
        # If user already exists or auth controller unavailable, login may still succeed for existing credentials.
    }

    $loginBody = @{ username = $u; password = $pwd } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType "application/json" -Body $loginBody
    if (-not $login.token) {
        throw "Could not obtain JWT token from /auth/login."
    }
    return [string]$login.token
}

function Invoke-TimedApi {
    param(
        [string]$Uri,
        [hashtable]$Headers
    )
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $status = 0
    $ok = $false
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri $Uri -Headers $Headers -TimeoutSec 30
        $status = [int]$resp.StatusCode
        $ok = ($status -ge 200 -and $status -lt 300)
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $status = [int]$_.Exception.Response.StatusCode
        } else {
            $status = 0
        }
        $ok = $false
    } finally {
        $sw.Stop()
    }
    return [pscustomobject]@{
        LatencyMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
        StatusCode = $status
        Success = $ok
    }
}

function Parse-HealthFromJfr {
    param(
        [string]$JfrFile
    )
    $result = [ordered]@{
        AvgJvmUserCpu = ""
        AvgJvmSystemCpu = ""
        PeakCommittedHeapMb = ""
    }
    if (-not (Test-Path $JfrFile)) { return $result }
    if ((Get-Item $JfrFile).Length -le 0) { return $result }

    $cpuText = @()
    $heapText = @()
    try { $cpuText = & jfr print --events jdk.CPULoad $JfrFile 2>$null } catch {}
    try { $heapText = & jfr print --events jdk.GCHeapSummary $JfrFile 2>$null } catch {}
    $cpuUser = @()
    $cpuSys = @()
    $heapCommitted = @()

    foreach ($line in $cpuText) {
        if ($line -match "jvmUser = ([0-9.]+)") { $cpuUser += [double]$matches[1] }
        if ($line -match "jvmSystem = ([0-9.]+)") { $cpuSys += [double]$matches[1] }
    }
    foreach ($line in $heapText) {
        if ($line -match "heapUsed = ([0-9.]+) bytes") { $heapCommitted += ([double]$matches[1] / 1MB) }
    }
    if ($cpuUser.Count -gt 0) {
        $result.AvgJvmUserCpu = [math]::Round((($cpuUser | Measure-Object -Average).Average * 100), 2)
    }
    if ($cpuSys.Count -gt 0) {
        $result.AvgJvmSystemCpu = [math]::Round((($cpuSys | Measure-Object -Average).Average * 100), 2)
    }
    if ($heapCommitted.Count -gt 0) {
        $result.PeakCommittedHeapMb = [math]::Round((($heapCommitted | Measure-Object -Maximum).Maximum), 2)
    }
    return $result
}

function Parse-HeapUsedMbFromJcmd {
    param([string]$HeapInfoFile)
    if (-not (Test-Path $HeapInfoFile)) { return "" }
    $lines = Get-Content $HeapInfoFile
    foreach ($line in $lines) {
        if ($line -match "used\s+([0-9]+)K") {
            return [math]::Round(([double]$matches[1] / 1024.0), 2)
        }
    }
    return ""
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Load-DotEnv
# Prevent inherited global/session MAVEN_OPTS (for example StartFlightRecording)
# from polluting the app baseline recording with Maven bootstrap samples.
Remove-Item Env:MAVEN_OPTS -ErrorAction SilentlyContinue
if (-not $env:SPRING_DATASOURCE_URL -and $env:DB_URL) { $env:SPRING_DATASOURCE_URL = $env:DB_URL }
if (-not $env:SPRING_DATASOURCE_USERNAME -and $env:DB_USERNAME) { $env:SPRING_DATASOURCE_USERNAME = $env:DB_USERNAME }
if (-not $env:SPRING_DATASOURCE_PASSWORD -and $env:DB_PASSWORD) { $env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD }

$selectedPort = Resolve-FreePort -PreferredPort $Port
if ($selectedPort -ne $Port) {
    Write-Host "Requested port $Port is busy. Using free port $selectedPort instead."
}
$Port = $selectedPort

$jfrFile = Join-Path $OutDir "epic-1-baseline.jfr"
$appLog = Join-Path $OutDir "epic-1-app.log"
$appErrLog = Join-Path $OutDir "epic-1-app.err.log"
$cpuTxt = Join-Path $OutDir "jfr-cpu.txt"
$allocTxt = Join-Path $OutDir "jfr-alloc.txt"
$threadDump = Join-Path $OutDir "epic-1-threaddump.txt"
$heapInfo = Join-Path $OutDir "epic-1-heap-info.txt"
$rawCsv = Join-Path $OutDir "baseline-latency-raw.csv"
$summaryJson = Join-Path $OutDir "baseline-summary.json"
$reportMd = Join-Path $OutDir "epic-1-baseline-report.md"

if (Test-Path $jfrFile) { Remove-Item $jfrFile -Force }

$args = @(
    "spring-boot:run",
    "-Dspring-boot.run.profiles=$Profile",
    "-Dspring-boot.run.arguments=--server.port=$Port"
)

Write-Host "Starting app for Epic 1 baseline capture..."
$proc = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $args -PassThru -RedirectStandardOutput $appLog -RedirectStandardError $appErrLog

try {
    $started = $false
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Path $appLog) {
            $tail = Get-Content $appLog -Tail 30 -ErrorAction SilentlyContinue
            if ($tail -match "Started SmartBlogApplication") {
                $started = $true
                break
            }
        }
        if ($proc.HasExited) { break }
    }
    if (-not $started) { throw "Application did not start successfully. See $appLog" }

    $baseUrl = "http://localhost:$Port"
    $javaPid = ""
    $pidLine = (Get-Content $appLog -Tail 400 | Select-String -Pattern "with PID ([0-9]+)" | Select-Object -Last 1)
    if ($pidLine -and $pidLine.Matches.Count -gt 0) {
        $javaPid = $pidLine.Matches[0].Groups[1].Value
    }
    if (-not $javaPid) {
        throw "Could not detect Java PID from app log. See $appLog"
    }

    # Start an explicit JFR recording tied to the Spring Boot JVM, then dump it before shutdown.
    & jcmd $javaPid JFR.start name=Epic1 settings=profile | Out-Null

    $token = Ensure-AuthToken -BaseUrl $baseUrl
    $headers = @{ Authorization = "Bearer $token" }

    $endpointMap = [ordered]@{
        "post_retrieval" = "$baseUrl/api/posts?page=0&size=20"
        "comment_loading" = "$baseUrl/api/comments/post/1?page=0&size=20"
        "user_analytics" = "$baseUrl/api/reviews/post/1/stats"
    }

    Write-Host "Warmup phase..."
    foreach ($name in $endpointMap.Keys) {
        for ($i = 0; $i -lt $WarmupIterations; $i++) {
            [void](Invoke-TimedApi -Uri $endpointMap[$name] -Headers $headers)
        }
    }

    Write-Host "Measurement phase..."
    $rows = New-Object System.Collections.Generic.List[object]
    foreach ($name in $endpointMap.Keys) {
        for ($i = 1; $i -le $MeasureIterations; $i++) {
            $r = Invoke-TimedApi -Uri $endpointMap[$name] -Headers $headers
            $rows.Add([pscustomobject]@{
                Endpoint = $name
                Iteration = $i
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
            minMs = [math]::Round((($lat | Measure-Object -Minimum).Minimum), 2)
            maxMs = [math]::Round((($lat | Measure-Object -Maximum).Maximum), 2)
            p50Ms = [math]::Round((Percentile -Values $lat -P 50), 2)
            p95Ms = [math]::Round((Percentile -Values $lat -P 95), 2)
        }
    }

    & jcmd $javaPid Thread.print > $threadDump
    & jcmd $javaPid GC.heap_info > $heapInfo
    & jcmd $javaPid JFR.dump name=Epic1 filename="$jfrFile" | Out-Null
    & jcmd $javaPid JFR.stop name=Epic1 | Out-Null

    if ((Test-Path $jfrFile) -and ((Get-Item $jfrFile).Length -gt 0)) {
        try {
            & jfr print --events jdk.ExecutionSample $jfrFile > $cpuTxt
        } catch {
            Set-Content -Path $cpuTxt -Value "Unable to print ExecutionSample from JFR: $($_.Exception.Message)"
        }
        try {
            & jfr print --events jdk.ObjectAllocationSample $jfrFile > $allocTxt
        } catch {
            Set-Content -Path $allocTxt -Value "Unable to print ObjectAllocationSample from JFR: $($_.Exception.Message)"
        }
    } else {
        Set-Content -Path $cpuTxt -Value "JFR file missing or empty; CPU sample extraction skipped."
        Set-Content -Path $allocTxt -Value "JFR file missing or empty; allocation sample extraction skipped."
    }

    $jfrHealth = Parse-HealthFromJfr -JfrFile $jfrFile
    $heapUsedMb = Parse-HeapUsedMbFromJcmd -HeapInfoFile $heapInfo
    $final = [ordered]@{
        generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        profile = $Profile
        port = $Port
        warmupIterations = $WarmupIterations
        measureIterations = $MeasureIterations
        endpoints = $summary
        jfr = [ordered]@{
            file = $jfrFile
            avgJvmUserCpuPercent = $jfrHealth.AvgJvmUserCpu
            avgJvmSystemCpuPercent = $jfrHealth.AvgJvmSystemCpu
            peakHeapUsedMb = $jfrHealth.PeakCommittedHeapMb
            heapUsedMbFromJcmd = $heapUsedMb
        }
        artifacts = [ordered]@{
            appLog = $appLog
            appErrLog = $appErrLog
            threadDump = $threadDump
            heapInfo = $heapInfo
            rawLatencyCsv = $rawCsv
            executionSamples = $cpuTxt
            allocationSamples = $allocTxt
        }
    }
    $final | ConvertTo-Json -Depth 6 | Set-Content -Path $summaryJson

    $lines = @()
    $lines += "# Epic 1 Baseline Report"
    $lines += ""
    $lines += "Generated: " + (Get-Date).ToString("u")
    $lines += ""
    $lines += "| Scenario | Requests | Errors | Avg (ms) | p50 (ms) | p95 (ms) | Max (ms) |"
    $lines += "|---|---:|---:|---:|---:|---:|---:|"
    foreach ($k in $summary.Keys) {
        $s = $summary[$k]
        $lines += "| $k | $($s.requests) | $($s.errorCount) | $($s.avgMs) | $($s.p50Ms) | $($s.p95Ms) | $($s.maxMs) |"
    }
    $lines += ""
    $lines += "JFR CPU/Memory Snapshot:"
    $lines += "- Avg JVM user CPU (%): $($jfrHealth.AvgJvmUserCpu)"
    $lines += "- Avg JVM system CPU (%): $($jfrHealth.AvgJvmSystemCpu)"
    $lines += "- Peak heap used (MB): $($jfrHealth.PeakCommittedHeapMb)"
    $lines += "- Heap used from jcmd GC.heap_info (MB): $heapUsedMb"
    $lines += ""
    $lines += "Artifacts:"
    $lines += "- $jfrFile"
    $lines += "- $appLog"
    $lines += "- $appErrLog"
    $lines += "- $cpuTxt"
    $lines += "- $allocTxt"
    $lines += "- $threadDump"
    $lines += "- $heapInfo"
    $lines += "- $rawCsv"
    $lines += "- $summaryJson"
    $lines | Set-Content -Path $reportMd

    Write-Host "Epic 1 baseline capture complete."
    Write-Host "Summary: $summaryJson"
    Write-Host "Report:  $reportMd"
}
finally {
    if ($proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}

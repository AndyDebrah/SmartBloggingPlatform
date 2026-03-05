param(
    [string]$Profile = "local",
    [int]$StartPort = 8120,
    [int]$Users = 16,
    [int]$RequestsPerUser = 12,
    [int]$TomcatMaxThreads = 8,
    [string]$ConfigMatrix = "4:16:150;8:32:300;12:48:500",
    [string]$OutDir = "analysis/epic-tests/epic-3"
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
    $u = "perf_epic3_" + (Get-Date -Format "yyyyMMddHHmmssfff")
    $email = "$u@example.com"
    $pwd = "PerfTest123!"
    $registerBody = @{ username = $u; email = $email; password = $pwd; role = "AUTHOR" } | ConvertTo-Json
    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/register" -ContentType "application/json" -Body $registerBody | Out-Null
    } catch {}
    $loginBody = @{ username = $u; password = $pwd } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType "application/json" -Body $loginBody
    if (-not $login.token) { throw "Could not get JWT token for Epic 3 tuning test." }
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

function Get-JavaPidForPort {
    param([int]$Port)
    try {
        $conn = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction Stop | Select-Object -First 1
        if ($conn) { return [int]$conn.OwningProcess }
    } catch {}
    return $null
}

function Get-HeapUsedMb {
    param([int]$ProcessId)
    try {
        $text = & jcmd $ProcessId GC.heap_info 2>$null
        foreach ($line in $text) {
            if ($line -match "used\s+(\d+)K") {
                return [math]::Round(([double]$matches[1] / 1024.0), 2)
            }
        }
    } catch {}
    return 0.0
}

function Parse-Config {
    param([string]$Entry)
    $parts = $Entry.Split(":")
    if ($parts.Length -ne 3) {
        throw "Invalid config entry '$Entry'. Expected format core:max:queue."
    }
    return [pscustomobject]@{
        Core = [int]$parts[0]
        Max = [int]$parts[1]
        Queue = [int]$parts[2]
    }
}

function Run-Configuration {
    param(
        [int]$Core,
        [int]$Max,
        [int]$Queue,
        [int]$Port,
        [string]$ProfileName,
        [int]$UsersCount,
        [int]$ReqPerUser,
        [int]$TomcatThreads,
        [string]$OutputDir
    )
    $label = "c$Core-m$Max-q$Queue"
    $stamp = Get-Date -Format "yyyyMMddHHmmss"
    $appLog = Join-Path $OutputDir ("epic-3-" + $label + "-app-" + $stamp + ".log")
    $appErr = Join-Path $OutputDir ("epic-3-" + $label + "-app-" + $stamp + ".err.log")

    $runArgs = "--server.port=$Port --app.async.enabled=true --server.tomcat.threads.max=$TomcatThreads --app.async.corePoolSize=$Core --app.async.maxPoolSize=$Max --app.async.queueCapacity=$Queue"
    $mvnArgs = @(
        "spring-boot:run",
        "-Dspring-boot.run.profiles=$ProfileName",
        "-Dspring-boot.run.arguments=`"$runArgs`""
    )

    Write-Host "Starting config $label on port $Port ..."
    $proc = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $mvnArgs -PassThru -RedirectStandardOutput $appLog -RedirectStandardError $appErr
    try {
        if (-not (Wait-ForStartup -Proc $proc -LogPath $appLog)) {
            throw "App did not start for config $label. See $appLog"
        }

        $javaPid = Get-JavaPidForPort -Port $Port
        if (-not $javaPid) { throw "Could not find Java PID for port $Port" }

        $before = Get-Process -Id $javaPid
        $startCpuSeconds = [double]$before.CPU
        $coreCount = [int][Environment]::ProcessorCount
        $sw = [System.Diagnostics.Stopwatch]::StartNew()

        $baseUrl = "http://localhost:$Port"
        $token = New-AuthToken -BaseUrl $baseUrl
        $endpointMap = [ordered]@{
            post_retrieval = "$baseUrl/api/posts?page=0&size=20"
            comment_loading = "$baseUrl/api/comments/post/1?page=0&size=20"
            user_analytics = "$baseUrl/api/reviews/post/1/stats"
        }

        $rows = New-Object System.Collections.Generic.List[object]
        foreach ($name in $endpointMap.Keys) {
            Write-Host "[$label] Running $name ..."
            $result = Invoke-ConcurrentLoad -Uri $endpointMap[$name] -Token $token -UsersCount $UsersCount -ReqPerUser $ReqPerUser
            foreach ($r in $result) {
                $rows.Add([pscustomobject]@{
                    Config = $label
                    Endpoint = $name
                    LatencyMs = $r.LatencyMs
                    StatusCode = $r.StatusCode
                    Success = $r.Success
                    TimestampUtc = (Get-Date).ToUniversalTime().ToString("o")
                })
            }
        }
        $sw.Stop()

        $after = Get-Process -Id $javaPid
        $endCpuSeconds = [double]$after.CPU
        $durationSec = [Math]::Max($sw.Elapsed.TotalSeconds, 0.001)
        $cpuPct = [math]::Round((($endCpuSeconds - $startCpuSeconds) / ($durationSec * $coreCount)) * 100.0, 2)
        $workingSetMb = [math]::Round(($after.WorkingSet64 / 1MB), 2)
        $threadCount = $after.Threads.Count
        $heapUsedMb = Get-HeapUsedMb -ProcessId $javaPid

        $overallLat = @($rows | ForEach-Object { [double]$_.LatencyMs })
        $overallErrors = @($rows | Where-Object { -not $_.Success }).Count
        $endpointSummary = [ordered]@{}
        foreach ($name in $endpointMap.Keys) {
            $slice = $rows | Where-Object { $_.Endpoint -eq $name }
            $lat = @($slice | ForEach-Object { [double]$_.LatencyMs })
            $ok = @($slice | Where-Object { $_.Success }).Count
            $endpointSummary[$name] = [ordered]@{
                requests = $slice.Count
                errorCount = $slice.Count - $ok
                avgMs = [math]::Round((($lat | Measure-Object -Average).Average), 2)
                p50Ms = [math]::Round((Percentile -Values $lat -P 50), 2)
                p95Ms = [math]::Round((Percentile -Values $lat -P 95), 2)
            }
        }

        return [pscustomobject]@{
            Config = $label
            CorePoolSize = $Core
            MaxPoolSize = $Max
            QueueCapacity = $Queue
            Port = $Port
            Requests = $rows.Count
            ErrorCount = $overallErrors
            OverallAvgMs = [math]::Round((($overallLat | Measure-Object -Average).Average), 2)
            OverallP50Ms = [math]::Round((Percentile -Values $overallLat -P 50), 2)
            OverallP95Ms = [math]::Round((Percentile -Values $overallLat -P 95), 2)
            CpuUtilizationPercent = $cpuPct
            WorkingSetMb = $workingSetMb
            HeapUsedMb = $heapUsedMb
            ThreadCount = $threadCount
            DurationSec = [math]::Round($durationSec, 2)
            AppLog = $appLog
            AppErrLog = $appErr
            EndpointSummary = $endpointSummary
            Rows = $rows
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

$configs = @()
foreach ($entry in $ConfigMatrix.Split(";", [System.StringSplitOptions]::RemoveEmptyEntries)) {
    $configs += Parse-Config -Entry $entry.Trim()
}

$rawCsv = Join-Path $OutDir "epic-3-threadpool-tuning-raw.csv"
$summaryJson = Join-Path $OutDir "epic-3-threadpool-tuning-summary.json"
$reportMd = Join-Path $OutDir "epic-3-threadpool-tuning-report.md"

$allRows = New-Object System.Collections.Generic.List[object]
$results = New-Object System.Collections.Generic.List[object]
$port = $StartPort

foreach ($cfg in $configs) {
    $run = Run-Configuration -Core $cfg.Core -Max $cfg.Max -Queue $cfg.Queue -Port $port -ProfileName $Profile -UsersCount $Users -ReqPerUser $RequestsPerUser -TomcatThreads $TomcatMaxThreads -OutputDir $OutDir
    $results.Add($run)
    $run.Rows | ForEach-Object { $allRows.Add($_) }
    $port++
}

$allRows | Export-Csv -NoTypeInformation -Path $rawCsv

$ranked = $results | Sort-Object @{Expression = "ErrorCount"; Ascending = $true}, @{Expression = "OverallP95Ms"; Ascending = $true}, @{Expression = "OverallAvgMs"; Ascending = $true}
$best = $ranked | Select-Object -First 1

$summary = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    profile = $Profile
    users = $Users
    requestsPerUser = $RequestsPerUser
    tomcatMaxThreads = $TomcatMaxThreads
    evaluatedConfigs = @($results | ForEach-Object {
            [ordered]@{
                config = $_.Config
                corePoolSize = $_.CorePoolSize
                maxPoolSize = $_.MaxPoolSize
                queueCapacity = $_.QueueCapacity
                requests = $_.Requests
                errorCount = $_.ErrorCount
                overallAvgMs = $_.OverallAvgMs
                overallP50Ms = $_.OverallP50Ms
                overallP95Ms = $_.OverallP95Ms
                cpuUtilizationPercent = $_.CpuUtilizationPercent
                workingSetMb = $_.WorkingSetMb
                heapUsedMb = $_.HeapUsedMb
                threadCount = $_.ThreadCount
                durationSec = $_.DurationSec
                appLog = $_.AppLog
                appErrLog = $_.AppErrLog
                endpointSummary = $_.EndpointSummary
            }
        })
    selectedOptimalConfig = [ordered]@{
        config = $best.Config
        corePoolSize = $best.CorePoolSize
        maxPoolSize = $best.MaxPoolSize
        queueCapacity = $best.QueueCapacity
        selectionReason = "Lowest error count, then lowest overall p95, then lowest overall average latency."
    }
    artifacts = [ordered]@{
        rawCsv = $rawCsv
        report = $reportMd
    }
}
$summary | ConvertTo-Json -Depth 8 | Set-Content -Path $summaryJson

$lines = @()
$lines += "# Epic 3 Thread Pool Tuning Report"
$lines += ""
$lines += "Generated: " + (Get-Date).ToString("u")
$lines += "Load profile: users=$Users, requestsPerUser=$RequestsPerUser, tomcatMaxThreads=$TomcatMaxThreads"
$lines += ""
$lines += "| Config | Core | Max | Queue | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | CPU % | Heap MB | WorkingSet MB | Threads |"
$lines += "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|"
foreach ($r in $results) {
    $lines += "| $($r.Config) | $($r.CorePoolSize) | $($r.MaxPoolSize) | $($r.QueueCapacity) | $($r.Requests) | $($r.ErrorCount) | $($r.OverallAvgMs) | $($r.OverallP50Ms) | $($r.OverallP95Ms) | $($r.CpuUtilizationPercent) | $($r.HeapUsedMb) | $($r.WorkingSetMb) | $($r.ThreadCount) |"
}
$lines += ""
$lines += "## Selected Optimal Configuration"
$lines += "- Config: $($best.Config)"
$lines += "- Core/Max/Queue: $($best.CorePoolSize)/$($best.MaxPoolSize)/$($best.QueueCapacity)"
$lines += "- Selection rule: lowest error count -> lowest p95 -> lowest avg."
$lines | Set-Content -Path $reportMd

Write-Host "Epic 3 tuning complete."
Write-Host "Summary: $summaryJson"
Write-Host "Report:  $reportMd"

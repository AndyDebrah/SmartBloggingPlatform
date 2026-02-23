param(
    [Parameter(Mandatory=$true)][string]$LogFile
)

# Two modes:
# 1) Parse [BENCHMARK] lines like: [BENCHMARK] com.example.Service.method took 12.345 ms
# 2) Parse tabular performance report with header "PERFORMANCE BENCHMARK REPORT" and columns

$benchRegex = '\[BENCHMARK\]\s+(.+?)\s+took\s+([0-9.]+)\s+ms'

$content = Get-Content -Path $LogFile -ErrorAction Stop

# Try mode 1 first
$groups = @{}

foreach ($line in $content) {
    $m = [regex]::Match($line, $benchRegex)
    if ($m.Success) {
        $name = $m.Groups[1].Value.Trim()
        $val = [double]$m.Groups[2].Value
        if (-not $groups.ContainsKey($name)) { $groups[$name] = New-Object System.Collections.Generic.List[double] }
        $groups[$name].Add($val)
    }
}

function Short-Name($full) {
    if ($full -match '\.') { return $full.Split('.')[-1] }
    return $full
}

if ($groups.Count -gt 0) {
    $ordered = $groups.GetEnumerator() | Sort-Object @{Expression={$_.Value.Count};Descending=$true}, Name
    foreach ($entry in $ordered) {
        $full = $entry.Key
        $vals = $entry.Value
        if ($vals.Count -eq 0) { continue }
        $cold = $vals[0]
        if ($vals.Count -gt 1) {
            $warm = ($vals | Select-Object -Skip 1 | Measure-Object -Average).Average
        } else {
            $warm = $cold
        }
        if ($cold -gt 0) { $improvement = (($cold - $warm) / $cold) * 100.0 } else { $improvement = 0 }

        $sname = Short-Name $full
        if ($cold -ge 1) { $cstr = [math]::Round($cold) + " ms" } else { $cstr = '{0:N1} ms' -f $cold }
        if ($warm -ge 1) { $wstr = '{0:N1} ms' -f $warm } else { $wstr = '{0:N1} ms' -f $warm }
        $impstr = '{0:N1}%' -f $improvement

        Write-Output "COLD $sname took: $cstr"
        Write-Output "WARM $sname took: $wstr"
        Write-Output "Cache improvement: $impstr"`n
    }
    exit 0
}

# Mode 2: try to parse a table like in the provided performance_report file
# Find header line that contains 'Test' and 'Cold (ms)'
$headerIndex = $null
for ($i = 0; $i -lt $content.Count; $i++) {
    if ($content[$i] -match 'Test\s+\s*Cold \(ms\)') { $headerIndex = $i; break }
}

if ($null -eq $headerIndex) {
    Write-Output "No [BENCHMARK] entries or recognized table found in $LogFile"
    exit 0
}
# Parse subsequent lines; rows may be wrapped across multiple physical lines.
$buffer = ""
$rowRegex = '^(.*?)\s{2,}([0-9.\-]+)\s+([0-9.\-]+)\s+([0-9.\-]+)\s+([0-9.]+)%$'

for ($j = $headerIndex + 1; $j -lt $content.Count; $j++) {
    $ln = $content[$j].Trim()
    if ([string]::IsNullOrWhiteSpace($ln)) { continue }
    if ($buffer -eq "") { $buffer = $ln } else { $buffer = $buffer + " " + $ln }

    # assume rows end with a percent ("%") marker; when present, try to parse the accumulated row
    if ($buffer -notmatch '%') { continue }

    $m = [regex]::Match($buffer, $rowRegex)
    if (-not $m.Success) {
        # couldn't parse the accumulated row; drop buffer to avoid cascading errors
        $buffer = ""
        continue
    }

    $testName = $m.Groups[1].Value.Trim()
    $cold = 0.0
    $warm = 0.0
    $pct = 0.0
    [double]::TryParse($m.Groups[2].Value, [ref]$cold) | Out-Null
    [double]::TryParse($m.Groups[3].Value, [ref]$warm) | Out-Null
    [double]::TryParse($m.Groups[5].Value, [ref]$pct) | Out-Null

    # normalize test name
    $clean = ($testName -replace '\(.*?\)','') -replace '[^a-zA-Z0-9 ]',''
    $words = $clean -split '\s+' | Where-Object { $_ -ne '' }
    if ($words.Count -eq 0) { $sname = $clean } else {
        $first = $words[0].ToLower()
        $rest = $words | Select-Object -Skip 1 | ForEach-Object { $_.Substring(0,1).ToUpper() + $_.Substring(1).ToLower() }
        $sname = $first + ($rest -join '')
    }

    if ($cold -ge 1) { $cstr = [math]::Round($cold) + " ms" } else { $cstr = '{0:N1} ms' -f $cold }
    if ($warm -ge 1) { $wstr = '{0:N1} ms' -f $warm } else { $wstr = '{0:N1} ms' -f $warm }
    $impstr = '{0:N1}%' -f $pct

    Write-Output "COLD $sname took: $cstr"
    Write-Output "WARM $sname took: $wstr"
    Write-Output "Cache improvement: $impstr"`n

    $buffer = ""
}

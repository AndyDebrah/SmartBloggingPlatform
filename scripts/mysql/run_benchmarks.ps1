param(
    [Parameter(Mandatory=$true)] [string] $User,
    [Parameter(Mandatory=$true)] [string] $Database,
    [Parameter(Mandatory=$true)] [string] $Query,
    [int] $Runs = 5
)

Write-Host "Running benchmark against database '$Database' as user '$User' ($Runs runs)"

$results = @()
for($i=1; $i -le $Runs; $i++){
    $start = Get-Date
    mysql -u $User -p -D $Database -e "$Query" > $null
    $elapsed = (Get-Date) - $start
    $ms = [math]::Round($elapsed.TotalMilliseconds,2)
    Write-Host "Run $i: $ms ms"
    $results += $ms
}

$avg = [math]::Round(($results | Measure-Object -Average).Average,2)
$median = ([array]::CreateInstance([double], $results.Count))
$sorted = $results | Sort-Object
if($sorted.Count % 2 -eq 1){ $median = $sorted[([int]($sorted.Count/2))] } else { $median = ([double]($sorted[($sorted.Count/2)-1] + $sorted[($sorted.Count/2)])/2) }

Write-Host "Average: $avg ms"
Write-Host "Median: $median ms"

Write-Host "Tip: For cold-cache, restart the MySQL server between runs or use a fresh instance. For EXPLAIN ANALYZE, run the EXPLAIN ANALYZE statements in `scripts/mysql/explain_queries.sql` and save outputs to JSON files."

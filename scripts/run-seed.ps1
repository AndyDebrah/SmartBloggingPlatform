<#
PowerShell helper to backup the DB, import the seed script, and call the seeder.
Usage: Run from project root in PowerShell:
  .\scripts\run-seed.ps1

The script tries to auto-detect MySQL/MariaDB binaries under common Program Files
locations. If not found you will be prompted to provide the path to the MySQL bin
folder (the folder containing mysql.exe and mysqldump.exe).
#>

Set-StrictMode -Version Latest
Write-Host "run-seed.ps1 — Backup DB, import seed script, and run CALL seed_demo_posts(N)"

function Find-MySqlBin {
    $candidates = @(
        'C:\Program Files\MySQL\MySQL Server 8.0\bin',
        'C:\Program Files\MySQL\MySQL Workbench 8.0',
        'C:\Program Files\MariaDB 10.4\bin',
        "${env:ProgramFiles}\MySQL\MySQL Server 8.0\bin"
    )
    foreach ($p in $candidates) {
        if (Test-Path (Join-Path $p 'mysql.exe')) { return $p }
    }
    return $null
}

$bin = Find-MySqlBin
if (-not $bin) {
    $bin = Read-Host "Could not auto-find MySQL binaries. Enter MySQL bin folder path (or press Enter to abort)"
    if (-not $bin) { Write-Error "No mysql path provided — aborting."; exit 1 }
}

$mysql = Join-Path $bin 'mysql.exe'
$mysqldump = Join-Path $bin 'mysqldump.exe'

if (-not (Test-Path $mysql)) { Write-Error "mysql.exe not found in $bin"; exit 1 }
if (-not (Test-Path $mysqldump)) { Write-Warning "mysqldump.exe not found in $bin — backup will be skipped" }

$db = Read-Host "Database name (default: smart_blog_db)"
if ([string]::IsNullOrWhiteSpace($db)) { $db = 'smart_blog_db' }
$user = Read-Host "DB user (default: root)"
if ([string]::IsNullOrWhiteSpace($user)) { $user = 'root' }
$pw = Read-Host -AsSecureString "DB password (input hidden)"
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($pw)
$plain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)

$projectRoot = Resolve-Path ".." -Relative
if (-not $projectRoot) { $projectRoot = Get-Location }

if (Test-Path $mysqldump) {
    Write-Host "Creating backup 'backup.sql'..."
    & "$mysqldump" -u $user -p$plain $db > backup.sql
    if ($LASTEXITCODE -ne 0) { Write-Warning "mysqldump returned non-zero code" }
    else { Write-Host "Backup written to backup.sql" }
} else {
    Write-Warning "Skipping backup — mysqldump not available";
}

$seedPath = Join-Path (Get-Location) 'scripts\seed_demo_posts.sql'
if (-not (Test-Path $seedPath)) { Write-Error "Seed script not found at $seedPath"; exit 1 }

Write-Host "Importing seed script into database $db..."
Get-Content $seedPath -Raw | & "$mysql" -u $user -p$plain $db
if ($LASTEXITCODE -ne 0) { Write-Warning "Import returned non-zero exit code" }
else { Write-Host "Seed script loaded." }

$count = Read-Host "How many demo posts to insert? (default: 50)"
if (-not [int]::TryParse($count, [ref]$null)) { $count = 50 }

Write-Host "Calling stored-procedure seed_demo_posts($count)"
& "$mysql" -u $user -p$plain -D $db -e "CALL seed_demo_posts($count);"

Write-Host "Done. Check your database for inserted posts and comments."

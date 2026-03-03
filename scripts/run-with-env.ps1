<#
Loads environment variables from a .env file at the repository root into the current process
and runs the Spring Boot application using the `prod` profile.

Usage (PowerShell):
  .\scripts\run-with-env.ps1

Note: Create a `.env` file from `.env.example` and do NOT commit it.
#>

$envPath = Join-Path $PSScriptRoot '..\.env'
if (-not (Test-Path $envPath)) {
    Write-Error ".env not found at $envPath. Copy .env.example to .env and fill values."
    exit 1
}

Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $parts = $line -split '=', 2
        if ($parts.Length -eq 2) {
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}

Write-Host "Loaded environment variables from $envPath"

# If script arguments are provided, treat them as Maven goals/arguments and run the Maven wrapper with them.
$mvnWrapper = Join-Path $PSScriptRoot '..\\mvnw.cmd'
if ($args.Count -gt 0) {
    Write-Host "Running Maven with arguments: $args"
    # If the JDBC URL contains ampersands, run the full command under cmd.exe to avoid PowerShell/Windows parsing issues
    # Join args but escape ampersands so cmd.exe won't split the command
    $safeArgs = $args | ForEach-Object { $_ -replace '&', '^&' }
    $mvnArgs = $safeArgs -join ' '
    if ($env:DB_URL -and $env:DB_URL.Contains('&')) {
        $cmd = "$mvnWrapper $mvnArgs"
        Write-Host "Detected '&' in DB_URL; escaping ampersands and executing via cmd.exe"
        Start-Process cmd.exe -ArgumentList '/c', $cmd -NoNewWindow -Wait
    } else {
        & $mvnWrapper @args
    }
} else {
    # Default: run the Spring Boot app with prod profile
    # Use cmd.exe to avoid PowerShell parsing of -D properties
    $cmd = "$mvnWrapper -Dspring-boot.run.profiles=prod spring-boot:run"
    Write-Host "Executing: $cmd"
    Start-Process cmd.exe -ArgumentList '/c', $cmd -NoNewWindow -Wait
}

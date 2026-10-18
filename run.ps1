$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Load-UserEnv([string]$Name) {
    if (-not (Test-Path "Env:$Name")) {
        $value = [Environment]::GetEnvironmentVariable($Name, 'User')
        if ($null -ne $value) { Set-Item -Path "Env:$Name" -Value $value }
    }
}

foreach ($name in @(
    'DB_HOST','DB_PORT','DB_NAME','DB_USER','DB_PASSWORD','DB_URL',
    'DATA_GOV_API_KEY','GEMINI_API_KEY','GEMINI_MODEL','AGRITECH_SETUP_DONE'
)) { Load-UserEnv $name }

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "          AGRITECH INTELLIGENCE - MYSQL/JDBC START" -ForegroundColor Green
Write-Host "==============================================================" -ForegroundColor Green

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java not found. JDK 17+ install karke PowerShell reopen kijiye."
}

if (-not $env:AGRITECH_SETUP_DONE -or -not $env:DB_USER) {
    Write-Host "First-run MySQL/API setup..." -ForegroundColor Cyan
    & "$PSScriptRoot\setup.ps1"
    foreach ($name in @(
        'DB_HOST','DB_PORT','DB_NAME','DB_USER','DB_PASSWORD','DB_URL',
        'DATA_GOV_API_KEY','GEMINI_API_KEY','GEMINI_MODEL','AGRITECH_SETUP_DONE'
    )) {
        $value = [Environment]::GetEnvironmentVariable($name, 'User')
        if ($null -ne $value) { Set-Item -Path "Env:$name" -Value $value }
    }
}

if (Test-Path '.\mvnw.cmd') {
    $maven = '.\mvnw.cmd'
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    $maven = 'mvn'
} else {
    throw "Maven wrapper/system Maven not found. Project ke .mvn folder aur mvnw.cmd ko delete mat kijiye."
}

Write-Host ""
Write-Host "Database target: $($env:DB_HOST):$($env:DB_PORT)/$($env:DB_NAME)" -ForegroundColor Cyan
Write-Host "Compiling project..." -ForegroundColor Cyan

& $maven '-q' '-DskipTests' 'package'
if ($LASTEXITCODE -ne 0) {
    throw "Application build failed. Upar ka FIRST compilation error dekhiye."
}

Write-Host ""
Write-Host "BUILD SUCCESS - starting terminal application..." -ForegroundColor Green
Write-Host ""

& $maven '-q' '-DskipTests' 'exec:java'
if ($LASTEXITCODE -ne 0) {
    throw "AgriTech stopped with an error. Upar ka first meaningful error dekhiye."
}

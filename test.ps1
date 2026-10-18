$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (Test-Path '.\mvnw.cmd') { $maven = '.\mvnw.cmd' }
elseif (Get-Command mvn -ErrorAction SilentlyContinue) { $maven = 'mvn' }
else { throw 'Maven wrapper/system Maven not found.' }

Write-Host "Running AgriTech unit tests..." -ForegroundColor Cyan
& $maven 'clean' 'test'
if ($LASTEXITCODE -ne 0) { throw 'Tests failed. Read the first test error above.' }
Write-Host 'ALL TESTS PASSED.' -ForegroundColor Green

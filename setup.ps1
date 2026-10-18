$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Read-SecretText([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    if ($secure.Length -eq 0) { return '' }
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

function UserValue([string]$Name) {
    return [Environment]::GetEnvironmentVariable($Name, 'User')
}

function Set-UserValue([string]$Name, [string]$Value) {
    if ($null -ne $Value) {
        [Environment]::SetEnvironmentVariable($Name, $Value, 'User')
        Set-Item -Path "Env:$Name" -Value $Value
    }
}

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "      AGRITECH INTELLIGENCE - JDBC / API SETUP" -ForegroundColor Green
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "Farmer + equipment data MySQL me store hoga through JDBC." -ForegroundColor Cyan
Write-Host "Koi CSV/TXT runtime database use nahi hota." -ForegroundColor Cyan
Write-Host "Secrets source code ya project file me save nahi honge." -ForegroundColor DarkGray
Write-Host ""

$oldHost = UserValue 'DB_HOST'; if (-not $oldHost) { $oldHost = 'localhost' }
$oldPort = UserValue 'DB_PORT'; if (-not $oldPort) { $oldPort = '3306' }
$oldName = UserValue 'DB_NAME'; if (-not $oldName) { $oldName = 'agritech_intelligence' }
$oldUser = UserValue 'DB_USER'; if (-not $oldUser) { $oldUser = 'root' }
$oldPassword = UserValue 'DB_PASSWORD'

$hostInput = Read-Host "MySQL host [$oldHost]"; if (-not $hostInput) { $hostInput = $oldHost }
$portInput = Read-Host "MySQL port [$oldPort]"; if (-not $portInput) { $portInput = $oldPort }
$nameInput = Read-Host "Database name [$oldName]"; if (-not $nameInput) { $nameInput = $oldName }
$userInput = Read-Host "MySQL username [$oldUser]"; if (-not $userInput) { $userInput = $oldUser }

if ($oldPassword) {
    $newDbPassword = Read-SecretText "MySQL password (Enter = keep existing)"
    if (-not $newDbPassword) { $newDbPassword = $oldPassword }
} else {
    $newDbPassword = Read-SecretText "MySQL password (Enter = blank password)"
}

Set-UserValue 'DB_HOST' $hostInput
Set-UserValue 'DB_PORT' $portInput
Set-UserValue 'DB_NAME' $nameInput
Set-UserValue 'DB_USER' $userInput
Set-UserValue 'DB_PASSWORD' $newDbPassword

Write-Host ""
Write-Host "Open-Meteo Weather + Geocoding: no API key required." -ForegroundColor Cyan
Write-Host "Mandi: AGMARKNET/CEDA no-key fallbacks + optional data.gov.in key." -ForegroundColor Cyan
Write-Host "Gemini: optional; local Hinglish fallback always available." -ForegroundColor Cyan

$oldDataKey = UserValue 'DATA_GOV_API_KEY'
if ($oldDataKey) {
    $newDataKey = Read-SecretText "DATA_GOV_API_KEY (Enter = keep existing)"
    if (-not $newDataKey) { $newDataKey = $oldDataKey }
} else {
    $newDataKey = Read-SecretText "DATA_GOV_API_KEY (optional; Enter = skip)"
}
if ($newDataKey) { Set-UserValue 'DATA_GOV_API_KEY' $newDataKey }

$oldGemini = UserValue 'GEMINI_API_KEY'
if ($oldGemini) {
    $newGemini = Read-SecretText "GEMINI_API_KEY (Enter = keep existing)"
    if (-not $newGemini) { $newGemini = $oldGemini }
} else {
    $newGemini = Read-SecretText "GEMINI_API_KEY (optional; Enter = local narrator)"
}
if ($newGemini) { Set-UserValue 'GEMINI_API_KEY' $newGemini }

$oldModel = UserValue 'GEMINI_MODEL'; if (-not $oldModel) { $oldModel = 'gemini-3.5-flash-lite' }
$modelInput = Read-Host "Gemini model [$oldModel]"; if (-not $modelInput) { $modelInput = $oldModel }
Set-UserValue 'GEMINI_MODEL' $modelInput
Set-UserValue 'AGRITECH_SETUP_DONE' '1'

Write-Host ""
Write-Host "SETUP SAVED TO WINDOWS USER ENVIRONMENT VARIABLES." -ForegroundColor Green
Write-Host "No API key / MySQL password was written into the project folder." -ForegroundColor Green
Write-Host ""
Write-Host "IMPORTANT NEXT STEP:" -ForegroundColor Yellow
Write-Host "MySQL Workbench me sql\01_agritech_schema.sql ek baar execute kijiye." -ForegroundColor Yellow
Write-Host "Uske baad .\run.ps1 run kijiye." -ForegroundColor Cyan

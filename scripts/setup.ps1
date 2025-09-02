# PowerShell 스크립트 (더 강력한 Windows 지원)
# Compass Backend 개발 환경 설정

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Compass Backend Development Setup" -ForegroundColor Cyan
Write-Host " PowerShell Version" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. .env 파일 생성
Write-Host "Checking .env file..." -ForegroundColor Yellow
if (-not (Test-Path .env)) {
    if (Test-Path .env.example) {
        Copy-Item .env.example .env
        Write-Host "[Created] .env file from .env.example" -ForegroundColor Green
        Write-Host ""
        Write-Host "Please add GitHub Secrets:" -ForegroundColor Yellow
        Write-Host "1. Open: https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE/settings/secrets/actions"
        Write-Host "2. Copy GOOGLE_CREDENTIALS_BASE64"
        Write-Host "3. Add to .env file"
        Write-Host ""
        Read-Host "Press Enter after adding the secrets"
    } else {
        Write-Host "[ERROR] .env.example not found!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[OK] .env file exists" -ForegroundColor Green
}

# 2. .env 파일 로드
Write-Host "Loading environment variables..." -ForegroundColor Yellow
$envContent = Get-Content .env
foreach ($line in $envContent) {
    if ($line -match '^([^#][^=]+)=(.+)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        [Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}
Write-Host "[OK] Environment variables loaded" -ForegroundColor Green

# 3. Google Cloud 인증 파일 생성
$base64Key = [Environment]::GetEnvironmentVariable("GOOGLE_CREDENTIALS_BASE64", "Process")
if ($base64Key -and $base64Key -ne "your-base64-encoded-key-here") {
    Write-Host "Creating Google Cloud credentials..." -ForegroundColor Yellow
    try {
        $bytes = [System.Convert]::FromBase64String($base64Key)
        [System.IO.File]::WriteAllBytes("$PWD\gcp-key.json", $bytes)
        [Environment]::SetEnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", "$PWD\gcp-key.json", "Process")
        Write-Host "[OK] Created gcp-key.json" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Failed to decode Base64" -ForegroundColor Red
        Write-Host $_.Exception.Message
    }
} else {
    Write-Host "[WARNING] GOOGLE_CREDENTIALS_BASE64 not set" -ForegroundColor Yellow
}

# 4. Java 확인
Write-Host "Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    if ($javaVersion -match "17") {
        Write-Host "[OK] Java 17 detected" -ForegroundColor Green
    } else {
        Write-Host "[WARNING] Java 17 required" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[ERROR] Java not installed" -ForegroundColor Red
}

# 5. Docker 확인 및 시작
Write-Host "Checking Docker..." -ForegroundColor Yellow
try {
    docker --version | Out-Null
    Write-Host "[OK] Docker installed" -ForegroundColor Green
    
    Write-Host "Starting database services..." -ForegroundColor Yellow
    docker-compose up -d postgres redis
    Write-Host "[OK] Services started" -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Docker not available" -ForegroundColor Yellow
    Write-Host "Please install Docker Desktop for Windows"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "To run the application:" -ForegroundColor Yellow
Write-Host "  .\run.ps1" -ForegroundColor White
Write-Host "Or:" -ForegroundColor Yellow
Write-Host "  .\gradlew.bat bootRun" -ForegroundColor White
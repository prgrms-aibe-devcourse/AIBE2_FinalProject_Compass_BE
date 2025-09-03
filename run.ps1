# PowerShell 실행 스크립트
# Compass Backend Application Runner

Write-Host "Starting Compass Backend..." -ForegroundColor Cyan

# .env 파일 로드
if (Test-Path .env) {
    $envContent = Get-Content .env
    foreach ($line in $envContent) {
        if ($line -match '^([^#][^=]+)=(.+)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

# Google Cloud 인증 설정
if (Test-Path gcp-key.json) {
    [Environment]::SetEnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", "$PWD\gcp-key.json", "Process")
    Write-Host "[OK] Google Cloud credentials set" -ForegroundColor Green
}

# Docker 서비스 시작
Write-Host "Starting database services..." -ForegroundColor Yellow
docker-compose up -d postgres redis

# Spring Boot 실행
Write-Host "Starting Spring Boot application..." -ForegroundColor Yellow
.\gradlew.bat bootRun
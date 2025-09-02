@echo off
REM Compass Backend Windows 개발 환경 설정 스크립트

echo ========================================
echo  Compass Backend Development Setup
echo  Windows Environment
echo ========================================
echo.

REM 1. .env 파일 확인 및 생성
if not exist .env (
    if exist .env.example (
        echo Creating .env file from .env.example...
        copy .env.example .env
        echo.
        echo [!] .env file created!
        echo [!] Please add GitHub Secrets values:
        echo     1. Open: https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE/settings/secrets/actions
        echo     2. Copy GOOGLE_CREDENTIALS_BASE64 value
        echo     3. Paste into .env file
        echo.
        pause
    ) else (
        echo [ERROR] .env.example not found!
        exit /b 1
    )
) else (
    echo [OK] .env file already exists
)

REM 2. Java 버전 확인
echo Checking Java version...
java -version 2>&1 | findstr "17" >nul
if %errorlevel% neq 0 (
    echo [WARNING] Java 17 is required. Current version:
    java -version
    echo.
) else (
    echo [OK] Java 17 detected
)

REM 3. Docker 상태 확인
echo Checking Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Docker is not installed or not running
    echo Please install Docker Desktop for Windows
    echo.
) else (
    echo [OK] Docker is installed
    echo Starting PostgreSQL and Redis...
    docker-compose up -d postgres redis
    echo [OK] Database services started
)

echo.
echo ========================================
echo  Setup Complete!
echo ========================================
echo.
echo Next steps:
echo 1. Make sure you've added GitHub Secrets to .env file
echo 2. Run: setup-credentials.bat
echo 3. Run: run.bat
echo.
pause
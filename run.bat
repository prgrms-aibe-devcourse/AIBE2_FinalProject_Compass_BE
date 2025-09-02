@echo off
REM Compass Backend 실행 스크립트 (Windows)

echo ========================================
echo  Starting Compass Backend
echo ========================================
echo.

REM 1. Google Cloud 인증 파일 확인
if exist gcp-key.json (
    echo [OK] Google Cloud credentials found
    set GOOGLE_APPLICATION_CREDENTIALS=%CD%\gcp-key.json
) else (
    echo [WARNING] gcp-key.json not found
    echo Run setup-credentials.bat first
    echo.
)

REM 2. Docker 서비스 시작
echo Starting database services...
docker-compose up -d postgres redis
if %errorlevel% neq 0 (
    echo [ERROR] Failed to start Docker services
    echo Make sure Docker Desktop is running
    pause
    exit /b 1
)

echo [OK] Database services started
echo.

REM 3. Spring Boot 애플리케이션 실행
echo Starting Spring Boot application...
echo.

REM .env 파일의 환경 변수들을 수동으로 설정
REM (Windows batch는 .env 파일을 자동으로 로드할 수 없음)
for /f "tokens=1,2 delims==" %%a in (.env) do (
    if not "%%a"=="" if not "%%b"=="" (
        set "%%a=%%b"
    )
)

REM Gradle 실행
call gradlew.bat bootRun

pause
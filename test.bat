@echo off
REM Compass Backend 테스트 스크립트 (Windows)

echo ========================================
echo  Running Tests
echo ========================================
echo.

REM Google Cloud 인증 파일 설정
if exist gcp-key.json (
    set GOOGLE_APPLICATION_CREDENTIALS=%CD%\gcp-key.json
)

REM .env 파일의 환경 변수들을 설정
for /f "tokens=1,2 delims==" %%a in (.env) do (
    if not "%%a"=="" if not "%%b"=="" (
        set "%%a=%%b"
    )
)

REM 테스트 실행
call gradlew.bat test

echo.
echo Test completed!
pause
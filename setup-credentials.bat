@echo off
REM Google Cloud 인증 파일 생성 스크립트 (Windows)

echo ========================================
echo  Google Cloud Credentials Setup
echo ========================================
echo.

REM .env 파일 로드 (Windows에서는 복잡하므로 PowerShell 사용)
echo Loading environment variables...

REM PowerShell을 사용하여 Base64 디코딩 및 파일 생성
powershell -Command "& {
    # .env 파일 읽기
    $envContent = Get-Content .env
    $base64Line = $envContent | Where-Object { $_ -match '^GOOGLE_CREDENTIALS_BASE64=' }
    
    if ($base64Line) {
        $base64Value = $base64Line -replace 'GOOGLE_CREDENTIALS_BASE64=', ''
        $base64Value = $base64Value.Trim()
        
        if ($base64Value -and $base64Value -ne 'your-base64-encoded-key-here') {
            Write-Host '[OK] Found GOOGLE_CREDENTIALS_BASE64'
            
            # Base64 디코딩
            try {
                $bytes = [System.Convert]::FromBase64String($base64Value)
                [System.IO.File]::WriteAllBytes('gcp-key.json', $bytes)
                Write-Host '[OK] Created gcp-key.json file'
                
                # 환경 변수 설정 안내
                Write-Host ''
                Write-Host 'To set environment variable, run:'
                Write-Host '  set GOOGLE_APPLICATION_CREDENTIALS=%CD%\gcp-key.json'
            } catch {
                Write-Host '[ERROR] Failed to decode Base64. Please check the value.'
                exit 1
            }
        } else {
            Write-Host '[ERROR] GOOGLE_CREDENTIALS_BASE64 not set in .env file'
            Write-Host 'Please add the value from GitHub Secrets'
            exit 1
        }
    } else {
        Write-Host '[ERROR] GOOGLE_CREDENTIALS_BASE64 not found in .env file'
        exit 1
    }
}"

echo.
echo Setup complete!
echo.
pause
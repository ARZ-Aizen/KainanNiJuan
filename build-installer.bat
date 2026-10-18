@echo off
echo ========================================================
echo   Creating Single-File Installer (KainanNiJuan-Setup.exe)
echo ========================================================

set "ISCC="
if exist "%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe" (
    set "ISCC=%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe"
) else if exist "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" (
    set "ISCC=C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
) else if exist "C:\Program Files\Inno Setup 6\ISCC.exe" (
    set "ISCC=C:\Program Files\Inno Setup 6\ISCC.exe"
)

if "%ISCC%"=="" (
    echo [ERROR] Inno Setup compiler ISCC.exe not found!
    pause
    exit /b 1
)

echo.
echo Compiling installer with Inno Setup...
"%ISCC%" installer.iss

if %errorlevel% neq 0 (
    echo [ERROR] Installer build failed!
    pause
    exit /b %errorlevel%
)

echo.
echo ========================================================
echo   SUCCESS! Single-File Installer Ready:
echo   File: dist-installer\KainanNiJuan-Setup.exe
echo ========================================================
echo.

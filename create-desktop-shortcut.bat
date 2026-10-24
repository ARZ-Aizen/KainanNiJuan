@echo off
echo ========================================================
echo   Creating Desktop Shortcut for Kainan Ni Juan
echo ========================================================

set "TARGET_EXE=%~dp0dist\KainanNiJuan\KainanNiJuan.exe"
set "ICON_FILE=%~dp0dist\KainanNiJuan\app-icon.ico"

if not exist "%TARGET_EXE%" (
    echo [ERROR] %TARGET_EXE% not found!
    echo Please run build-exe.bat first.
    pause
    exit /b 1
)

if not exist "%ICON_FILE%" (
    if exist "%~dp0app-icon.ico" (
        set "ICON_FILE=%~dp0app-icon.ico"
    ) else (
        set "ICON_FILE=%TARGET_EXE%"
    )
)

powershell -NoProfile -Command ^
    "$ws = New-Object -ComObject WScript.Shell; " ^
    "$desktop = [Environment]::GetFolderPath('Desktop'); " ^
    "$shortcut = $ws.CreateShortcut(\"$desktop\Kainan Ni Juan.lnk\"); " ^
    "$shortcut.TargetPath = '%TARGET_EXE%'; " ^
    "$shortcut.WorkingDirectory = '%~dp0dist\KainanNiJuan'; " ^
    "$shortcut.IconLocation = '%ICON_FILE%,0'; " ^
    "$shortcut.Description = 'Kainan Ni Juan Restaurant POS'; " ^
    "$shortcut.Save(); " ^
    "Write-Output '[SUCCESS] Desktop shortcut created at:' $shortcut.FullName"

echo.
echo ========================================================
echo   Done! Check your desktop for the Kainan Ni Juan icon.
echo ========================================================
echo.

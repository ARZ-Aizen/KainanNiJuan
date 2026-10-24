@echo off
echo ========================================================
echo   Building Kainan Ni Juan Standalone Windows EXE
echo ========================================================

:: Check if KainanNiJuan is currently running
tasklist /fi "imagename eq KainanNiJuan.exe" 2>NUL | find /i "KainanNiJuan.exe" >NUL
if "%errorlevel%"=="0" (
    echo [INFO] KainanNiJuan.exe is currently running. Closing it to update files...
    taskkill /f /im KainanNiJuan.exe >nul 2>&1
    timeout /t 2 /nobreak >nul
)

:: 1. Detect or set JAVA_HOME
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
)

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo [ERROR] jpackage.exe not found in %JAVA_HOME%\bin!
    echo Please make sure JDK 21+ is installed.
    pause
    exit /b 1
)

:: 2. Compile and package jar
echo.
echo [1/4] Compiling and packaging Maven JAR...
call .\mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed!
    pause
    exit /b %errorlevel%
)

:: 3. Copy dependencies
echo.
echo [2/4] Gathering dependencies...
call .\mvnw.cmd dependency:copy-dependencies -DoutputDirectory=target/libs -DincludeScope=runtime
if %errorlevel% neq 0 (
    echo [ERROR] Failed to gather dependencies!
    pause
    exit /b %errorlevel%
)

:: 4. Prepare packaging folder
echo.
echo [3/4] Preparing bundle folder...
if exist "target\package-input" rmdir /s /q "target\package-input"
mkdir "target\package-input"
copy "target\libs\*.jar" "target\package-input\" >nul
copy "target\KainanNiJuan-1.0-SNAPSHOT.jar" "target\package-input\" >nul

:: Ensure application icon exists
if not exist "app-icon.ico" (
    echo [INFO] Generating app-icon.ico from logo...
    powershell -ExecutionPolicy Bypass -File generate-icon.ps1
)

:: 5. Run jpackage
echo.
echo [4/4] Creating KainanNiJuan.exe using jpackage...
if exist "dist\KainanNiJuan" rmdir /s /q "dist\KainanNiJuan"

"%JAVA_HOME%\bin\jpackage.exe" ^
  --input "target\package-input" ^
  --main-jar "KainanNiJuan-1.0-SNAPSHOT.jar" ^
  --main-class "com.kainanresto.KainanNiJuan" ^
  --name "KainanNiJuan" ^
  --type app-image ^
  --icon "app-icon.ico" ^
  --dest "dist"

if %errorlevel% neq 0 (
    echo [ERROR] jpackage failed!
    pause
    exit /b %errorlevel%
)

:: Copy local configs and icon if present
if exist "app-icon.ico" copy "app-icon.ico" "dist\KainanNiJuan\" >nul
if exist "config.properties" copy "config.properties" "dist\KainanNiJuan\" >nul
if exist "db.properties" (
    copy "db.properties" "dist\KainanNiJuan\" >nul
) else if exist "src\main\resources\db.properties" (
    copy "src\main\resources\db.properties" "dist\KainanNiJuan\" >nul
)

echo.
echo ========================================================
echo   SUCCESS! Your standalone executable is ready:
echo   Folder: dist\KainanNiJuan
echo   Run:    dist\KainanNiJuan\KainanNiJuan.exe
echo ========================================================
echo.

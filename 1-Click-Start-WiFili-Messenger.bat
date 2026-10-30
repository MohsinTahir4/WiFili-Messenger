@echo off
setlocal
cd /d "%~dp0"
title WiFili Messenger - 1-Click Launcher
echo ====================================================
echo 📶 Starting WiFili Messenger...
echo ====================================================

:: Automatically configure Windows Defender Firewall rules in background
netsh advfirewall firewall show rule name="WiFili Messenger LAN" >nul 2>&1
if %errorlevel% neq 0 (
    echo Configuring Windows Defender Firewall...
    netsh advfirewall firewall add rule name="WiFili Messenger LAN" dir=in action=allow protocol=TCP localport=45455,8080,45457,45458 profile=any >nul 2>&1
    netsh advfirewall firewall add rule name="WiFili Messenger Discovery" dir=in action=allow protocol=UDP localport=45454,45457,45458 profile=any >nul 2>&1
)

:: Locate WiFili-Messenger-Desktop.jar
set "JAR_PATH="
if exist "%~dp0WiFili-Messenger-Desktop.jar" set "JAR_PATH=%~dp0WiFili-Messenger-Desktop.jar"
if not defined JAR_PATH if exist "%~dp0releases\WiFili-Messenger-Desktop.jar" set "JAR_PATH=%~dp0releases\WiFili-Messenger-Desktop.jar"
if not defined JAR_PATH if exist "%~dp0desktop\build\libs\desktop.jar" set "JAR_PATH=%~dp0desktop\build\libs\desktop.jar"

:: If JAR is missing, build it automatically via Gradle
if not defined JAR_PATH (
    if exist "%~dp0gradlew.bat" (
        echo WiFili-Messenger-Desktop.jar not found. Building app automatically via Gradle...
        call "%~dp0gradlew.bat" :desktop:jar
        if exist "%~dp0desktop\build\libs\desktop.jar" (
            set "JAR_PATH=%~dp0desktop\build\libs\desktop.jar"
        )
    )
)

if not defined JAR_PATH (
    echo.
    echo ⚠️ Error: WiFili-Messenger-Desktop.jar could not be located or built!
    echo Please download "WiFili-Messenger-Desktop-Complete.zip" instead.
    echo.
    pause
    exit /b
)

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Java runtime is required to run WiFili Messenger.
    echo Opening official Java download page...
    start https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b
)

start "" javaw -jar "%JAR_PATH%"
if %errorlevel% neq 0 (
    java -jar "%JAR_PATH%"
)

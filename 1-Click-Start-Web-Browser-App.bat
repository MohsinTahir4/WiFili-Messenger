@echo off
setlocal
cd /d "%~dp0"
title WiFili Messenger Web Edition
echo ====================================================
echo 📶 Starting WiFili Messenger Web Edition...
echo ====================================================

:: Automatically configure Windows Defender Firewall rules in background
netsh advfirewall firewall show rule name="WiFili Messenger LAN" >nul 2>&1
if %errorlevel% neq 0 (
    echo Configuring Windows Defender Firewall...
    netsh advfirewall firewall add rule name="WiFili Messenger LAN" dir=in action=allow protocol=TCP localport=45455,8080,45457,45458 profile=any >nul 2>&1
    netsh advfirewall firewall add rule name="WiFili Messenger Discovery" dir=in action=allow protocol=UDP localport=45454,45457,45458 profile=any >nul 2>&1
)

:: Locate WiFili-Messenger-Desktop.jar in current folder, releases/ folder, or build/ folder
set "JAR_PATH="
if exist "%~dp0WiFili-Messenger-Desktop.jar" set "JAR_PATH=%~dp0WiFili-Messenger-Desktop.jar"
if not defined JAR_PATH if exist "%~dp0releases\WiFili-Messenger-Desktop.jar" set "JAR_PATH=%~dp0releases\WiFili-Messenger-Desktop.jar"
if not defined JAR_PATH if exist "%~dp0desktop\build\libs\desktop.jar" set "JAR_PATH=%~dp0desktop\build\libs\desktop.jar"

if not defined JAR_PATH (
    echo.
    echo ⚠️ Error: WiFili-Messenger-Desktop.jar not found!
    echo Please ensure WiFili-Messenger-Desktop.jar is in this folder.
    echo.
    pause
    exit /b
)

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Java runtime is required to run WiFili Web Server.
    echo Opening official Java download page...
    start https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b
)

start "" javaw -jar "%JAR_PATH%" web
if %errorlevel% neq 0 (
    java -jar "%JAR_PATH%" web
)

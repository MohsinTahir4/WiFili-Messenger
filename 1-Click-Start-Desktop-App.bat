@echo off
title WiFili Messenger Desktop
echo ====================================================
echo 📶 Starting WiFili Messenger Desktop App...
echo ====================================================

:: Automatically configure Windows Defender Firewall rules in background
netsh advfirewall firewall show rule name="WiFili Messenger LAN" >nul 2>&1
if %errorlevel% neq 0 (
    echo Configuring Windows Defender Firewall...
    netsh advfirewall firewall add rule name="WiFili Messenger LAN" dir=in action=allow protocol=TCP localport=45455,8080,45457,45458 profile=any >nul 2>&1
    netsh advfirewall firewall add rule name="WiFili Messenger Discovery" dir=in action=allow protocol=UDP localport=45454,45457,45458 profile=any >nul 2>&1
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

start "" javaw -jar "%~dp0WiFili-Messenger-Desktop.jar"
if %errorlevel% neq 0 (
    java -jar "%~dp0WiFili-Messenger-Desktop.jar"
)

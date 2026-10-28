@echo off
title WiFili Messenger Web Edition
echo ====================================================
echo 📶 Launching WiFili Messenger Web Edition...
echo ====================================================

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Java runtime is required to run WiFili Web Server.
    echo Opening official Java download page...
    start https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b
)

start "" javaw -jar "%~dp0WiFili-Messenger-Desktop.jar" web
if %errorlevel% neq 0 (
    java -jar "%~dp0WiFili-Messenger-Desktop.jar" web
)

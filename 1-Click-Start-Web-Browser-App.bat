@echo off
setlocal
cd /d "%~dp0"
title WiFili Messenger - Web Edition

rem 1. Check for bundled portable JRE in jre\bin\javaw.exe
if exist "%~dp0jre\bin\javaw.exe" (
    start "" "%~dp0jre\bin\javaw.exe" -jar "%~dp0WiFili-Messenger-Desktop.jar" web
    exit /b
)

rem 2. Check for installed system Java
where java >nul 2>nul
if %errorlevel% equ 0 (
    start "" javaw -jar "%~dp0WiFili-Messenger-Desktop.jar" web
    if %errorlevel% neq 0 (
        java -jar "%~dp0WiFili-Messenger-Desktop.jar" web
    )
    exit /b
)

rem 3. If Java is missing on PC, prompt to install Java 17
echo ====================================================
echo 📶 WiFili Messenger - Java Runtime Required
echo ====================================================
echo.
echo Java 17 or higher is required to run WiFili Web Server on PC.
echo.
echo Opening official free Java 17 download page...
echo.
start https://adoptium.net/temurin/releases/?version=17
pause

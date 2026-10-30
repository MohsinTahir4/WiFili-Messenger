@echo off
setlocal
cd /d "%~dp0"
title WiFili Messenger - 1-Click Launcher
echo ====================================================
echo 📶 Starting WiFili Messenger...
echo ====================================================

:: 1. Locate JAR
set "JAR_PATH="
if exist "%~dp0WiFili-Messenger-Desktop.jar" set "JAR_PATH=%~dp0WiFili-Messenger-Desktop.jar"
if not defined JAR_PATH if exist "%~dp0releases\WiFili-Messenger-Desktop.jar" set "JAR_PATH=%~dp0releases\WiFili-Messenger-Desktop.jar"
if not defined JAR_PATH if exist "%~dp0desktop\build\libs\desktop.jar" set "JAR_PATH=%~dp0desktop\build\libs\desktop.jar"

:: 2. Auto-build via Gradle if missing
if not defined JAR_PATH (
    if exist "%~dp0gradlew.bat" (
        echo Building application automatically via Gradle...
        call "%~dp0gradlew.bat" :desktop:jar
        if exist "%~dp0desktop\build\libs\desktop.jar" (
            copy /y "%~dp0desktop\build\libs\desktop.jar" "%~dp0WiFili-Messenger-Desktop.jar" >nul
            set "JAR_PATH=%~dp0WiFili-Messenger-Desktop.jar"
        )
    )
)

if not defined JAR_PATH (
    echo.
    echo ❌ ERROR: WiFili-Messenger-Desktop.jar could not be located or built!
    echo.
    pause
    exit /b
)

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Java runtime not found. Opening Java download page...
    start https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b
)

start "" javaw -jar "%JAR_PATH%"
if %errorlevel% neq 0 (
    java -jar "%JAR_PATH%"
)

@echo off
echo Starting WiFili Web Server for Windows and Linux...
call "%~dp0gradlew.bat" :desktop:run --args="web"
if %errorlevel% neq 0 (
    echo.
    echo Press any key to exit...
    pause > nul
)

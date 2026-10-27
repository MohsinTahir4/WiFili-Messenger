@echo off
echo Starting LanChat Desktop...
call "%~dp0gradlew.bat" :desktop:run
if %errorlevel% neq 0 (
    echo.
    echo Press any key to exit...
    pause > nul
)

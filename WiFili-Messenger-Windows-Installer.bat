@echo off
title WiFili Messenger - Windows Installer & Firewall Setup
echo ====================================================
echo 📶 Installing WiFili Messenger & Configuring Firewall...
echo ====================================================

:: Check for Administrator privileges to configure Windows Firewall automatically
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Note: For automatic Windows Firewall configuration,
    echo please right-click this installer and select "Run as administrator".
    echo.
) else (
    echo [1/2] Automatically configuring Windows Defender Firewall rules...
    netsh advfirewall firewall delete rule name="WiFili Messenger LAN" >nul 2>&1
    netsh advfirewall firewall delete rule name="WiFili Messenger Discovery" >nul 2>&1

    netsh advfirewall firewall add rule name="WiFili Messenger LAN" dir=in action=allow protocol=TCP localport=45455,8080,45457,45458 profile=any >nul 2>&1
    netsh advfirewall firewall add rule name="WiFili Messenger Discovery" dir=in action=allow protocol=UDP localport=45454,45457,45458 profile=any >nul 2>&1
    echo ✅ Windows Firewall successfully configured to allow WiFili P2P connections!
)

set "INSTALL_DIR=%LOCALAPPDATA%\WiFiliMessenger"
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

if exist "%~dp0WiFili-Messenger-Desktop.jar" copy /y "%~dp0WiFili-Messenger-Desktop.jar" "%INSTALL_DIR%\" >nul
if exist "%~dp01-Click-Start-Desktop-App.bat" copy /y "%~dp01-Click-Start-Desktop-App.bat" "%INSTALL_DIR%\" >nul
if exist "%~dp0web" xcopy /s /y "%~dp0web" "%INSTALL_DIR%\web\" >nul

echo [2/2] Creating Desktop Shortcut...
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut.vbs"
echo sLinkFile = oWS.ExpandEnvironmentStrings("%USERPROFILE%\Desktop\WiFili Messenger.lnk") >> "%TEMP%\CreateShortcut.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\CreateShortcut.vbs"
echo oLink.TargetPath = "%INSTALL_DIR%\1-Click-Start-Desktop-App.bat" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.WorkingDirectory = "%INSTALL_DIR%" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Description = "WiFili Messenger 100% Offline P2P LAN Chat" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Save >> "%TEMP%\CreateShortcut.vbs"
cscript //nologo "%TEMP%\CreateShortcut.vbs"
del "%TEMP%\CreateShortcut.vbs"

echo.
echo ====================================================
echo ✅ Installation Complete!
echo A shortcut has been placed on your Windows Desktop.
echo ====================================================
echo.
pause

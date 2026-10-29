@echo off
title WiFili Messenger - Windows Installer
echo ====================================================
echo 📶 Installing WiFili Messenger on Windows...
echo ====================================================

set "INSTALL_DIR=%LOCALAPPDATA%\WiFiliMessenger"
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

if exist "%~dp0WiFili-Messenger-Desktop.jar" copy /y "%~dp0WiFili-Messenger-Desktop.jar" "%INSTALL_DIR%\" >nul
if exist "%~dp01-Click-Start-Desktop-App.bat" copy /y "%~dp01-Click-Start-Desktop-App.bat" "%INSTALL_DIR%\" >nul
if exist "%~dp0web" xcopy /s /y "%~dp0web" "%INSTALL_DIR%\web\" >nul

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
echo ✅ Installation Complete!
echo A shortcut has been placed on your Windows Desktop: "WiFili Messenger"
echo.
pause

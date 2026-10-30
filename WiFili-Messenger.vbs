Set WshShell = CreateObject("WScript.Shell")
WshShell.CurrentDirectory = WshShell.ExpandEnvironmentStrings("%~dp0")
WshShell.Run "cmd.exe /c if not exist ""WiFili-Messenger-Desktop.jar"" call gradlew.bat :desktop:jar & if exist ""desktop\build\libs\desktop.jar"" copy /y ""desktop\build\libs\desktop.jar"" ""WiFili-Messenger-Desktop.jar"" >nul & start """" javaw -jar ""WiFili-Messenger-Desktop.jar""", 0, False

@echo off
echo === Testing Recording Debugger ===
cd /d "C:\Users\ROMAIN\Documents\GitHub\MyJavaDebugger"

REM Compiler
echo Compiling...
call gradlew.bat build -q

REM Executer
echo.
echo Running RecordingDebugger...
echo.
java -cp "build\libs\untitled1-1.0-SNAPSHOT.jar;%JAVA_HOME%\lib\tools.jar" dbg.RecordingDebugger

pause

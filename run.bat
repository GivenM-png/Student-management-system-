@echo off
setlocal
cd /d "%~dp0"

if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 (
    echo.
    echo Build failed. Check that a JDK is installed and available on PATH.
    pause
    exit /b 1
)

java -cp out Main

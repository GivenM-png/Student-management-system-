@echo off
setlocal
cd /d "%~dp0"

if exist backend\.env (
    for /f "usebackq tokens=1,* delims==" %%A in ("backend\.env") do set "%%A=%%B"
)

if exist backend\target\studenthub-backend-1.0.0.jar (
    java -jar backend\target\studenthub-backend-1.0.0.jar
    exit /b %errorlevel%
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo Backend JAR is missing and Maven is not installed.
    echo Build backend\pom.xml with Maven, then run this file again.
    pause
    exit /b 1
)

call mvn -f backend\pom.xml spring-boot:run

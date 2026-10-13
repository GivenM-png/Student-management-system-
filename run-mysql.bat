@echo off
setlocal

set "MYSQL_HOME=C:\Program Files\MySQL\MySQL Server 8.4"
set "MYSQL_DATA=%LOCALAPPDATA%\StudentHub\MySQLData"

if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
    echo MySQL Server 8.4 is not installed at "%MYSQL_HOME%".
    exit /b 1
)

if not exist "%MYSQL_DATA%\auto.cnf" (
    echo The StudentHub MySQL data directory has not been initialized.
    echo Initialize the database once before starting the server.
    pause
    exit /b 1
)

"%MYSQL_HOME%\bin\mysqld.exe" --basedir="%MYSQL_HOME%" --datadir="%MYSQL_DATA%" --bind-address=127.0.0.1 --port=3306 --mysqlx=0 --console

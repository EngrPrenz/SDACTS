@echo off
echo ========================================
echo Java CRUD Management System
echo ========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java JDK 11 or higher
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

echo Java is installed. Checking Maven...
echo.

REM Check if Maven is installed
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Apache Maven
    echo Download from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo Maven is installed. Building project...
echo.

REM Build the project
call mvn clean install

if errorlevel 1 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo Build successful! Starting application...
echo.

REM Run the application
java -jar target\JavaCRUDSystem-1.0-SNAPSHOT.jar

if errorlevel 1 (
    echo.
    echo ERROR: Failed to start application!
    echo Please check database configuration in src/config/DatabaseConfig.java
    pause
    exit /b 1
)

pause
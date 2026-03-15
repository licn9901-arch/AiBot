@echo off
chcp 65001 >nul
REM DeskPet Image Build Script (Windows)
REM Usage: build.bat [TAG] [REGISTRY]
REM Example: build.bat v0.1.0 licn9901

setlocal enabledelayedexpansion

set TAG=%1
if "%TAG%"=="" set TAG=latest

set REGISTRY=%2
if "%REGISTRY%"=="" set REGISTRY=

set PROJECT_ROOT=%~dp0..

REM Maven path - modify this if mvn is not in PATH
REM If mvn is in PATH, just use "mvn"
REM Otherwise set the full path, e.g.:
REM set MVN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn"
set MVN_CMD=mvn
where mvn >nul 2>nul
if errorlevel 1 (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" (
        set MVN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd"
    ) else (
        echo [ERROR] mvn not found in PATH, please install Maven or set MVN_CMD in this script.
        exit /b 1
    )
)

echo ==========================================
echo  DeskPet Image Build
echo  TAG: %TAG%
echo  REGISTRY: %REGISTRY%
echo ==========================================

echo.
echo [1/4] Maven package...
cd /d "%PROJECT_ROOT%"
call %MVN_CMD% clean package -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Maven build failed!
    exit /b 1
)

echo [2/4] Building pet-core image...
docker build -t "%REGISTRY%/pet-core:%TAG%" "%PROJECT_ROOT%\pet-core"

REM echo [3/4] Building mqtt-gateway image...
REM docker build -t "%REGISTRY%/mqtt-gateway:%TAG%" "%PROJECT_ROOT%\mqtt-gateway"

REM echo [4/4] Building pet-ai image...
REM docker build -t "%REGISTRY%/pet-ai:%TAG%" "%PROJECT_ROOT%\pet-ai"

echo.
echo ==========================================
echo  Build complete!
echo ==========================================
docker images | findstr deskpet | findstr %TAG%

if not "%REGISTRY%"=="" (
    echo.
    set /p confirm="Push images to %REGISTRY%? (y/N) "
    if /i "!confirm!"=="y" (
        echo Pushing images...
        docker push "%REGISTRY%/pet-core:%TAG%"
        REM docker push "%REGISTRY%/mqtt-gateway:%TAG%"
        REM docker push "%REGISTRY%/pet-ai:%TAG%"
        echo Push complete!
    )
)

endlocal

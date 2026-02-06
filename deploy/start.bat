@echo off
setlocal
set SCRIPT_DIR=%~dp0
docker compose -f "%SCRIPT_DIR%docker-compose.yml" up -d
echo DB services started.
endlocal

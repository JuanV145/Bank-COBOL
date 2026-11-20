@echo off
REM ==================================================================
REM  run_frontend.bat : Arranca Angular (ng serve) en el puerto 4200
REM ==================================================================
setlocal
cd /d "%~dp0..\frontend"
npm start
endlocal

@echo off
REM ==================================================================
REM  run_backend.bat : Arranca Spring Boot en el puerto 8080
REM  - Apunta al pom del backend (-f backend\pom.xml)
REM  - Agrega el runtime COBOL (dll\) al PATH antes de iniciar la JVM
REM ==================================================================
setlocal
cd /d "%~dp0.."

REM Runtime de GnuCOBOL accesible para JNA
if exist "%CD%\dll\libcob-4.dll" set "PATH=%CD%\dll;%PATH%"
if exist "%CD%\dll\libcob.dll" set "PATH=%CD%\dll;%PATH%"
if exist "C:\msys64\ucrt64\bin" set "PATH=C:\msys64\ucrt64\bin;%PATH%"
if exist "C:\msys64\mingw64\bin" set "PATH=C:\msys64\mingw64\bin;%PATH%"

REM Usar Maven global si existe; si no, el incluido en tools\
set "MVN=mvn"
where mvn >nul 2>nul || set "MVN=%~dp0..\tools\apache-maven\bin\mvn.cmd"

echo Usando Maven: %MVN%
"%MVN%" -f "%CD%\backend\pom.xml" -Dspring-boot.run.workingDirectory="%CD%" spring-boot:run

endlocal

@echo off
REM ==================================================================
REM  build_dll.bat : Compila los 5 programas COBOL en un solo DLL
REM
REM  Proceso:
REM    1) cobc -m -c  -> compila cada .cbl a objeto (.o)
REM    2) gcc -shared -> enlaza los 5 objetos en COBOLBANK.dll
REM       (con --export-all-symbols para exportar los 5 entry points)
REM
REM  Requiere GnuCOBOL instalado con MSYS2:
REM    winget install MSYS2.MSYS2
REM    C:\msys64\usr\bin\bash -lc "pacman -S --noconfirm mingw-w64-ucrt-x86_64-gnucobol"
REM ==================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0.."

REM --- Localizar el entorno GnuCOBOL (ucrt64 o mingw64) ---
set "COBBIN="
if exist "C:\msys64\ucrt64\bin\cobc.exe" set "COBBIN=C:\msys64\ucrt64\bin"
if not defined COBBIN if exist "C:\msys64\mingw64\bin\cobc.exe" set "COBBIN=C:\msys64\mingw64\bin"
if not defined COBBIN (
    echo [ERROR] No se encontro cobc.exe ^(GnuCOBOL^).
    echo Instalacion sugerida:
    echo    winget install MSYS2.MSYS2
    echo    C:\msys64\usr\bin\bash -lc "pacman -S --noconfirm mingw-w64-ucrt-x86_64-gnucobol"
    exit /b 1
)

REM --- Variables de entorno que cobc/gcc necesitan fuera del shell MSYS2 ---
set "PATH=%COBBIN%;%PATH%"
if exist "C:\msys64\ucrt64\share\gnucobol\config" set "COB_CONFIG_DIR=C:\msys64\ucrt64\share\gnucobol\config"
if exist "C:\msys64\ucrt64\share\gnucobol\copy"   set "COB_COPY_DIR=C:\msys64\ucrt64\share\gnucobol\copy"

echo Compilador: %COBBIN%\cobc.exe
%COBBIN%\cobc.exe --version | findstr "cobc"

if not exist "obj" mkdir "obj"
if not exist "dll" mkdir "dll"

REM --- 1) Compilar cada programa COBOL a objeto ---
set "OBJETOS="
for %%P in (PGDEPOSIT PGRETIRO PGTRAN PGSALDO PGCIERRE) do (
    echo Compilando %%P.cbl ...
    %COBBIN%\cobc.exe -m -c -o obj\%%P.o cobol\%%P.cbl -I cobol\copy
    if errorlevel 1 (
        echo [ERROR] Fallo compilando %%P.cbl
        exit /b 1
    )
    set "OBJETOS=!OBJETOS! obj\%%P.o"
)

REM --- 2) Enlazar los 5 objetos en el DLL unico ---
echo Enlazando dll\COBOLBANK.dll ...
%COBBIN%\gcc.exe -shared -Wl,--export-all-symbols -o dll\COBOLBANK.dll %OBJETOS% ^
    -L"%COBBIN%\..\lib" -lcob
if errorlevel 1 (
    echo [ERROR] Fallo el enlazado del DLL
    exit /b 1
)

REM --- 3) Copiar el runtime de GnuCOBOL junto al DLL ---
for %%D in (libcob-4.dll libintl-8.dll libgmp-10.dll libdb-6.2.dll libjson-c-5.dll ^
            libncursesw6.dll libwinpthread-1.dll libxml2-16.dll libiconv-2.dll ^
            zlib1.dll liblzma-5.dll libzstd.dll libgcc_s_seh-1.dll) do (
    if exist "%COBBIN%\%%D" copy /Y "%COBBIN%\%%D" dll\ >nul
)

echo.
echo [OK] dll\COBOLBANK.dll generado con las 5 operaciones COBOL:
echo      PGDEPOSIT, PGRETIRO, PGTRAN, PGSALDO, PGCIERRE
endlocal

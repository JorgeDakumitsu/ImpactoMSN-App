@echo off
chcp 65001 > nul
echo ==============================
echo Instalando versiones Debug...
echo ==============================

cd 
call adb install ./app/build/outputs/apk/debug/app-debug.apk

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] La Instalacion ha fallado. Revisa los mensajes anteriores.
) else (
    echo.
    echo [EXITO] Instalacion completada con exito.
)

pause
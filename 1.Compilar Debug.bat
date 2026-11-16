@echo off
chcp 65001 > nul
echo ==============================
echo Compilando versiones Debug...
echo ==============================

call .\gradlew.bat :app:assembleDebug

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] La compilacion ha fallado. Revisa los mensajes anteriores.
) else (
    echo.
    echo [EXITO] Compilación completada con exito.
)

pause
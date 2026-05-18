@echo off
echo ==================================================
echo   INICIANDO SISTEMA INTEGRADO NEXTGEN-MOTORS
echo ==================================================
echo.

echo [1/2] Iniciando Servidor Principal (Puerto 8080)...
start "NextGen Main" cmd /k "mvnw spring-boot:run"

echo.
echo [2/2] Iniciando Cerebro de Dante (Puerto 8081)...
cd spring-ai
start "Dante AI" cmd /k "gradlew bootRun"

echo.
echo ==================================================
echo   AMBOS SERVIDORES ESTAN CARGANDO...
echo   Dante estara listo en: http://localhost:8081
echo   Dashboard en: http://localhost:8080
echo ==================================================
pause

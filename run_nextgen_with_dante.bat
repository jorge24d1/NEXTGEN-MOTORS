@echo off
echo ==================================================
echo   INICIANDO NEXTGEN-MOTORS (SISTEMA UNIFICADO)
echo ==================================================
echo.
echo [1/1] Iniciando servidor principal con MCP + IA...
echo       - Puerto:        http://localhost:8080
echo       - Chatbot Dante: /api/chatbot/mensaje
echo       - MCP endpoint:  /api/chatbot/mcp?message=...
echo       - MCP servers:   filesystem + dbhub (Docker)
echo.
start "NextGen Motors" cmd /k "mvnw spring-boot:run"

echo.
echo ==================================================
echo   SERVIDOR CARGANDO...
echo   Dashboard:    http://localhost:8080
echo   MCP (Docker): filesystem + dbhub activos
echo ==================================================
pause

package com.concesionario.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnClass(McpSyncClient.class)
@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResilientMcpConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilientMcpConfig.class);

    @Bean
    @Primary
    public ToolCallbackProvider resilientMcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
        List<ToolCallback> allCallbacks = new ArrayList<>();

        for (McpSyncClient client : mcpClients) {
            try {
                SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(client);
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks != null) {
                    allCallbacks.addAll(List.of(callbacks));
                }
                log.info("✅ Herramientas MCP cargadas correctamente de: {}", client);
            } catch (Exception e) {
                log.warn("⚠️ Servidor MCP no disponible o falló al listar herramientas: {}. La aplicación continuará funcionando.", e.getMessage());
            }
        }

        return () -> allCallbacks.toArray(new ToolCallback[0]);
    }
}


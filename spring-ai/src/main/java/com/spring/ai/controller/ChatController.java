package com.spring.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/mcp")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder, Optional<ToolCallbackProvider> toolCallbackProvider) {
        ChatClient.Builder builder = chatClientBuilder;
        if (toolCallbackProvider.isPresent()) {
            builder = builder.defaultToolCallbacks(toolCallbackProvider.get());
        }
        this.chatClient = builder.build();
    }

    @GetMapping("/postgre")
    public String getPostgre(@RequestParam("message") String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}

package com.liubo.rag.knowledge.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.liubo.rag.knowledge.constant.OllamaConstant.DEEP_SEEK_MODEL;

/**
 * @author 68
 * 2026/1/30 21:34
 */
@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Bean("ollamaChatModel")
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder()
                        .baseUrl(baseUrl)
                        .build())
                .defaultOptions(OllamaOptions.builder()
                        .model(DEEP_SEEK_MODEL)
                        .build())
                .build();
    }
}

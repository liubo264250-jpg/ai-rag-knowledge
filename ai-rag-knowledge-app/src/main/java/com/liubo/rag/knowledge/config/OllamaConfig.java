package com.liubo.rag.knowledge.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.liubo.rag.knowledge.constant.OllamaConstant.DEEP_SEEK_MODEL;
import static com.liubo.rag.knowledge.constant.OllamaConstant.NOMIC_EMBED_TEXT;

/**
 * @author 68
 * 2026/1/30 21:34
 */
@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder().baseUrl(baseUrl).build();
    }


    @Bean("ollamaChatModel")
    public OllamaChatModel ollamaChatModel(OllamaApi  ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder().model(DEEP_SEEK_MODEL).build())
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    @Bean
    public PgVectorStore pgVectorStore(JdbcTemplate jdbcTemplate,
                                       OllamaApi ollamaApi) {
        EmbeddingModel embeddingModel = new OllamaEmbeddingModel(
                ollamaApi,
                OllamaOptions.builder().model(NOMIC_EMBED_TEXT).build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions.defaults()
        );
        return PgVectorStore.builder(jdbcTemplate,embeddingModel).build();
    }
}

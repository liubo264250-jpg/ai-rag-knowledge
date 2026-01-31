package com.liubo.rag.knowledge.tigger.http;


import com.liubo.rag.knowledge.api.IAiService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 68
 * 2026/1/29 08:58
 */
@RestController
@RequestMapping("/api/v1/ollama/")
public class OllamaController implements IAiService {

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    private PgVectorStore pgVectorStore;

    @Override
    @GetMapping("/generate")
    public ChatResponse generate(@RequestParam("model") String model,@RequestParam("message") String message) {
        return ollamaChatModel.call(
                new Prompt(message,
                        OllamaOptions.builder().model(model).build())
        );
    }

    @Override
    @GetMapping("/generateStream")
    public Flux<ChatResponse> generateStream(@RequestParam("model")String model, @RequestParam("message") String message) {
        return ollamaChatModel.stream(
                new Prompt(message,
                        OllamaOptions.builder().model(model).build())
        );
    }
    @Override
    @GetMapping("/generateStreamRag")
    public Flux<ChatResponse> generateStreamRag(@RequestParam("model")String model, @RequestParam("message") String message) {
        String SYSTEM_PROMPT = """
            Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
            If unsure, simply state that you don't know.
            Another thing you need to note is that your reply must be in Chinese!
            DOCUMENTS:
                {documents}
            """;

        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(5)
                .filterExpression("knowledge == '知识库名称'")
                .build();

        List<Document> documents = pgVectorStore.similaritySearch(searchRequest);
        String documentCollectors = documents.stream().map(Document::getText).collect(Collectors.joining());
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentCollectors));
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        return ollamaChatModel.stream(
                new Prompt(message,
                        OllamaOptions.builder().model(model).build())
        );
    }
}

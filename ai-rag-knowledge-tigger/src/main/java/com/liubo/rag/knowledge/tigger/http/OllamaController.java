package com.liubo.rag.knowledge.tigger.http;


import com.liubo.rag.knowledge.api.IAiService;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author 68
 * 2026/1/29 08:58
 */
@RestController
@RequestMapping("/api/v1/ollama/")
public class OllamaController implements IAiService {

    @Autowired
    private OllamaChatModel ollamaChatModel;

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
}

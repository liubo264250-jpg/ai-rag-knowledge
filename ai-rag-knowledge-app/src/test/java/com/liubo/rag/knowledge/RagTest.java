package com.liubo.rag.knowledge;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 68
 * 2026/1/30 23:07
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RagTest {

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private OllamaChatModel ollamaChatModel;

    @Resource
    private PgVectorStore pgVectorStore;

    @Test
    public void upload(){
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader("./data/test.md");
        List<Document> splitterDocumentList = tokenTextSplitter.apply(tikaDocumentReader.read());
        splitterDocumentList.forEach(document -> document.getMetadata().put("knowledge","知识库名称"));
        pgVectorStore.accept(splitterDocumentList);
    }

    @Test
    public void chat(){
        String message = "刘博，哪年出生";
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
        ChatResponse call = ollamaChatModel.call(new Prompt(messages));
        log.info("测试结果：{}", JSON.toJSONString(call));
    }
}

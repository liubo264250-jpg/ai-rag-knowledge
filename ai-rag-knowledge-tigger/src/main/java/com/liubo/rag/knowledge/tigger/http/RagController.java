package com.liubo.rag.knowledge.tigger.http;

import cn.hutool.core.collection.CollUtil;
import com.liubo.rag.knowledge.api.IRagService;
import com.liubo.rag.knowledge.api.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static com.liubo.rag.knowledge.tigger.constant.Constant.SUCCESS;
import static com.liubo.rag.knowledge.tigger.constant.Constant.SUCCESS_INFO;
import static com.liubo.rag.knowledge.tigger.constant.RedisConstant.RAG_TAG;


/**
 * @author 68
 * 2026/1/31 15:30
 */
@RestController
@RequestMapping("/api/v1/rag")
@Slf4j
public class RagController implements IRagService {

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @GetMapping("/queryRagTagList")
    public Response<List<String>> queryRagTagList() {
        List<String> ragTagList = Optional.ofNullable(stringRedisTemplate.opsForList().range(RAG_TAG, 0, -1)).orElse(List.of());
        return Response.<List<String>>builder().code(SUCCESS).info(SUCCESS_INFO).data(ragTagList).build();
    }

    @Override
    @PostMapping("/uploadFile")
    public Response<String> uploadFile(@RequestParam("ragTag") String ragTag, @RequestParam("files") List<MultipartFile> files) {
        log.info("上传知识库:{}", ragTag);
        for (MultipartFile file : files) {
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            if (CollUtil.isNotEmpty(documentReader.get())) {
                List<Document> documentList = tokenTextSplitter.apply(documentReader.get());
                documentList.forEach(document -> document.getMetadata().put("knowledge", ragTag));
                pgVectorStore.accept(documentList);
            }
            List<String> elements = Optional.ofNullable(stringRedisTemplate.opsForList().range(RAG_TAG, 0, -1)).orElse(List.of());
            if (!elements.contains(ragTag)) {
                stringRedisTemplate.opsForList().leftPush(RAG_TAG, ragTag);
            }
        }
        log.info("上传知识库完成 {}", ragTag);
        return Response.<String>builder().code(SUCCESS).info(SUCCESS_INFO).data(ragTag).build();
    }
}

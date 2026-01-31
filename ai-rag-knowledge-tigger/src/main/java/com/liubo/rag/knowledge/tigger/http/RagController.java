package com.liubo.rag.knowledge.tigger.http;

import cn.hutool.core.collection.CollUtil;
import com.liubo.rag.knowledge.api.IRagService;
import com.liubo.rag.knowledge.api.response.Response;
import jakarta.annotation.Resource;
import jodd.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;

import static com.liubo.rag.knowledge.tigger.constant.Constant.*;
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

    @Override
    @PostMapping("/analyzeGitRepository")
    public Response<String> analyzeGitRepository(@RequestParam("repoUrl") String repoUrl,
                                                 @RequestParam("userName") String userName,
                                                 @RequestParam("token") String token) {
        String localPath = GIT_CLONED_REPO;
        Git git = null;
        try {
            String repoProjectName = extractProjectName(repoUrl);
            log.info("克隆路径：{}", new File(localPath).getAbsolutePath());
            FileUtils.deleteDirectory(new File(localPath));

            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                    .call();
            Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    log.info("{} 遍历解析路径，上传知识库:{}", repoProjectName, file.getFileName());
                    try {
                        TikaDocumentReader documentReader = new TikaDocumentReader(new PathResource(file));
                        if (CollUtil.isNotEmpty(documentReader.get())) {
                            List<Document> documentList = tokenTextSplitter.apply(documentReader.get());
                            documentList.forEach(document -> document.getMetadata().put("knowledge", repoProjectName));
                            pgVectorStore.accept(documentList);
                        }
                    }catch (Exception e){
                        log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
            FileUtil.deleteDir(new File(localPath));

            List<String> elements = Optional.ofNullable(stringRedisTemplate.opsForList().range(RAG_TAG, 0, -1)).orElse(List.of());
            if (!elements.contains(repoProjectName)) {
                stringRedisTemplate.opsForList().leftPush(RAG_TAG, repoProjectName);
            }
            git.close();
            log.info("遍历解析路径，上传完成:{}", repoUrl);
            return Response.<String>builder().code(SUCCESS).info("调用成功").build();
        } catch (GitAPIException e) {
            log.error("Git操作失败：{} - {}", repoUrl, e.getMessage(), e);
            return Response.<String>builder()
                    .code(ERROR)
                    .info("Git克隆失败：" + e.getMessage())
                    .build();
        } catch (IOException e) {
            log.error("文件操作失败：{} - {}", repoUrl, e.getMessage(), e);
            return Response.<String>builder()
                    .code(ERROR)
                    .info("文件处理失败：" + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("分析Git仓库异常：{} - {}", repoUrl, e.getMessage(), e);
            return Response.<String>builder()
                    .code(ERROR)
                    .info("系统异常：" + e.getMessage())
                    .build();
        } finally {
            // 6. 资源清理
            if (git != null) {
                try {
                    git.close();
                    log.debug("Git资源已关闭");
                } catch (Exception e) {
                    log.warn("关闭Git资源失败：{}", e.getMessage());
                }
            }
            // 清理本地目录
            try {
                FileUtils.deleteDirectory(new File(localPath));
                log.debug("本地目录清理完成：{}", localPath);
            } catch (Exception e) {
                log.warn("清理本地目录失败：{} - {}", localPath, e.getMessage());
            }
        }
    }


    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        return parts[parts.length - 1];
    }
}

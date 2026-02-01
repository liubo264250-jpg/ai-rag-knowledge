package com.liubo.rag.knowledge.api;

import com.liubo.rag.knowledge.api.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 68
 * 2026/1/31 15:22
 */
public interface IRagService {
    /**
     * 获取知识库标签
     * @return
     */
    Response<List<String>> queryRagTagList();

    /**
     * 上传知识库
     * @param ragTag
     * @param files
     * @return
     */
    Response<String> uploadFile(String ragTag, List<MultipartFile> files);

    /**
     * git仓库代码库解析上传到知识库
     * @param repoUrl
     * @param userName
     * @param token
     * @return
     */
    Response<String> analyzeGitRepository(String repoUrl,String userName, String token) ;
}

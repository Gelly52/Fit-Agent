package com.itgeo.service;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 文档向量化与检索服务。
 *
 * 说明：
 * 1. 当前仅服务手动 RAG 上传/检索接口；
 * 2. Phase 1 不自动接入 /chat/doChat 或 /agent/execute；
 * 3. userId 参数用于约束文档归属范围。
 */
public interface DocumentService {

    /**
     * 加载文本文件并写入向量库。
     *
     * @param resource 文本文件资源
     * @param fileName 文件名
     * @param userId 当前用户主键
     * @return 原始读取出的文档列表
     */
    List<Document> loadText(Resource resource, String fileName, Long userId);

    /**
     * 根据问题检索当前用户可见的文档片段。
     *
     * @param question 提问内容
     * @param userId 当前用户主键
     * @param topK 期望返回数量
     * @return 检索结果
     */
    List<Document> doSearch(String question, Long userId, Integer topK);
}

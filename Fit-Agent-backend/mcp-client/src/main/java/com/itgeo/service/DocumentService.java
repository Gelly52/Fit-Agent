package com.itgeo.service;


import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

public interface DocumentService {

    /*
     * 加载文本文件并且读取数据进行保存
     * @param resource 文本文件资源
     * @param fileName 文件名
     */
    public List<Document> loadText(Resource resource, String fileName);

    /**
     * 根据提问搜索向量数据库知识和相关文档（相似查询）
     * @param question 提问
     * @return 搜索结果
     */
    public List<Document> doSearch(String question);
}

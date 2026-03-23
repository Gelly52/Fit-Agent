package com.itgeo.service;

import com.itgeo.bean.SearchResult;
import org.springframework.ai.document.Document;

import java.util.List;

public interface SearXngService {

    /**
     * 调用本地搜索引擎SearXng联网搜索
     * @param query 搜索关键词
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query);

}

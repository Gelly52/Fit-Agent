package com.itgeo.service;

import com.itgeo.bean.SearchResult;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * SearXng 联网搜索服务契约。
 * <p>
 * 根据查询词返回可供上层问答链路消费的搜索结果列表。
 */
public interface SearXngService {

    /**
     * 调用 SearXng 执行一次联网搜索。
     *
     * @param query 搜索关键词
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query);

}

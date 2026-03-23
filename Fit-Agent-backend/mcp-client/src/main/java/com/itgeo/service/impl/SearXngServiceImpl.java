package com.itgeo.service.impl;

import cn.hutool.json.JSONUtil;
import com.itgeo.bean.SearXngResponse;
import com.itgeo.bean.SearchResult;
import com.itgeo.service.SearXngService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearXngServiceImpl implements SearXngService {

    @Value("${internet.websearch.searxng.url}")
    private String SEARXNG_URL;
    @Value("${internet.websearch.searxng.counts}")
    private Integer SEARXNG_COUNTS;

    private final OkHttpClient okHttpClient;

    @Override
    public List<SearchResult> search(String query) {

        // 构建SearXng搜索URL
        HttpUrl url = HttpUrl.get(SEARXNG_URL)
                .newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
//                .addQueryParameter("count", SEARXNG_COUNTS.toString())
                .build();
        log.info("搜索的SearXng url地址为: {}", url.url());

        // 构建SearXng搜索请求
        Request request = new Request.Builder()
                .url(url)
                .build();

        //发送请求
        try (Response response = okHttpClient.newCall(request).execute()) {
            // 解析响应体, 判断响应是否成功
            if (!response.isSuccessful()) {
                throw new IOException("SearXng搜索请求失败: " + response.code());
            }

            // 获得响应体数据
            if (response.body() != null) {
                String responseBody = response.body().string();
                log.info("<UNK>SearXng <UNK>: {}", responseBody);
                // 解析响应体, 转换为SearXngResponse对象
                SearXngResponse searXngResponse = JSONUtil.toBean(responseBody, SearXngResponse.class);
                // 处理SearXng搜索结果, 截取限制的个数

                return dealResults(searXngResponse.getResults());
            }
            log.error("搜索失败：{}", response.message());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }


    /**
     * 处理SearXng搜索结果, 截取限制的个数
     *
     * @param results
     * @return
     */
    private List<SearchResult> dealResults(List<SearchResult> results) {

        return results.subList(0, Math.min(SEARXNG_COUNTS, results.size()))
                .parallelStream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(SEARXNG_COUNTS)
                .toList();
    }


}

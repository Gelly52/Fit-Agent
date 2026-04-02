package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * SearXng 搜索响应。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SearXngResponse {

    /** 原始查询词。 */
    private String query;
    /** 搜索结果列表。 */
    private List<SearchResult> results;

}

package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 联网搜索结果
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult {

    private String title;
    private String url;
    private String content;
    private double score;

}

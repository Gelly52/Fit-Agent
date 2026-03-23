package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * SearXng搜索响应
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SearXngResponse {

    private String query;
    private List<SearchResult> results;

}

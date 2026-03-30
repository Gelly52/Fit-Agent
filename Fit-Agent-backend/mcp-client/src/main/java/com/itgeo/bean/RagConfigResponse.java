package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 手动 RAG 配置响应。
 *
 * 说明：
 * 1. defaultTopK / maxTopK 约束前端检索条数选择；
 * 2. filterScanLimit 表示扩大召回时的最大扫描量；
 * 3. userIsolationEnabled / isolationStrategy 用于说明当前隔离策略。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagConfigResponse {
    private Integer defaultTopK;
    private Integer maxTopK;
    private Integer filterScanLimit;
    private Boolean userIsolationEnabled;
    private String isolationStrategy;
}

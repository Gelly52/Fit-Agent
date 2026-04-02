package com.itgeo.bean.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * bge-m3 向量化响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BgeM3EmbedResponse {

    /** 模型名称。 */
    private String model;
    /** 向量维度。 */
    private Integer dimension;
    /** 向量结果列表。 */
    private List<Item> data;

    /**
     * 单条向量结果。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        /** 输入文本在请求列表中的下标。 */
        private Integer index;
        /** 向量数据。 */
        private List<Float> embedding;
    }
}

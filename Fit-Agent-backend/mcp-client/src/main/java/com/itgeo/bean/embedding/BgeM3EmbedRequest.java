package com.itgeo.bean.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * bge-m3 向量化请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BgeM3EmbedRequest {
    /** 待向量化的文本列表。 */
    private List<String> texts;
    /** 单批处理数量。 */
    private Integer batchSize;
    /** 单条文本最大长度。 */
    private Integer maxLength;
}

package com.itgeo.bean.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BgeM3EmbedRequest {
    private List<String> texts;
    private Integer batchSize;
    private Integer maxLength;
}
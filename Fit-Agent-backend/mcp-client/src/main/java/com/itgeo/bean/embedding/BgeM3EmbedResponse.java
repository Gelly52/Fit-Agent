package com.itgeo.bean.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BgeM3EmbedResponse {

    private String model;
    private Integer dimension;
    private List<Item> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Integer index;
        private List<Float> embedding;
    }
}
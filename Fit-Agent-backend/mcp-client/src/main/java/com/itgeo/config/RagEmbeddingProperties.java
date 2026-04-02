package com.itgeo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagEmbeddingProperties {

    private Embedding embedding = new Embedding();
    private Vectorstore vectorstore = new Vectorstore();

    @Data
    public static class Embedding {
        /**
         * transformers-default / bge-m3-http
         */
        private String provider = "transformers-default";
        private String serviceUrl = "http://127.0.0.1:7086";
        private String modelName = "bge-m3";
        private Integer dimension = 1024;
    }

    @Data
    public static class Vectorstore {
        private String indexName = "lee-vectorstore";
        private String prefix = "embedding:";
    }

}

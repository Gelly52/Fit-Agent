package com.itgeo.config;

import com.itgeo.service.embedding.BgeM3HttpEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RagRedisVectorStoreConfig {

    private final RagEmbeddingProperties ragEmbeddingProperties;

    @Bean(destroyMethod = "close")
    public JedisPooled jedisPooled(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password) {

        HostAndPort hostAndPort = new HostAndPort(host, port);

        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder();
        if (password != null && !password.isBlank()) {
            configBuilder.password(password);
        }

        return new JedisPooled(hostAndPort, configBuilder.build());
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "rag.embedding",
            name = "provider",
            havingValue = "transformers-default",
            matchIfMissing = true
    )
    public RedisVectorStore ragRedisVectorStoreDefault(
            JedisPooled jedisPooled,
            EmbeddingModel embeddingModel
    ) {
        log.info("使用默认 EmbeddingModel: {}", embeddingModel.getClass().getName());
        return buildRedisVectorStore(jedisPooled, embeddingModel);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "rag.embedding",
            name = "provider",
            havingValue = "bge-m3-http"
    )
    public RedisVectorStore ragRedisVectorStoreBgeM3Http(
            JedisPooled jedisPooled,
            OkHttpClient okHttpClient
    ) {
        BgeM3HttpEmbeddingModel embeddingModel = new BgeM3HttpEmbeddingModel(
                okHttpClient,
                ragEmbeddingProperties.getEmbedding().getServiceUrl(),
                ragEmbeddingProperties.getEmbedding().getModelName(),
                ragEmbeddingProperties.getEmbedding().getDimension()
        );

        log.info("使用 bge-m3 HTTP embedding 服务: {}", ragEmbeddingProperties.getEmbedding().getServiceUrl());
        return buildRedisVectorStore(jedisPooled, embeddingModel);
    }

    private RedisVectorStore buildRedisVectorStore(
            JedisPooled jedisPooled,
            EmbeddingModel embeddingModel
    ) {
        log.info("RAG Redis indexName={}", ragEmbeddingProperties.getVectorstore().getIndexName());
        log.info("RAG Redis prefix={}", ragEmbeddingProperties.getVectorstore().getPrefix());
        log.info("最终使用 EmbeddingModel: {}", embeddingModel.getClass().getName());

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(ragEmbeddingProperties.getVectorstore().getIndexName())
                .prefix(ragEmbeddingProperties.getVectorstore().getPrefix())
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("userId"),
                        RedisVectorStore.MetadataField.tag("fileName"),
                        RedisVectorStore.MetadataField.tag("source")
                )
                .initializeSchema(true)
                .build();
    }


//    @Bean
//    @Primary
//    public RedisVectorStore ragRedisVectorStore(
//            JedisPooled jedisPooled,
//            @Qualifier("ragEmbeddingModel") EmbeddingModel embeddingModel) {
//        log.info("RAG Redis indexName={}", ragEmbeddingProperties.getVectorstore().getIndexName());
//        log.info("RAG Redis prefix={}", ragEmbeddingProperties.getVectorstore().getPrefix());
//        log.info("最终使用 EmbeddingModel: {}", embeddingModel.getClass().getName());
//        return RedisVectorStore.builder(jedisPooled, embeddingModel)
//                .indexName(ragEmbeddingProperties.getVectorstore().getIndexName())
//                .prefix(ragEmbeddingProperties.getVectorstore().getPrefix())
//                .metadataFields(
//                        RedisVectorStore.MetadataField.tag("userId"),
//                        RedisVectorStore.MetadataField.tag("fileName"),
//                        RedisVectorStore.MetadataField.tag("source")
//                )
//                .initializeSchema(true)
//                .build();
//    }
}
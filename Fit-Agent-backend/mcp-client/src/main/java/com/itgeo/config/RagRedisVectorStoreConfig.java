package com.itgeo.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RagRedisVectorStoreConfig {

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
    public RedisVectorStore ragRedisVectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("lee-vectorstore")
                .prefix("embedding:")
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("userId"),
                        RedisVectorStore.MetadataField.tag("fileName"),
                        RedisVectorStore.MetadataField.tag("source")
                )
                .initializeSchema(true)
                .build();
    }
}
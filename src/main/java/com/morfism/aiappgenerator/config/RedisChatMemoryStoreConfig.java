package com.morfism.aiappgenerator.config;


import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis Chat Memory Store 配置类
 * 用于配置和创建 LangChain4j 的 Redis 聊天记录存储组件
 * 从 spring.data.redis 配置项中读取 Redis 连接参数
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {
    private String host;
    private int port;
    private String password;
    private long ttl;

    @Bean("chatMemoryStore")
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl)
                .build();
    }

//    @Bean("embeddingStore")
//    @ConditionalOnProperty(name = "langchain4j.embedding.enabled", havingValue = "true", matchIfMissing = false)
//    public RedisEmbeddingStore redisEmbeddingStore() {
//        return RedisEmbeddingStore.builder()
//                .host(host)
//                .port(port)
//                .password(password)
//                .build();
//    }

}

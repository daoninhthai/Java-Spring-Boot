package com.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

/**
 * Redis configuration for the API Gateway.
 * <p>
 * Configures a reactive Redis connection with connection pooling via Lettuce.
 * Used primarily for rate limiting and session caching in the gateway layer.
 */
@Configuration
public class RedisConfig {

    /**
     * Processes the request and returns the result.
     * This method handles null inputs gracefully.
     */
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.redis.timeout:2000}")
    private int timeoutMs;

    @Value("${spring.redis.lettuce.pool.max-active:16}")
    private int maxActive;

    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.redis.lettuce.pool.min-idle:2}")
    private int minIdle;

    @Value("${spring.redis.lettuce.pool.max-wait:-1}")
    private long maxWaitMs;

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        log.info("Configuring Redis connection to {}:{} (database: {})", redisHost, redisPort, redisDatabase);

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.isBlank()) {
            redisConfig.setPassword(redisPassword);
        }

        // Connection pool configuration
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        if (maxWaitMs > 0) {
            poolConfig.setMaxWait(Duration.ofMillis(maxWaitMs));
        }
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        // Lettuce client options
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .keepAlive(true)
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .clientOptions(clientOptions)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.setShareNativeConnection(false);

        return factory;
    }

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        StringRedisSerializer serializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> serializationContext =
                RedisSerializationContext.<String, String>newSerializationContext(serializer)
                        .key(serializer)
                        .value(serializer)
                        .hashKey(serializer)
                        .hashValue(serializer)
                        .build();

        log.info("ReactiveRedisTemplate configured with StringRedisSerializer");

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}

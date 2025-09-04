package com.netra.authrex.configs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.netra.authrex.configs.utils.RedisCustomProperties;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableRedisRepositories(basePackageClasses = {})
public class RedisConfig {

    private final RedisCustomProperties redisProps;

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        // Pool
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxIdle(redisProps.getLettuce().getPool().getMaxIdle());
        poolConfig.setMinIdle(redisProps.getLettuce().getPool().getMinIdle());
        poolConfig.setMaxTotal(redisProps.getLettuce().getPool().getMaxActive());
        poolConfig.setMaxWaitMillis(redisProps.getLettuce().getPool().getMaxWait());
        poolConfig.setTimeBetweenEvictionRunsMillis(
                redisProps.getLettuce().getPool().getTimeBetweenEvictionRunsMillis()
        );

        // Cluster refresh
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(redisProps.getLettuce().getRefreshPeriod()))
                .enableAllAdaptiveRefreshTriggers()
                .build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(redisProps.getLettuce().getRefreshPeriod())))
                .topologyRefreshOptions(topologyRefreshOptions)
                .build();

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisProps.getTimeout()))
                .poolConfig(poolConfig)
                .clientOptions(clusterClientOptions)
                .build();

        if (redisProps.isClusterEnabled()) {
            // Cluster mode
            String[] parts = redisProps.getCluster().getNodes().split(":");
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration()
                    .clusterNode(parts[0], Integer.parseInt(parts[1]));
            clusterConfig.setMaxRedirects(redisProps.getCluster().getMaxRedirects());

            if (redisProps.getPassword() != null) {
                clusterConfig.setPassword(redisProps.getPassword());
                if (redisProps.getUsername() != null) {
                    clusterConfig.setUsername(redisProps.getUsername());
                }
            }

            log.info("Starting Redis in CLUSTER mode with endpoint {} (user={})",
                    redisProps.getCluster().getNodes(), redisProps.getUsername());

            return new LettuceConnectionFactory(clusterConfig, clientConfig);

        } else {
            // Standalone mode
            RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(
                    redisProps.getStandalone().getHost(),
                    redisProps.getStandalone().getPort()
            );

            if (redisProps.getPassword() != null) {
                standaloneConfig.setPassword(redisProps.getPassword());
                if (redisProps.getUsername() != null) {
                    standaloneConfig.setUsername(redisProps.getUsername());
                }
            }

            log.info("Starting Redis in STANDALONE mode with host {}:{} (user={})",
                    redisProps.getStandalone().getHost(),
                    redisProps.getStandalone().getPort(),
                    redisProps.getUsername());

            return new LettuceConnectionFactory(standaloneConfig, clientConfig);
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(lettuceConnectionFactory);

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        jackson2JsonRedisSerializer.setObjectMapper(mapper);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}

package com.netra.authrex.configs.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedisCustomProperties {

    /**
     * Toggle between cluster-enabled and standalone mode.
     */
    private boolean clusterEnabled = true;

    /**
     * Connection timeout in ms
     */
    private long timeout = 1000;

    /**
     * Authentication (optional: only if AUTH/ACL enabled)
     */
    private String username;
    private String password;

    private Lettuce lettuce = new Lettuce();
    private Cluster cluster = new Cluster();
    private Standalone standalone = new Standalone();

    @Getter
    @Setter
    public static class Lettuce {
        private Pool pool = new Pool();
        private int refreshPeriod = 60;

        @Getter
        @Setter
        public static class Pool {
            private int maxIdle = 50;
            private int minIdle = 5;
            private int maxActive = 50;
            private long maxWait = -1;
            private long timeBetweenEvictionRunsMillis = 300000;
        }
    }

    @Getter
    @Setter
    public static class Cluster {
        /**
         * ElastiCache config endpoint, e.g.
         * my-redis-cluster.xxxxxx.clustercfg.use1.cache.amazonaws.com:6379
         */
        private String nodes;
        private int maxRedirects = 5;
    }

    @Getter
    @Setter
    public static class Standalone {
        /**
         * ElastiCache primary endpoint or localhost for dev
         */
        private String host = "localhost";
        private int port = 6379;
    }
}

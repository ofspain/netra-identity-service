package com.netra.authrex.configs.utils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EnvironmentAwareLogger {

    private final RedisCustomProperties redisProps;
    private final Environment environment;

    @PostConstruct
    public void logConfigs() {
        String activeProfiles = String.join(", ", environment.getActiveProfiles());
        if (activeProfiles.isEmpty()) {
            activeProfiles = "default";
        }

        delimiter("start env logging");
        log.info(" Active Spring Profiles : {}", activeProfiles);
        logRedisConfig();

    }

    private void delimiter(String message) {
        log.info("------------------------------ {} ----------------------------------------", message.toUpperCase());
    }

    private void logRedisConfig(){
        delimiter("start redis properties logging");

        log.info(" Redis Mode             : {}", redisProps.isClusterEnabled() ? "CLUSTER" : "STANDALONE");

        if (redisProps.isClusterEnabled()) {
            log.info(" Redis Cluster Endpoint : {}", redisProps.getCluster().getNodes());
            log.info(" Redis Max Redirects    : {}", redisProps.getCluster().getMaxRedirects());
        } else {
            log.info(" Redis Host             : {}", redisProps.getStandalone().getHost());
            log.info(" Redis Port             : {}", redisProps.getStandalone().getPort());
        }

        if (redisProps.getUsername() != null) {
            log.info(" Redis User             : {}", redisProps.getUsername());
        } else {
            log.info(" Redis User             : [default user or unauthenticated]");
        }

        if (redisProps.getPassword() != null) {
            log.info(" Redis Password         : [PROVIDED]");
        } else {
            log.info(" Redis Password         : [NONE]");
        }

        log.info(" Pool Max Active        : {}", redisProps.getLettuce().getPool().getMaxActive());
        log.info(" Pool Max Idle          : {}", redisProps.getLettuce().getPool().getMaxIdle());
        log.info(" Pool Min Idle          : {}", redisProps.getLettuce().getPool().getMinIdle());
        log.info(" Pool Max Wait (ms)     : {}", redisProps.getLettuce().getPool().getMaxWait());
        log.info(" Refresh Period (sec)   : {}", redisProps.getLettuce().getRefreshPeriod());
        log.info(" Timeout (ms)           : {}", redisProps.getTimeout());
        delimiter("end redis properties logging");
    }
}

package com.netra.authrex.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DatasourceConfig {


    @Bean
    @ConfigurationProperties
    public DataSourceProperties writeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "writeDataSource")
    @ConfigurationProperties
    public DataSource writeDataSource() {
        log.info("Writing DataSource, Properties: {}", writeDataSourceProperties());
        return writeDataSourceProperties().initializeDataSourceBuilder().build();
    }
}

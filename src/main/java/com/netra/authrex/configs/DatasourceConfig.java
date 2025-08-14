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
    @ConfigurationProperties("write.datasource")
    public DataSourceProperties writeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "writeDataSource")
    @ConfigurationProperties("write.datasource.hikari")
    public DataSource writeDataSource() {
        return writeDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
}

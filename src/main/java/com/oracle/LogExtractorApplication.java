package com.oracle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        FileStorageProperties.class
})
public class LogExtractorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogExtractorApplication.class, args);
    }
}

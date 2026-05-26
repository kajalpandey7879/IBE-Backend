package com.example.ibe.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@EnableConfigurationProperties(AwsSesProperties.class)
public class AwsSesConfig {

    @Bean
    public SesClient sesClient(AwsSesProperties awsSesProperties) {
        return SesClient.builder()
                .region(Region.of(awsSesProperties.getRegion()))
                .build();
    }
}

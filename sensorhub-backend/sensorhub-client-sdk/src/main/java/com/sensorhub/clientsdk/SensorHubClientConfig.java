package com.sensorhub.clientsdk;

import com.sensorhub.clientsdk.client.SensorHubClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * SensorHub client configuration.
 */
@Configuration
@ConfigurationProperties("sensorhub.client")
@Data
@ComponentScan
public class SensorHubClientConfig {

    private String accessKey;

    private String secretKey;

    private String gatewayHost = "http://localhost:8290";

    @Bean
    public SensorHubClient sensorHubClient() {
        return new SensorHubClient(accessKey, secretKey, gatewayHost);
    }
}

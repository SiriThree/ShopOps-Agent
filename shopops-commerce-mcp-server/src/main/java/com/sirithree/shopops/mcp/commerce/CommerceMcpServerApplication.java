package com.sirithree.shopops.mcp.commerce;

import com.sirithree.shopops.mcp.commerce.config.CommerceMcpServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CommerceMcpServerProperties.class)
public class CommerceMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommerceMcpServerApplication.class, args);
    }
}

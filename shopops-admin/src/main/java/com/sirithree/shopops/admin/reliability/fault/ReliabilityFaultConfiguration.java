package com.sirithree.shopops.admin.reliability.fault;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReliabilityFaultConfiguration {
    @Bean
    @ConditionalOnMissingBean(ReliabilityFaultController.class)
    public ReliabilityFaultController reliabilityFaultController() {
        return (point, context) -> { };
    }
}

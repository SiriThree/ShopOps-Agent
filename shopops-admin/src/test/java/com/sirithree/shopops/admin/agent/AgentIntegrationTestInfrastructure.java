package com.sirithree.shopops.admin.agent;

import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.mcp.support.InMemoryCommerceMcpClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
class AgentIntegrationTestInfrastructure {
    @Bean
    @Primary
    InMemoryCommerceMcpClient inMemoryCommerceMcpClient(CommentRiskService commentRiskService) {
        return new InMemoryCommerceMcpClient(commentRiskService);
    }
}

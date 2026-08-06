package com.sirithree.shopops.admin.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityConfigurationValidatorTest {
    @Test
    void shouldRejectDevelopmentAuthenticationInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductionSecurityConfigurationValidator validator = new ProductionSecurityConfigurationValidator(
                environment, true, "a-secure-token-secret-with-more-than-32-characters",
                "a-secure-credential-secret-with-more-than-32-characters");
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("header development authentication");
    }

    @Test
    void shouldRejectDefaultSecretsInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductionSecurityConfigurationValidator validator = new ProductionSecurityConfigurationValidator(
                environment, false, "shopops-dev-token-secret-change-me",
                "shopops-dev-connector-secret-change-me");
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shopops.auth.token-secret");
    }
}

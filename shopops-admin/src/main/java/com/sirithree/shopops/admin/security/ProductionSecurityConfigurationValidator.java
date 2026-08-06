package com.sirithree.shopops.admin.security;

import java.util.Arrays;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityConfigurationValidator implements InitializingBean {
    private static final String DEFAULT_TOKEN = "shopops-dev-token-secret-change-me";
    private static final String DEFAULT_CREDENTIAL = "shopops-dev-connector-secret-change-me";

    private final Environment environment;
    private final boolean headerDevMode;
    private final String tokenSecret;
    private final String credentialSecret;

    public ProductionSecurityConfigurationValidator(Environment environment,
            @Value("${shopops.auth.header-dev-mode:false}") boolean headerDevMode,
            @Value("${shopops.auth.token-secret:}") String tokenSecret,
            @Value("${shopops.connector.credential-secret:}") String credentialSecret) {
        this.environment = environment;
        this.headerDevMode = headerDevMode;
        this.tokenSecret = tokenSecret;
        this.credentialSecret = credentialSecret;
    }

    @Override
    public void afterPropertiesSet() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }
        if (headerDevMode) {
            throw new IllegalStateException("Production profile forbids header development authentication");
        }
        requireStrongSecret("shopops.auth.token-secret", tokenSecret, DEFAULT_TOKEN);
        requireStrongSecret("shopops.connector.credential-secret", credentialSecret, DEFAULT_CREDENTIAL);
    }

    private void requireStrongSecret(String property, String value, String knownDefault) {
        if (value == null || value.isBlank() || value.equals(knownDefault) || value.length() < 32) {
            throw new IllegalStateException("Production requires a non-default secret of at least 32 characters: " + property);
        }
    }
}

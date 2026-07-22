package com.sirithree.shopops.admin.connector;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.connector.service.impl.ConnectorCredentialCrypto;
import org.junit.jupiter.api.Test;

class ConnectorCredentialCryptoTest {
    @Test
    void shouldEncryptWithoutPlainTextAndDecrypt() {
        ConnectorCredentialCrypto crypto = new ConnectorCredentialCrypto("unit-test-secret");

        String encrypted = crypto.encrypt("sk-live-secret-001");

        assertThat(encrypted)
                .startsWith("v1:")
                .doesNotContain("sk-live-secret-001");
        assertThat(crypto.decrypt(encrypted)).isEqualTo("sk-live-secret-001");
        assertThat(crypto.encrypt("sk-live-secret-001")).isNotEqualTo(encrypted);
    }
}

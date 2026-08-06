package com.sirithree.shopops.admin.persistence.component;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class FlywayMigrationRunner implements ApplicationRunner {
    private final DataSource dataSource;
    private final boolean enabled;
    private final String baselineVersion;

    public FlywayMigrationRunner(DataSource dataSource,
                                 @Value("${shopops.flyway.enabled:true}") boolean enabled,
                                 @Value("${shopops.flyway.baseline-version:3}") String baselineVersion) {
        this.dataSource = dataSource;
        this.enabled = enabled;
        this.baselineVersion = baselineVersion;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(baselineVersion)
                .load()
                .migrate();
    }
}

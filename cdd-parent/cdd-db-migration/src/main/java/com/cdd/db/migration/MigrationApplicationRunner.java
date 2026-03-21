package com.cdd.db.migration;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MigrationApplicationRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final MigrationProperties properties;

    public MigrationApplicationRunner(DataSource dataSource, MigrationProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(properties.getLocations().toArray(String[]::new))
                .validateMigrationNaming(properties.isValidateMigrationNaming())
                .connectRetries(properties.getConnectRetries())
                .load();
        flyway.migrate();
    }
}


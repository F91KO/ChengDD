package com.cdd.db.migration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cdd.migration")
public class MigrationProperties {

    private List<String> locations = new ArrayList<>(List.of("filesystem:db/migration"));
    private boolean validateMigrationNaming = true;
    private int connectRetries = 3;

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public boolean isValidateMigrationNaming() {
        return validateMigrationNaming;
    }

    public void setValidateMigrationNaming(boolean validateMigrationNaming) {
        this.validateMigrationNaming = validateMigrationNaming;
    }

    public int getConnectRetries() {
        return connectRetries;
    }

    public void setConnectRetries(int connectRetries) {
        this.connectRetries = connectRetries;
    }
}


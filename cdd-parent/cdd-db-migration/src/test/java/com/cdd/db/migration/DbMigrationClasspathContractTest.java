package com.cdd.db.migration;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import liquibase.ChecksumVersion;
import liquibase.Scope;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringResourceAccessor;
import liquibase.parser.ChangeLogParserFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbMigrationClasspathContractTest {

    private static final String CHANGELOG_RESOURCE = "classpath:config/db-migration/db.changelog-master.yaml";
    private static final String LEGACY_LOGICAL_FILE_PATH =
            "file:../../config/db-migration/db.changelog-master.yaml";

    @Test
    void packagedChangelogPreservesExistingDatabaseIdentitiesAndChecksums() throws Exception {
        Map<String, String> expectedChecksums = expectedChecksums();

        try (SpringResourceAccessor resourceAccessor = new SpringResourceAccessor(
                new DefaultResourceLoader(getClass().getClassLoader()))) {
            DatabaseChangeLog changeLog = parse(resourceAccessor);
            Set<String> actualIds = new LinkedHashSet<>();

            Scope.child(Scope.Attr.resourceAccessor, resourceAccessor, () -> {
                for (ChangeSet changeSet : changeLog.getChangeSets()) {
                    assertEquals(LEGACY_LOGICAL_FILE_PATH, changeSet.getFilePath(),
                            () -> "logical changelog identity changed for " + changeSet.getId());
                    String storedChecksum = expectedChecksums.get(changeSet.getId());
                    String generatedChecksum = changeSet.generateCheckSum(ChecksumVersion.V9).toString();
                    boolean explicitlyAccepted = changeSet.getValidCheckSums().stream()
                            .anyMatch(checksum -> checksum.toString().equals(storedChecksum));
                    assertTrue(storedChecksum.equals(generatedChecksum) || explicitlyAccepted,
                            () -> "historical checksum is no longer valid for " + changeSet.getId()
                                    + "; generated=" + generatedChecksum);
                    actualIds.add(changeSet.getId());
                }
            });

            assertEquals(expectedChecksums.keySet(), actualIds);
        }
    }

    private static DatabaseChangeLog parse(SpringResourceAccessor resourceAccessor) throws LiquibaseException {
        return ChangeLogParserFactory.getInstance()
                .getParser(CHANGELOG_RESOURCE, resourceAccessor)
                .parse(CHANGELOG_RESOURCE, new ChangeLogParameters(), resourceAccessor);
    }

    private static Map<String, String> expectedChecksums() {
        return Map.ofEntries(
                Map.entry("V1__auth_and_config", "9:728c6da289251c3e29cbfce3f4eacfe0"),
                Map.entry("V2__merchant", "9:26645b69d9919189361c332b8ed56cba"),
                Map.entry("V3__decoration", "9:a5d06721c36ab126a5f10c83256f455f"),
                Map.entry("V4__product", "9:459851a26b940ff32a1de5644bcc51eb"),
                Map.entry("V5__order", "9:425e5d629965d71702905619783f6bef"),
                Map.entry("V6__release", "9:356f9c36d2c32039db2c3213a0806b6b"),
                Map.entry("V7__marketing_and_report", "9:e7ae32750335c1ad4b677b4c07daa60c"),
                Map.entry("V8__seed_data", "9:831323a8b9c111009ec6157089991b19"),
                Map.entry("V9__idempotency_and_compensation", "9:b8dc731df5f5c3ae87d20c9b19ce8d55"),
                Map.entry("V10__auth_persistence", "9:c836ab3b5d76112382aee52e0ad5a612"),
                Map.entry("V11__auth_seed_accounts", "9:1b8d573860d7e5b9181f21898c60f9fb"),
                Map.entry("V12__merchant_onboarding_baseline", "9:172d5aa92cedf93a1e14ebf85dfd4f21"),
                Map.entry("V13__release_governance_baseline", "9:53112769b91947c941932ec0d2407caf"),
                Map.entry("V14__config_switches_baseline", "9:948f2d5bcb2daf35fd5e4847dfe2fade"),
                Map.entry("V15__product_catalog_baseline", "9:1c8f9a97ea410b242c8e4af3d4cea773"),
                Map.entry("V16__order_baseline", "9:084b20de06c2a5fc96c126b87d68a874"),
                Map.entry("V17__order_item_level_after_sale_refund", "9:4d246afa3d7ef9c15a4492ec645a641b"),
                Map.entry("V18__local_demo_product_order_seed", "9:b6a127cdde3144c68586c16459b1ba22"),
                Map.entry("V19__local_demo_product_seed_patch", "9:92628a5e486b9eb06a3233b189279946"),
                Map.entry("V20__local_demo_report_seed", "9:2de700e74d247697b79d99222b768539"),
                Map.entry("V21__local_demo_dashboard_refresh", "9:7432d9d142f88ef028c9f575bc6a63a0"),
                Map.entry("V22__local_demo_after_sale_seed", "9:8b2e93e11e28f3f428e338c5b042e01e"),
                Map.entry("V23__local_demo_release_seed", "9:296660e63a237b36a2bc6a9118d41a2e"),
                Map.entry("V24__product_category_template_nodes_and_fresh_templates", "9:15a50913b7a8e93984086d3826dcc756"),
                Map.entry("V25__rename_product_category_template_names", "9:2addff4de57fe95175ebc8107b0ce5ef"),
                Map.entry("V26__order_shipping_fields", "9:44240a096d4423290e4eed5947b9fc8e"),
                Map.entry("V27__local_demo_order_shipping_patch", "9:b3e930b8118bc917f77170b5b5c08ed9"),
                Map.entry("V28__local_demo_after_sale_refunding_patch", "9:2de5b2711a8a261d3c22eb7491adb4d8"),
                Map.entry("V29__local_demo_after_sale_item_refund_status_patch", "9:a234ad81637f401e2abee14825398f22"),
                Map.entry("V30__merchant_account_permissions_and_roles", "9:ea58d9a5d6ba9c075c9031e90431cd45")
        );
    }
}

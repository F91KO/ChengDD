package com.cdd.decoration.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DecorationRepository {

    Optional<HomeConfigRecord> findHomeConfig(long merchantId, long storeId, long miniProgramId);

    List<HomeModuleRecord> listHomeModules(long homeConfigId);

    HomeConfigRecord saveHomeConfig(HomeConfigUpsertCommand command);

    void replaceHomeModules(long merchantId, long storeId, long homeConfigId, List<HomeModuleDraft> modules);

    void upsertTemplateBinding(long id,
                               long merchantId,
                               long storeId,
                               long miniProgramId,
                               String templateCode,
                               String styleMode,
                               String bindingStatus);

    void createHomeVersion(long id,
                           long merchantId,
                           long storeId,
                           long homeConfigId,
                           int versionNo,
                           String versionStatus,
                           String snapshotJson,
                           Instant publishedAt);

    List<HomeVersionRecord> listHomeVersions(long homeConfigId);

    record HomeConfigUpsertCommand(
            long id,
            long merchantId,
            long storeId,
            long miniProgramId,
            String homeTemplateCode,
            String homeStyleMode,
            String themeColor,
            String searchPlaceholder,
            String headerServiceBarJson,
            String brandHeroBlockJson,
            String deliveryPromiseText,
            String deliveryFeeText,
            String minimumOrderText,
            String announcementText,
            String status,
            int versionNo) {
    }

    record HomeModuleDraft(
            long id,
            String moduleType,
            String moduleName,
            int sortOrder,
            boolean enabled,
            String styleMode,
            String dataSourceType,
            Long dataSourceId,
            String jumpTargetJson,
            String configPayloadJson) {
    }

    record HomeConfigRecord(
            long id,
            long merchantId,
            long storeId,
            long miniProgramId,
            String homeTemplateCode,
            String homeStyleMode,
            String themeColor,
            String searchPlaceholder,
            String headerServiceBarJson,
            String brandHeroBlockJson,
            String deliveryPromiseText,
            String deliveryFeeText,
            String minimumOrderText,
            String announcementText,
            String status,
            int versionNo) {
    }

    record HomeModuleRecord(
            long id,
            long homeConfigId,
            String moduleType,
            String moduleName,
            int sortOrder,
            boolean enabled,
            String styleMode,
            String dataSourceType,
            Long dataSourceId,
            String jumpTargetJson,
            String configPayloadJson) {
    }

    record HomeVersionRecord(
            long id,
            long homeConfigId,
            int versionNo,
            String versionStatus,
            String snapshotJson) {
    }
}

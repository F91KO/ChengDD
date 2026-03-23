package com.cdd.report.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {

    HomeEventDailyRecord upsertHomeEventDaily(long id,
                                              long merchantId,
                                              long storeId,
                                              LocalDate statDate,
                                              Long miniProgramId,
                                              long pageViewCount,
                                              long visitorCount,
                                              long clickCount);

    List<HomeEventDailyRecord> listHomeEventDaily(long merchantId, long storeId, LocalDate startDate, LocalDate endDate);

    OrderDailyRecord upsertOrderDaily(long id,
                                      long merchantId,
                                      long storeId,
                                      LocalDate statDate,
                                      long orderCount,
                                      long paidOrderCount,
                                      BigDecimal grossAmount,
                                      BigDecimal refundAmount);

    List<OrderDailyRecord> listOrderDaily(long merchantId, long storeId, LocalDate startDate, LocalDate endDate);

    ProductDailyRecord upsertProductDaily(long id,
                                          long merchantId,
                                          long storeId,
                                          LocalDate statDate,
                                          long productId,
                                          Long skuId,
                                          long viewCount,
                                          long saleCount,
                                          BigDecimal saleAmount);

    List<ProductDailyRecord> listProductDaily(long merchantId,
                                              long storeId,
                                              Long productId,
                                              LocalDate startDate,
                                              LocalDate endDate);

    MerchantDashboardSnapshotRecord createMerchantDashboardSnapshot(long id,
                                                                    long merchantId,
                                                                    long storeId,
                                                                    Timestamp snapshotTime,
                                                                    String dashboardPayloadJson);

    Optional<MerchantDashboardSnapshotRecord> findLatestMerchantDashboardSnapshot(long merchantId, long storeId);

    List<MerchantDashboardSnapshotRecord> listMerchantDashboardSnapshots(Long merchantId, Long storeId);

    PlatformDashboardSnapshotRecord createPlatformDashboardSnapshot(long id,
                                                                    Timestamp snapshotTime,
                                                                    String dashboardPayloadJson);

    Optional<PlatformDashboardSnapshotRecord> findLatestPlatformDashboardSnapshot();

    record HomeEventDailyRecord(
            long id,
            long merchantId,
            long storeId,
            LocalDate statDate,
            Long miniProgramId,
            long pageViewCount,
            long visitorCount,
            long clickCount) {
    }

    record OrderDailyRecord(
            long id,
            long merchantId,
            long storeId,
            LocalDate statDate,
            long orderCount,
            long paidOrderCount,
            BigDecimal grossAmount,
            BigDecimal refundAmount) {
    }

    record ProductDailyRecord(
            long id,
            long merchantId,
            long storeId,
            LocalDate statDate,
            long productId,
            Long skuId,
            long viewCount,
            long saleCount,
            BigDecimal saleAmount) {
    }

    record MerchantDashboardSnapshotRecord(
            long id,
            long merchantId,
            long storeId,
            Timestamp snapshotTime,
            String dashboardPayloadJson) {
    }

    record PlatformDashboardSnapshotRecord(
            long id,
            Timestamp snapshotTime,
            String dashboardPayloadJson) {
    }
}

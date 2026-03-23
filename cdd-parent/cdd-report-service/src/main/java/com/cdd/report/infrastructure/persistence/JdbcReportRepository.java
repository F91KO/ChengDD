package com.cdd.report.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcReportRepository implements ReportRepository {

    private static final RowMapper<HomeEventDailyRecord> HOME_EVENT_ROW_MAPPER = (rs, rowNum) -> new HomeEventDailyRecord(
            rs.getLong("id"),
            rs.getLong("merchant_id"),
            rs.getLong("store_id"),
            rs.getDate("stat_date").toLocalDate(),
            rs.getObject("mini_program_id", Long.class),
            rs.getLong("page_view_count"),
            rs.getLong("visitor_count"),
            rs.getLong("click_count"));

    private static final RowMapper<OrderDailyRecord> ORDER_DAILY_ROW_MAPPER = (rs, rowNum) -> new OrderDailyRecord(
            rs.getLong("id"),
            rs.getLong("merchant_id"),
            rs.getLong("store_id"),
            rs.getDate("stat_date").toLocalDate(),
            rs.getLong("order_count"),
            rs.getLong("paid_order_count"),
            rs.getBigDecimal("gross_amount"),
            rs.getBigDecimal("refund_amount"));

    private static final RowMapper<ProductDailyRecord> PRODUCT_DAILY_ROW_MAPPER = (rs, rowNum) -> new ProductDailyRecord(
            rs.getLong("id"),
            rs.getLong("merchant_id"),
            rs.getLong("store_id"),
            rs.getDate("stat_date").toLocalDate(),
            rs.getLong("product_id"),
            rs.getObject("sku_id", Long.class),
            rs.getLong("view_count"),
            rs.getLong("sale_count"),
            rs.getBigDecimal("sale_amount"));

    private static final RowMapper<MerchantDashboardSnapshotRecord> MERCHANT_DASHBOARD_ROW_MAPPER = (rs, rowNum) -> new MerchantDashboardSnapshotRecord(
            rs.getLong("id"),
            rs.getLong("merchant_id"),
            rs.getLong("store_id"),
            rs.getTimestamp("snapshot_time"),
            rs.getString("dashboard_payload_json"));

    private static final RowMapper<PlatformDashboardSnapshotRecord> PLATFORM_DASHBOARD_ROW_MAPPER = (rs, rowNum) -> new PlatformDashboardSnapshotRecord(
            rs.getLong("id"),
            rs.getTimestamp("snapshot_time"),
            rs.getString("dashboard_payload_json"));

    private final JdbcTemplate jdbcTemplate;

    public JdbcReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public HomeEventDailyRecord upsertHomeEventDaily(long id,
                                                     long merchantId,
                                                     long storeId,
                                                     LocalDate statDate,
                                                     Long miniProgramId,
                                                     long pageViewCount,
                                                     long visitorCount,
                                                     long clickCount) {
        jdbcTemplate.update("""
                INSERT INTO cdd_report_home_event_daily (
                    id, merchant_id, store_id, stat_date, mini_program_id, page_view_count, visitor_count, click_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    merchant_id = VALUES(merchant_id),
                    mini_program_id = VALUES(mini_program_id),
                    page_view_count = VALUES(page_view_count),
                    visitor_count = VALUES(visitor_count),
                    click_count = VALUES(click_count),
                    updated_at = CURRENT_TIMESTAMP,
                    deleted = 0
                """,
                id,
                merchantId,
                storeId,
                Date.valueOf(statDate),
                miniProgramId,
                pageViewCount,
                visitorCount,
                clickCount);
        return findHomeEventDaily(merchantId, storeId, statDate);
    }

    @Override
    public List<HomeEventDailyRecord> listHomeEventDaily(long merchantId, long storeId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, merchant_id, store_id, stat_date, mini_program_id, page_view_count, visitor_count, click_count
                FROM cdd_report_home_event_daily
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.add(storeId);
        appendDateRange(sql, args, startDate, endDate);
        sql.append(" ORDER BY stat_date DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), HOME_EVENT_ROW_MAPPER, args.toArray());
    }

    @Override
    public OrderDailyRecord upsertOrderDaily(long id,
                                             long merchantId,
                                             long storeId,
                                             LocalDate statDate,
                                             long orderCount,
                                             long paidOrderCount,
                                             BigDecimal grossAmount,
                                             BigDecimal refundAmount) {
        jdbcTemplate.update("""
                INSERT INTO cdd_report_order_daily (
                    id, merchant_id, store_id, stat_date, order_count, paid_order_count, gross_amount, refund_amount
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    merchant_id = VALUES(merchant_id),
                    order_count = VALUES(order_count),
                    paid_order_count = VALUES(paid_order_count),
                    gross_amount = VALUES(gross_amount),
                    refund_amount = VALUES(refund_amount),
                    updated_at = CURRENT_TIMESTAMP,
                    deleted = 0
                """,
                id,
                merchantId,
                storeId,
                Date.valueOf(statDate),
                orderCount,
                paidOrderCount,
                grossAmount,
                refundAmount);
        return findOrderDaily(merchantId, storeId, statDate);
    }

    @Override
    public List<OrderDailyRecord> listOrderDaily(long merchantId, long storeId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, merchant_id, store_id, stat_date, order_count, paid_order_count, gross_amount, refund_amount
                FROM cdd_report_order_daily
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.add(storeId);
        appendDateRange(sql, args, startDate, endDate);
        sql.append(" ORDER BY stat_date DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), ORDER_DAILY_ROW_MAPPER, args.toArray());
    }

    @Override
    public ProductDailyRecord upsertProductDaily(long id,
                                                 long merchantId,
                                                 long storeId,
                                                 LocalDate statDate,
                                                 long productId,
                                                 Long skuId,
                                                 long viewCount,
                                                 long saleCount,
                                                 BigDecimal saleAmount) {
        jdbcTemplate.update("""
                INSERT INTO cdd_report_product_daily (
                    id, merchant_id, store_id, stat_date, product_id, sku_id, view_count, sale_count, sale_amount
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    merchant_id = VALUES(merchant_id),
                    sku_id = VALUES(sku_id),
                    view_count = VALUES(view_count),
                    sale_count = VALUES(sale_count),
                    sale_amount = VALUES(sale_amount),
                    updated_at = CURRENT_TIMESTAMP,
                    deleted = 0
                """,
                id,
                merchantId,
                storeId,
                Date.valueOf(statDate),
                productId,
                skuId,
                viewCount,
                saleCount,
                saleAmount);
        return findProductDaily(merchantId, storeId, productId, statDate);
    }

    @Override
    public List<ProductDailyRecord> listProductDaily(long merchantId,
                                                     long storeId,
                                                     Long productId,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, merchant_id, store_id, stat_date, product_id, sku_id, view_count, sale_count, sale_amount
                FROM cdd_report_product_daily
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.add(storeId);
        if (productId != null) {
            sql.append(" AND product_id = ?");
            args.add(productId);
        }
        appendDateRange(sql, args, startDate, endDate);
        sql.append(" ORDER BY stat_date DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), PRODUCT_DAILY_ROW_MAPPER, args.toArray());
    }

    @Override
    public MerchantDashboardSnapshotRecord createMerchantDashboardSnapshot(long id,
                                                                           long merchantId,
                                                                           long storeId,
                                                                           Timestamp snapshotTime,
                                                                           String dashboardPayloadJson) {
        jdbcTemplate.update("""
                INSERT INTO cdd_metric_merchant_dashboard (
                    id, merchant_id, store_id, snapshot_time, dashboard_payload_json
                ) VALUES (?, ?, ?, ?, ?)
                """,
                id,
                merchantId,
                storeId,
                snapshotTime,
                dashboardPayloadJson);
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, snapshot_time, dashboard_payload_json
                FROM cdd_metric_merchant_dashboard
                WHERE id = ?
                  AND deleted = 0
                """, MERCHANT_DASHBOARD_ROW_MAPPER, id).get(0);
    }

    @Override
    public Optional<MerchantDashboardSnapshotRecord> findLatestMerchantDashboardSnapshot(long merchantId, long storeId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, snapshot_time, dashboard_payload_json
                FROM cdd_metric_merchant_dashboard
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                ORDER BY snapshot_time DESC, id DESC
                LIMIT 1
                """, MERCHANT_DASHBOARD_ROW_MAPPER, merchantId, storeId).stream().findFirst();
    }

    @Override
    public List<MerchantDashboardSnapshotRecord> listMerchantDashboardSnapshots(Long merchantId, Long storeId) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, merchant_id, store_id, snapshot_time, dashboard_payload_json
                FROM cdd_metric_merchant_dashboard
                WHERE deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        if (merchantId != null) {
            sql.append(" AND merchant_id = ?");
            args.add(merchantId);
        }
        if (storeId != null) {
            sql.append(" AND store_id = ?");
            args.add(storeId);
        }
        sql.append(" ORDER BY snapshot_time DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), MERCHANT_DASHBOARD_ROW_MAPPER, args.toArray());
    }

    @Override
    public PlatformDashboardSnapshotRecord createPlatformDashboardSnapshot(long id,
                                                                           Timestamp snapshotTime,
                                                                           String dashboardPayloadJson) {
        jdbcTemplate.update("""
                INSERT INTO cdd_metric_platform_dashboard (
                    id, snapshot_time, dashboard_payload_json
                ) VALUES (?, ?, ?)
                """,
                id,
                snapshotTime,
                dashboardPayloadJson);
        return jdbcTemplate.query("""
                SELECT id, snapshot_time, dashboard_payload_json
                FROM cdd_metric_platform_dashboard
                WHERE id = ?
                  AND deleted = 0
                """, PLATFORM_DASHBOARD_ROW_MAPPER, id).get(0);
    }

    @Override
    public Optional<PlatformDashboardSnapshotRecord> findLatestPlatformDashboardSnapshot() {
        return jdbcTemplate.query("""
                SELECT id, snapshot_time, dashboard_payload_json
                FROM cdd_metric_platform_dashboard
                WHERE deleted = 0
                ORDER BY snapshot_time DESC, id DESC
                LIMIT 1
                """, PLATFORM_DASHBOARD_ROW_MAPPER).stream().findFirst();
    }

    private HomeEventDailyRecord findHomeEventDaily(long merchantId, long storeId, LocalDate statDate) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, stat_date, mini_program_id, page_view_count, visitor_count, click_count
                FROM cdd_report_home_event_daily
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND stat_date = ?
                  AND deleted = 0
                """, HOME_EVENT_ROW_MAPPER, merchantId, storeId, Date.valueOf(statDate)).get(0);
    }

    private OrderDailyRecord findOrderDaily(long merchantId, long storeId, LocalDate statDate) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, stat_date, order_count, paid_order_count, gross_amount, refund_amount
                FROM cdd_report_order_daily
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND stat_date = ?
                  AND deleted = 0
                """, ORDER_DAILY_ROW_MAPPER, merchantId, storeId, Date.valueOf(statDate)).get(0);
    }

    private ProductDailyRecord findProductDaily(long merchantId, long storeId, long productId, LocalDate statDate) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, stat_date, product_id, sku_id, view_count, sale_count, sale_amount
                FROM cdd_report_product_daily
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND product_id = ?
                  AND stat_date = ?
                  AND deleted = 0
                """, PRODUCT_DAILY_ROW_MAPPER, merchantId, storeId, productId, Date.valueOf(statDate)).get(0);
    }

    private void appendDateRange(StringBuilder sql, List<Object> args, LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            sql.append(" AND stat_date >= ?");
            args.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND stat_date <= ?");
            args.add(Date.valueOf(endDate));
        }
    }
}

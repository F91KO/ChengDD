package com.cdd.report.service;

import com.cdd.api.report.model.MerchantDashboardSnapshotResponse;
import com.cdd.api.report.model.MerchantDashboardSnapshotUpsertRequest;
import com.cdd.api.report.model.PlatformDashboardSnapshotResponse;
import com.cdd.api.report.model.PlatformDashboardSnapshotUpsertRequest;
import com.cdd.api.report.model.ReportDataHealthResponse;
import com.cdd.api.report.model.ReportHealthItemResponse;
import com.cdd.api.report.model.ReportHealthResponse;
import com.cdd.api.report.model.ReportHomeEventDailyResponse;
import com.cdd.api.report.model.ReportHomeEventDailyUpsertRequest;
import com.cdd.api.report.model.ReportOrderDailyResponse;
import com.cdd.api.report.model.ReportOrderDailyUpsertRequest;
import com.cdd.api.report.model.ReportProductDailyResponse;
import com.cdd.api.report.model.ReportProductDailyUpsertRequest;
import com.cdd.api.report.model.dashboard.DashboardTodoItemResponse;
import com.cdd.api.report.model.dashboard.MerchantDashboardOverviewResponse;
import com.cdd.api.report.model.dashboard.MerchantDashboardTrendPointResponse;
import com.cdd.api.report.model.dashboard.PlatformDashboardMerchantStatResponse;
import com.cdd.api.report.model.dashboard.PlatformDashboardOverviewResponse;
import com.cdd.common.core.error.BusinessException;
import com.cdd.report.error.ReportErrorCode;
import com.cdd.report.infrastructure.persistence.ReportRepository;
import com.cdd.report.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReportApplicationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReportRepository reportRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public ReportApplicationService(ReportRepository reportRepository,
                                    IdGenerator idGenerator,
                                    ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReportHomeEventDailyResponse upsertHomeEventDaily(ReportHomeEventDailyUpsertRequest request) {
        ReportRepository.HomeEventDailyRecord record = reportRepository.upsertHomeEventDaily(
                idGenerator.nextId(),
                request.merchantId(),
                request.storeId(),
                parseDate(request.statDate()),
                request.miniProgramId(),
                request.pageViewCount(),
                request.visitorCount(),
                request.clickCount());
        return toHomeEventResponse(record);
    }

    public List<ReportHomeEventDailyResponse> listHomeEventDaily(long merchantId, long storeId, String startDate, String endDate) {
        return reportRepository.listHomeEventDaily(merchantId, storeId, parseDateOrNull(startDate), parseDateOrNull(endDate)).stream()
                .map(this::toHomeEventResponse)
                .toList();
    }

    @Transactional
    public ReportOrderDailyResponse upsertOrderDaily(ReportOrderDailyUpsertRequest request) {
        ReportRepository.OrderDailyRecord record = reportRepository.upsertOrderDaily(
                idGenerator.nextId(),
                request.merchantId(),
                request.storeId(),
                parseDate(request.statDate()),
                request.orderCount(),
                request.paidOrderCount(),
                request.grossAmount(),
                request.refundAmount());
        return toOrderResponse(record);
    }

    public List<ReportOrderDailyResponse> listOrderDaily(long merchantId, long storeId, String startDate, String endDate) {
        return reportRepository.listOrderDaily(merchantId, storeId, parseDateOrNull(startDate), parseDateOrNull(endDate)).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional
    public ReportProductDailyResponse upsertProductDaily(ReportProductDailyUpsertRequest request) {
        ReportRepository.ProductDailyRecord record = reportRepository.upsertProductDaily(
                idGenerator.nextId(),
                request.merchantId(),
                request.storeId(),
                parseDate(request.statDate()),
                request.productId(),
                request.skuId(),
                request.viewCount(),
                request.saleCount(),
                request.saleAmount());
        return toProductResponse(record);
    }

    public List<ReportProductDailyResponse> listProductDaily(long merchantId,
                                                             long storeId,
                                                             Long productId,
                                                             String startDate,
                                                             String endDate) {
        return reportRepository.listProductDaily(merchantId, storeId, productId, parseDateOrNull(startDate), parseDateOrNull(endDate)).stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional
    public MerchantDashboardSnapshotResponse createMerchantDashboardSnapshot(MerchantDashboardSnapshotUpsertRequest request) {
        ReportRepository.MerchantDashboardSnapshotRecord record = reportRepository.createMerchantDashboardSnapshot(
                idGenerator.nextId(),
                request.merchantId(),
                request.storeId(),
                parseDateTime(request.snapshotTime()),
                writeJson(request.dashboardPayload()));
        return toMerchantDashboardResponse(record);
    }

    public MerchantDashboardSnapshotResponse getLatestMerchantDashboardSnapshot(long merchantId, long storeId) {
        return reportRepository.findLatestMerchantDashboardSnapshot(merchantId, storeId)
                .map(this::toMerchantDashboardResponse)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_MERCHANT_DASHBOARD_NOT_FOUND));
    }

    public ReportHealthResponse getReportHealth(long merchantId, long storeId) {
        List<ReportHealthItemResponse> items = List.of(
                buildDailyHealthItem(
                        "home_event_daily",
                        "首页事件日报",
                        reportRepository.findLatestHomeEventDaily(merchantId, storeId)
                                .map(record -> formatDate(record.statDate()))
                                .orElse(null)),
                buildDailyHealthItem(
                        "order_daily",
                        "订单日报",
                        reportRepository.findLatestOrderDaily(merchantId, storeId)
                                .map(record -> formatDate(record.statDate()))
                                .orElse(null)),
                buildDailyHealthItem(
                        "product_daily",
                        "商品日报",
                        reportRepository.findLatestProductDaily(merchantId, storeId)
                                .map(record -> formatDate(record.statDate()))
                                .orElse(null)),
                buildSnapshotHealthItem(
                        "merchant_dashboard",
                        "商家看板快照",
                        reportRepository.findLatestMerchantDashboardSnapshot(merchantId, storeId)
                                .map(record -> formatDateTime(record.snapshotTime()))
                                .orElse(null)));
        boolean ready = items.stream().allMatch(item -> "ready".equals(item.status()));
        String summary = ready ? "报表数据已准备完成。" : "报表数据未完全准备，请先补齐缺失数据。";
        return new ReportHealthResponse(merchantId, storeId, ready, summary, items);
    }

    public ReportDataHealthResponse getReportDataHealth() {
        long homeEventDailyCount = reportRepository.countHomeEventDaily();
        long orderDailyCount = reportRepository.countOrderDaily();
        long productDailyCount = reportRepository.countProductDaily();
        long merchantDashboardSnapshotCount = reportRepository.countMerchantDashboardSnapshots();
        long platformDashboardSnapshotCount = reportRepository.countPlatformDashboardSnapshots();

        List<String> issues = Stream.of(
                        homeEventDailyCount == 0 ? "首页事件日报为空" : null,
                        orderDailyCount == 0 ? "订单日报为空" : null,
                        productDailyCount == 0 ? "商品日报为空" : null,
                        merchantDashboardSnapshotCount == 0 ? "商家看板快照为空" : null,
                        platformDashboardSnapshotCount == 0 ? "平台看板快照为空" : null)
                .filter(StringUtils::hasText)
                .toList();
        return new ReportDataHealthResponse(
                issues.isEmpty() ? "healthy" : "degraded",
                formatDateTime(Timestamp.valueOf(LocalDateTime.now())),
                homeEventDailyCount,
                orderDailyCount,
                productDailyCount,
                merchantDashboardSnapshotCount,
                platformDashboardSnapshotCount,
                reportRepository.findLatestHomeEventStatDate().map(this::formatDate).orElse(null),
                reportRepository.findLatestOrderStatDate().map(this::formatDate).orElse(null),
                reportRepository.findLatestProductStatDate().map(this::formatDate).orElse(null),
                reportRepository.findLatestMerchantDashboardSnapshotTime().map(this::formatDateTime).orElse(null),
                reportRepository.findLatestPlatformDashboardSnapshotTime().map(this::formatDateTime).orElse(null),
                issues);
    }

    public MerchantDashboardOverviewResponse getMerchantDashboardOverview(long merchantId, long storeId) {
        ReportRepository.MerchantDashboardSnapshotRecord snapshot = reportRepository.findLatestMerchantDashboardSnapshot(merchantId, storeId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_MERCHANT_DASHBOARD_NOT_FOUND));
        List<ReportRepository.OrderDailyRecord> orderRecords = reportRepository.listOrderDaily(
                merchantId,
                storeId,
                LocalDate.now().minusDays(6),
                LocalDate.now());
        List<ReportRepository.HomeEventDailyRecord> homeEventRecords = reportRepository.listHomeEventDaily(
                merchantId,
                storeId,
                LocalDate.now().minusDays(6),
                LocalDate.now());
        return toMerchantDashboardOverview(snapshot, orderRecords, homeEventRecords);
    }

    @Transactional
    public PlatformDashboardSnapshotResponse createPlatformDashboardSnapshot(PlatformDashboardSnapshotUpsertRequest request) {
        ReportRepository.PlatformDashboardSnapshotRecord record = reportRepository.createPlatformDashboardSnapshot(
                idGenerator.nextId(),
                parseDateTime(request.snapshotTime()),
                writeJson(request.dashboardPayload()));
        return toPlatformDashboardResponse(record);
    }

    public PlatformDashboardSnapshotResponse getLatestPlatformDashboardSnapshot() {
        return reportRepository.findLatestPlatformDashboardSnapshot()
                .map(this::toPlatformDashboardResponse)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_PLATFORM_DASHBOARD_NOT_FOUND));
    }

    public PlatformDashboardOverviewResponse getPlatformDashboardOverview() {
        ReportRepository.PlatformDashboardSnapshotRecord snapshot = reportRepository.findLatestPlatformDashboardSnapshot()
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_PLATFORM_DASHBOARD_NOT_FOUND));
        JsonNode payload = readJson(snapshot.dashboardPayloadJson());
        return new PlatformDashboardOverviewResponse(
                formatDateTime(snapshot.snapshotTime()),
                asLong(payload, "merchant_count"),
                asLong(payload, "daily_active_store_count"),
                asLong(payload, "today_order_count"),
                asDecimal(payload, "today_gmv"));
    }

    public List<PlatformDashboardMerchantStatResponse> listPlatformMerchantStats(Long merchantId, Long storeId) {
        Map<String, ReportRepository.MerchantDashboardSnapshotRecord> latestSnapshots = new LinkedHashMap<>();
        for (ReportRepository.MerchantDashboardSnapshotRecord record : reportRepository.listMerchantDashboardSnapshots(merchantId, storeId)) {
            String key = record.merchantId() + "_" + record.storeId();
            latestSnapshots.putIfAbsent(key, record);
        }
        return latestSnapshots.values().stream()
                .map(this::toPlatformMerchantStatResponse)
                .toList();
    }

    private ReportHealthItemResponse buildDailyHealthItem(String code, String name, String latestDataTime) {
        if (latestDataTime == null) {
            return new ReportHealthItemResponse(code, name, "missing", null, "暂无数据，请先完成日报写入。");
        }
        return new ReportHealthItemResponse(code, name, "ready", latestDataTime, "最近一条日报数据可用。");
    }

    private ReportHealthItemResponse buildSnapshotHealthItem(String code, String name, String latestDataTime) {
        if (latestDataTime == null) {
            return new ReportHealthItemResponse(code, name, "missing", null, "暂无看板快照，请先刷新看板快照。");
        }
        return new ReportHealthItemResponse(code, name, "ready", latestDataTime, "最近一条看板快照可用。");
    }

    private ReportHomeEventDailyResponse toHomeEventResponse(ReportRepository.HomeEventDailyRecord record) {
        return new ReportHomeEventDailyResponse(
                record.id(),
                record.merchantId(),
                record.storeId(),
                formatDate(record.statDate()),
                record.miniProgramId(),
                record.pageViewCount(),
                record.visitorCount(),
                record.clickCount());
    }

    private ReportOrderDailyResponse toOrderResponse(ReportRepository.OrderDailyRecord record) {
        return new ReportOrderDailyResponse(
                record.id(),
                record.merchantId(),
                record.storeId(),
                formatDate(record.statDate()),
                record.orderCount(),
                record.paidOrderCount(),
                record.grossAmount(),
                record.refundAmount());
    }

    private ReportProductDailyResponse toProductResponse(ReportRepository.ProductDailyRecord record) {
        return new ReportProductDailyResponse(
                record.id(),
                record.merchantId(),
                record.storeId(),
                formatDate(record.statDate()),
                record.productId(),
                record.skuId(),
                record.viewCount(),
                record.saleCount(),
                record.saleAmount());
    }

    private MerchantDashboardSnapshotResponse toMerchantDashboardResponse(ReportRepository.MerchantDashboardSnapshotRecord record) {
        return new MerchantDashboardSnapshotResponse(
                record.id(),
                record.merchantId(),
                record.storeId(),
                formatDateTime(record.snapshotTime()),
                readJson(record.dashboardPayloadJson()));
    }

    private PlatformDashboardSnapshotResponse toPlatformDashboardResponse(ReportRepository.PlatformDashboardSnapshotRecord record) {
        return new PlatformDashboardSnapshotResponse(
                record.id(),
                formatDateTime(record.snapshotTime()),
                readJson(record.dashboardPayloadJson()));
    }

    private MerchantDashboardOverviewResponse toMerchantDashboardOverview(ReportRepository.MerchantDashboardSnapshotRecord snapshot,
                                                                         List<ReportRepository.OrderDailyRecord> orderRecords,
                                                                         List<ReportRepository.HomeEventDailyRecord> homeEventRecords) {
        JsonNode payload = readJson(snapshot.dashboardPayloadJson());
        Map<LocalDate, ReportRepository.HomeEventDailyRecord> homeEventRecordMap = new LinkedHashMap<>();
        for (ReportRepository.HomeEventDailyRecord record : homeEventRecords) {
            homeEventRecordMap.put(record.statDate(), record);
        }
        return new MerchantDashboardOverviewResponse(
                snapshot.merchantId(),
                snapshot.storeId(),
                formatDateTime(snapshot.snapshotTime()),
                asDecimal(payload, "gmv"),
                asLong(payload, "order_count"),
                asLong(payload, "paid_order_count"),
                asLong(payload, "visitor_count"),
                asLong(payload, "click_count"),
                asLong(payload, "active_product_count"),
                asLong(payload, "pending_delivery_count"),
                asLong(payload, "after_sale_processing_count"),
                asLong(payload, "release_exception_count"),
                toTodoSummary(payload.path("todo_summary")),
                orderRecords.stream()
                        .sorted((left, right) -> left.statDate().compareTo(right.statDate()))
                        .map(order -> {
                            ReportRepository.HomeEventDailyRecord homeEvent = homeEventRecordMap.get(order.statDate());
                            return new MerchantDashboardTrendPointResponse(
                                    formatDate(order.statDate()),
                                    order.grossAmount(),
                                    order.orderCount(),
                                    homeEvent == null ? 0L : homeEvent.visitorCount(),
                                    homeEvent == null ? 0L : homeEvent.clickCount());
                        })
                        .toList());
    }

    private PlatformDashboardMerchantStatResponse toPlatformMerchantStatResponse(ReportRepository.MerchantDashboardSnapshotRecord snapshot) {
        JsonNode payload = readJson(snapshot.dashboardPayloadJson());
        return new PlatformDashboardMerchantStatResponse(
                snapshot.merchantId(),
                snapshot.storeId(),
                formatDateTime(snapshot.snapshotTime()),
                asDecimal(payload, "gmv"),
                asLong(payload, "order_count"),
                asLong(payload, "visitor_count"),
                asLong(payload, "active_product_count"),
                asLong(payload, "pending_delivery_count"),
                asLong(payload, "after_sale_processing_count"),
                asLong(payload, "release_exception_count"));
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ReportErrorCode.REPORT_STAT_DATE_INVALID);
        }
    }

    private LocalDate parseDateOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseDate(value);
    }

    private Timestamp parseDateTime(String value) {
        try {
            return Timestamp.valueOf(LocalDateTime.parse(value, DATETIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ReportErrorCode.REPORT_SNAPSHOT_TIME_INVALID);
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(Timestamp value) {
        return value == null ? null : DATETIME_FORMATTER.format(value.toLocalDateTime());
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("报表快照序列化失败", ex);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("报表快照反序列化失败", ex);
        }
    }

    private List<DashboardTodoItemResponse> toTodoSummary(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<DashboardTodoItemResponse> items = new ArrayList<>();
        for (JsonNode item : node) {
            items.add(new DashboardTodoItemResponse(
                    item.path("title").asText(""),
                    item.path("detail").asText(""),
                    item.path("tone").asText("default")));
        }
        return items;
    }

    private Long asLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? 0L : value.asLong(0L);
    }

    private BigDecimal asDecimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return BigDecimal.ZERO;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        try {
            return new BigDecimal(value.asText("0"));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}

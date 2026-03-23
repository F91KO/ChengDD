package com.cdd.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cdd.api.report.model.MerchantDashboardSnapshotUpsertRequest;
import com.cdd.api.report.model.PlatformDashboardSnapshotUpsertRequest;
import com.cdd.api.report.model.ReportHomeEventDailyUpsertRequest;
import com.cdd.api.report.model.ReportOrderDailyUpsertRequest;
import com.cdd.api.report.model.ReportProductDailyUpsertRequest;
import com.cdd.report.ReportServiceApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ReportServiceApplication.class)
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportApplicationServiceTest {

    private static final long MERCHANT_ID = 1001L;
    private static final long STORE_ID = 1001L;

    @Autowired
    private ReportApplicationService reportApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldUpsertAndQueryReportDomainData() throws Exception {
        var homeEvent = reportApplicationService.upsertHomeEventDaily(new ReportHomeEventDailyUpsertRequest(
                MERCHANT_ID,
                STORE_ID,
                "2026-03-22",
                1001L,
                128L,
                64L,
                23L));
        assertEquals(128L, homeEvent.pageViewCount());

        var orderDaily = reportApplicationService.upsertOrderDaily(new ReportOrderDailyUpsertRequest(
                MERCHANT_ID,
                STORE_ID,
                "2026-03-22",
                18L,
                15L,
                new BigDecimal("1299.90"),
                new BigDecimal("88.00")));
        assertEquals(15L, orderDaily.paidOrderCount());

        var productDaily = reportApplicationService.upsertProductDaily(new ReportProductDailyUpsertRequest(
                MERCHANT_ID,
                STORE_ID,
                "2026-03-22",
                3100001L,
                3200001L,
                96L,
                12L,
                new BigDecimal("588.80")));
        assertEquals(3100001L, productDaily.productId());

        var merchantDashboard = reportApplicationService.createMerchantDashboardSnapshot(new MerchantDashboardSnapshotUpsertRequest(
                MERCHANT_ID,
                STORE_ID,
                "2026-03-22 10:30:00",
                objectMapper.readTree("{\"gmv\":1299.90,\"order_count\":18}")));
        assertEquals(STORE_ID, merchantDashboard.storeId());

        var platformDashboard = reportApplicationService.createPlatformDashboardSnapshot(new PlatformDashboardSnapshotUpsertRequest(
                "2026-03-22 11:00:00",
                objectMapper.readTree("{\"merchant_count\":12,\"daily_active_store_count\":8}")));
        assertEquals("2026-03-22 11:00:00", platformDashboard.snapshotTime());

        assertEquals(1, reportApplicationService.listHomeEventDaily(MERCHANT_ID, STORE_ID, "2026-03-01", "2026-03-31").size());
        assertEquals(1, reportApplicationService.listOrderDaily(MERCHANT_ID, STORE_ID, null, null).size());
        assertEquals(1, reportApplicationService.listProductDaily(MERCHANT_ID, STORE_ID, 3100001L, null, null).size());
        assertEquals("2026-03-22 10:30:00",
                reportApplicationService.getLatestMerchantDashboardSnapshot(MERCHANT_ID, STORE_ID).snapshotTime());
        assertEquals("2026-03-22 11:00:00",
                reportApplicationService.getLatestPlatformDashboardSnapshot().snapshotTime());
    }
}

package com.cdd.report.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cdd.report.ReportServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ReportServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportControllerIntegrationTest {

    private static final long MERCHANT_ID = 920001L;
    private static final long STORE_ID = 920001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnDegradedHealthWhenNoDataPresent() throws Exception {
        mockMvc.perform(get("/api/report/data-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").exists())
                .andExpect(jsonPath("$.data.issues").isArray());
    }

    @Test
    void shouldReturnHealthyDataHealthAfterReportDataPrepared() throws Exception {
        JsonNode initialHealth = readData(mockMvc.perform(get("/api/report/data-health"))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(post("/api/report/home-events/daily")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", MERCHANT_ID,
                                "store_id", STORE_ID,
                                "stat_date", "2026-03-23",
                                "mini_program_id", 1200001L,
                                "page_view_count", 188,
                                "visitor_count", 66,
                                "click_count", 27))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page_view_count").value(188));

        mockMvc.perform(post("/api/report/orders/daily")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", MERCHANT_ID,
                                "store_id", STORE_ID,
                                "stat_date", "2026-03-23",
                                "order_count", 19,
                                "paid_order_count", 16,
                                "gross_amount", 1688.50,
                                "refund_amount", 66.60))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_count").value(19));

        mockMvc.perform(post("/api/report/products/daily")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", MERCHANT_ID,
                                "store_id", STORE_ID,
                                "stat_date", "2026-03-23",
                                "product_id", 3300001L,
                                "sku_id", 3301001L,
                                "view_count", 98,
                                "sale_count", 13,
                                "sale_amount", 688.20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product_id").value(3300001L));

        mockMvc.perform(post("/api/report/merchant-dashboard/snapshots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", MERCHANT_ID,
                                "store_id", STORE_ID,
                                "snapshot_time", "2026-03-23 12:30:00",
                                "dashboard_payload", Map.of(
                                        "gmv", 1688.50,
                                        "order_count", 19,
                                        "paid_order_count", 16,
                                        "visitor_count", 66,
                                        "click_count", 27,
                                        "active_product_count", 8,
                                        "pending_delivery_count", 3,
                                        "after_sale_processing_count", 1,
                                        "release_exception_count", 0)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.store_id").value(STORE_ID));

        mockMvc.perform(post("/api/report/platform-dashboard/snapshots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "snapshot_time", "2026-03-23 12:35:00",
                                "dashboard_payload", Map.of(
                                        "merchant_count", 12,
                                        "daily_active_store_count", 9,
                                        "today_order_count", 128,
                                        "today_gmv", 8899.90)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot_time").value("2026-03-23 12:35:00"));

        JsonNode latestHealth = readData(mockMvc.perform(get("/api/report/data-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("healthy"))
                .andExpect(jsonPath("$.data.latest_home_event_stat_date").value("2026-03-23"))
                .andExpect(jsonPath("$.data.latest_order_stat_date").value("2026-03-23"))
                .andExpect(jsonPath("$.data.latest_product_stat_date").value("2026-03-23"))
                .andExpect(jsonPath("$.data.latest_merchant_dashboard_snapshot_time").value("2026-03-23 12:30:00"))
                .andExpect(jsonPath("$.data.latest_platform_dashboard_snapshot_time").value("2026-03-23 12:35:00"))
                .andExpect(jsonPath("$.data.issues").isEmpty())
                .andReturn());

        assertEquals(initialHealth.path("home_event_daily_count").asLong() + 1, latestHealth.path("home_event_daily_count").asLong());
        assertEquals(initialHealth.path("order_daily_count").asLong() + 1, latestHealth.path("order_daily_count").asLong());
        assertEquals(initialHealth.path("product_daily_count").asLong() + 1, latestHealth.path("product_daily_count").asLong());
        assertEquals(initialHealth.path("merchant_dashboard_snapshot_count").asLong() + 1,
                latestHealth.path("merchant_dashboard_snapshot_count").asLong());
        assertEquals(initialHealth.path("platform_dashboard_snapshot_count").asLong() + 1,
                latestHealth.path("platform_dashboard_snapshot_count").asLong());
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}

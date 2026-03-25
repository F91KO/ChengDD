package com.cdd.order.web;

import com.cdd.order.OrderServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OrderServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportOrderBaselineLifecycle() throws Exception {
        long merchantId = 3001L;
        long storeId = 4001L;
        long userId = 5001L;

        mockMvc.perform(post("/api/order/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "product_id", 91001L,
                                "sku_id", 92001L,
                                "quantity", 2,
                                "selected", true,
                                "snapshot_price", new BigDecimal("12.50")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.invalid_status").value("valid"));

        MvcResult checkoutResult = mockMvc.perform(post("/api/order/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.item_count").value(1))
                .andExpect(jsonPath("$.data.total_amount").value(25.00))
                .andReturn();

        JsonNode checkoutData = readData(checkoutResult);
        String snapshotToken = checkoutData.path("snapshot_token").asText();
        assertThat(snapshotToken).isNotBlank();

        MvcResult createOrderResult = mockMvc.perform(post("/api/order/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "snapshot_token", snapshotToken,
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "buyer_remark", "尽快发货",
                                "receiver_name", "张三",
                                "receiver_mobile", "13800000000",
                                "receiver_address", "上海市浦东新区"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.order_status").value("pending_pay"))
                .andExpect(jsonPath("$.data.pay_status").value("unpaid"))
                .andReturn();

        JsonNode createOrderData = readData(createOrderResult);
        String orderNo = createOrderData.path("order_no").asText();
        assertThat(orderNo).isNotBlank();

        MvcResult payingResult = mockMvc.perform(post("/api/order/orders/{order_no}/paying", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "pay_channel", "wechat_pay",
                                "pay_method", "jsapi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pay_status").value("paying"))
                .andReturn();

        String payNo = readData(payingResult).path("pay_no").asText();
        assertThat(payNo).isNotBlank();

        mockMvc.perform(post("/api/order/orders/{order_no}/paid", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "pay_no", payNo,
                                "third_party_trade_no", "wx_trade_10001",
                                "paid_amount", new BigDecimal("25.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.order_status").value("paid"))
                .andExpect(jsonPath("$.data.pay_status").value("paid"))
                .andExpect(jsonPath("$.data.delivery_status").value("pending"));

        mockMvc.perform(get("/api/order/orders/{order_no}", orderNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.order_no").value(orderNo))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.status_logs[0].to_status").value("pending_pay"));

        mockMvc.perform(get("/api/order/orders")
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.page_size").value(20))
                .andExpect(jsonPath("$.data.list[0].order_no").value(orderNo));

        mockMvc.perform(post("/api/order/orders/{order_no}/cancel", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "cancel_reason", "测试取消"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40034))
                .andExpect(jsonPath("$.message").value("已支付订单不允许取消"));
    }

    @Test
    void shouldSupportPayCallbackRefundAndCompensationFlow() throws Exception {
        long merchantId = 3002L;
        long storeId = 4002L;
        long userId = 5002L;

        String orderNo = createPaidOrderByCallback(merchantId, storeId, userId, new BigDecimal("36.00"), "callback-success-1");

        MvcResult refundResult = mockMvc.perform(post("/api/order/orders/{order_no}/refunds", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "refund_amount", new BigDecimal("12.00"),
                                "refund_reason", "用户申请退款"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refund_status").value("processing"))
                .andExpect(jsonPath("$.data.compensation_task_code").doesNotExist())
                .andReturn();

        String refundNo = readData(refundResult).path("refund_no").asText();
        assertThat(refundNo).isNotBlank();

        mockMvc.perform(post("/api/order/refunds/{refund_no}/callbacks", refundNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "callback_event_id", "refund-success-1",
                                "callback_status", "success",
                                "third_party_refund_no", "wx_refund_10001",
                                "callback_payload_json", "{\"status\":\"success\"}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refund_status").value("success"))
                .andExpect(jsonPath("$.data.pay_status").value("refund_partial"))
                .andExpect(jsonPath("$.data.duplicated").value(false));
    }

    @Test
    void shouldCreateCompensationTaskWhenRefundCallbackFails() throws Exception {
        long merchantId = 3003L;
        long storeId = 4003L;
        long userId = 5003L;

        String orderNo = createPaidOrderByCallback(merchantId, storeId, userId, new BigDecimal("50.00"), "callback-failed-1");

        MvcResult refundResult = mockMvc.perform(post("/api/order/orders/{order_no}/refunds", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "refund_amount", new BigDecimal("50.00"),
                                "refund_reason", "整单退款"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String refundNo = readData(refundResult).path("refund_no").asText();
        assertThat(refundNo).isNotBlank();

        MvcResult failedCallback = mockMvc.perform(post("/api/order/refunds/{refund_no}/callbacks", refundNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "callback_event_id", "refund-failed-1",
                                "callback_status", "failed",
                                "failure_reason", "渠道超时",
                                "callback_payload_json", "{\"status\":\"failed\"}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refund_status").value("failed"))
                .andExpect(jsonPath("$.data.duplicated").value(false))
                .andExpect(jsonPath("$.data.compensation_task_code").isNotEmpty())
                .andReturn();

        String compensationTaskCode = readData(failedCallback).path("compensation_task_code").asText();
        assertThat(compensationTaskCode).isNotBlank();

        mockMvc.perform(post("/api/order/refunds/{refund_no}/callbacks", refundNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "callback_event_id", "refund-failed-1",
                                "callback_status", "failed",
                                "failure_reason", "渠道超时",
                                "callback_payload_json", "{\"status\":\"failed\"}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refund_status").value("failed"))
                .andExpect(jsonPath("$.data.duplicated").value(true))
                .andExpect(jsonPath("$.data.compensation_task_code").value(compensationTaskCode));
    }

    @Test
    void shouldRestrictCumulativePartialRefundAmount() throws Exception {
        long merchantId = 3004L;
        long storeId = 4004L;
        long userId = 5004L;

        String orderNo = createPaidOrderByCallback(merchantId, storeId, userId, new BigDecimal("36.00"), "callback-partial-1");

        String firstRefundNo = createRefund(orderNo, merchantId, storeId, userId, new BigDecimal("10.00"), "部分退款一");
        completeRefund(firstRefundNo, merchantId, storeId, "refund-partial-success-1", "wx_refund_partial_1")
                .andExpect(jsonPath("$.data.refund_status").value("success"))
                .andExpect(jsonPath("$.data.pay_status").value("refund_partial"));

        String secondRefundNo = createRefund(orderNo, merchantId, storeId, userId, new BigDecimal("20.00"), "部分退款二");
        completeRefund(secondRefundNo, merchantId, storeId, "refund-partial-success-2", "wx_refund_partial_2")
                .andExpect(jsonPath("$.data.refund_status").value("success"))
                .andExpect(jsonPath("$.data.pay_status").value("refund_partial"));

        mockMvc.perform(post("/api/order/orders/{order_no}/refunds", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "refund_amount", new BigDecimal("7.00"),
                                "refund_reason", "超过剩余金额"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40038));

        String thirdRefundNo = createRefund(orderNo, merchantId, storeId, userId, new BigDecimal("6.00"), "补足剩余金额");
        completeRefund(thirdRefundNo, merchantId, storeId, "refund-partial-success-3", "wx_refund_partial_3")
                .andExpect(jsonPath("$.data.refund_status").value("success"))
                .andExpect(jsonPath("$.data.pay_status").value("refund_full"));
    }

    @Test
    void shouldSupportItemLevelRefundOnlyAfterSaleFlow() throws Exception {
        long merchantId = 3010L;
        long storeId = 4010L;
        long userId = 5010L;

        String orderNo = createPaidOrderWithItems(
                merchantId,
                storeId,
                userId,
                "callback-item-refund-only",
                new OrderItemSeed(93101L, 94101L, 2, new BigDecimal("12.00")),
                new OrderItemSeed(93102L, 94102L, 1, new BigDecimal("20.00")));

        JsonNode orderDetail = readData(mockMvc.perform(get("/api/order/orders/{order_no}", orderNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andReturn());
        long firstItemId = orderDetail.path("items").get(0).path("id").asLong();

        MvcResult createAfterSaleResult = mockMvc.perform(post("/api/order/orders/{order_no}/after-sales", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "order_item_id", firstItemId,
                                "after_sale_type", "refund_only",
                                "refund_quantity", 1,
                                "refund_amount", new BigDecimal("12.00"),
                                "reason_code", "damaged",
                                "reason_desc", "商品破损"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("pending_merchant"))
                .andReturn();

        String afterSaleNo = readData(createAfterSaleResult).path("after_sale_no").asText();
        assertThat(afterSaleNo).isNotBlank();

        MvcResult reviewResult = mockMvc.perform(post("/api/order/after-sales/{after_sale_no}/review", afterSaleNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "operator_id", 90001L,
                                "review_action", "agree",
                                "merchant_result", "同意仅退款"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("refunding"))
                .andReturn();

        String refundNo = readData(reviewResult).path("refund_no").asText();
        assertThat(refundNo).isNotBlank();

        completeRefund(refundNo, merchantId, storeId, "after-sale-refund-success-1", "wx_after_sale_refund_1")
                .andExpect(jsonPath("$.data.refund_status").value("success"))
                .andExpect(jsonPath("$.data.pay_status").value("refund_partial"));

        mockMvc.perform(get("/api/order/orders/{order_no}", orderNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("refund_partial"))
                .andExpect(jsonPath("$.data.items[0].refund_status").value("partial_refunded"))
                .andExpect(jsonPath("$.data.items[0].refunded_quantity").value(1))
                .andExpect(jsonPath("$.data.items[0].refunded_amount").value(12.00))
                .andExpect(jsonPath("$.data.items[1].refund_status").value("none"))
                .andExpect(jsonPath("$.data.items[1].refunded_quantity").value(0))
                .andExpect(jsonPath("$.data.items[1].refunded_amount").value(0.00));
    }

    @Test
    void shouldSupportReturnRefundAfterSaleFlow() throws Exception {
        long merchantId = 3011L;
        long storeId = 4011L;
        long userId = 5011L;

        String orderNo = createPaidOrderWithItems(
                merchantId,
                storeId,
                userId,
                "callback-item-return-refund",
                new OrderItemSeed(93201L, 94201L, 2, new BigDecimal("18.00")));

        JsonNode orderDetail = readData(mockMvc.perform(get("/api/order/orders/{order_no}", orderNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andReturn());
        long itemId = orderDetail.path("items").get(0).path("id").asLong();

        MvcResult createAfterSaleResult = mockMvc.perform(post("/api/order/orders/{order_no}/after-sales", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "order_item_id", itemId,
                                "after_sale_type", "return_refund",
                                "refund_quantity", 1,
                                "refund_amount", new BigDecimal("18.00"),
                                "reason_code", "size_wrong",
                                "reason_desc", "尺码不合适"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("pending_merchant"))
                .andReturn();

        String afterSaleNo = readData(createAfterSaleResult).path("after_sale_no").asText();
        assertThat(afterSaleNo).isNotBlank();

        mockMvc.perform(post("/api/order/after-sales/{after_sale_no}/review", afterSaleNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "operator_id", 90002L,
                                "review_action", "agree",
                                "merchant_result", "请寄回商品"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("waiting_return"));

        MvcResult returnResult = mockMvc.perform(post("/api/order/after-sales/{after_sale_no}/return", afterSaleNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "return_company", "SF",
                                "return_logistics_no", "SF10001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("refunding"))
                .andReturn();

        String refundNo = readData(returnResult).path("refund_no").asText();
        assertThat(refundNo).isNotBlank();

        completeRefund(refundNo, merchantId, storeId, "after-sale-return-success-1", "wx_after_sale_return_1")
                .andExpect(jsonPath("$.data.refund_status").value("success"))
                .andExpect(jsonPath("$.data.pay_status").value("refund_partial"));

        mockMvc.perform(get("/api/order/orders/{order_no}", orderNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].refund_status").value("partial_refunded"))
                .andExpect(jsonPath("$.data.items[0].refunded_quantity").value(1))
                .andExpect(jsonPath("$.data.items[0].refunded_amount").value(18.00));
    }

    @Test
    void shouldListAfterSalesAndSupportStatusFilter() throws Exception {
        long merchantId = 3012L;
        long storeId = 4012L;
        long userId = 5012L;

        String orderNo = createPaidOrderWithItems(
                merchantId,
                storeId,
                userId,
                "callback-after-sale-list",
                new OrderItemSeed(93301L, 94301L, 1, new BigDecimal("15.00")),
                new OrderItemSeed(93302L, 94302L, 1, new BigDecimal("28.00")));

        JsonNode orderDetail = readData(mockMvc.perform(get("/api/order/orders/{order_no}", orderNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode firstItem = orderDetail.path("items").get(0);
        JsonNode secondItem = orderDetail.path("items").get(1);
        long firstItemId = firstItem.path("id").asLong();
        long secondItemId = secondItem.path("id").asLong();
        String pendingAfterSaleNo = readData(mockMvc.perform(post("/api/order/orders/{order_no}/after-sales", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "order_item_id", firstItemId,
                                "after_sale_type", "refund_only",
                                "refund_quantity", 1,
                                "refund_amount", new BigDecimal("15.00"),
                                "reason_code", "damaged",
                                "reason_desc", "商品破损"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("pending_merchant"))
                .andReturn()).path("after_sale_no").asText();

        String waitingReturnAfterSaleNo = readData(mockMvc.perform(post("/api/order/orders/{order_no}/after-sales", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "order_item_id", secondItemId,
                                "after_sale_type", "return_refund",
                                "refund_quantity", 1,
                                "refund_amount", new BigDecimal("28.00"),
                                "reason_code", "quality_issue",
                                "reason_desc", "商品质量问题"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("pending_merchant"))
                .andReturn()).path("after_sale_no").asText();

        mockMvc.perform(post("/api/order/after-sales/{after_sale_no}/review", waitingReturnAfterSaleNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "operator_id", 90003L,
                                "review_action", "agree",
                                "merchant_result", "请寄回商品"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("waiting_return"));

        mockMvc.perform(get("/api/order/after-sales")
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].after_sale_no").value(waitingReturnAfterSaleNo))
                .andExpect(jsonPath("$.data.list[0].after_sale_status").value("waiting_return"))
                .andExpect(jsonPath("$.data.list[0].product_name").isNotEmpty())
                .andExpect(jsonPath("$.data.list[0].sku_name").isNotEmpty())
                .andExpect(jsonPath("$.data.list[0].refund_amount").value(28.00))
                .andExpect(jsonPath("$.data.list[1].after_sale_no").value(pendingAfterSaleNo))
                .andExpect(jsonPath("$.data.list[1].after_sale_status").value("pending_merchant"));

        mockMvc.perform(get("/api/order/after-sales")
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("after_sale_status", "waiting_return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].after_sale_no").value(waitingReturnAfterSaleNo))
                .andExpect(jsonPath("$.data.list[0].after_sale_status").value("waiting_return"));
    }

    @Test
    void shouldReturnExtendedOrderSummaryAndExportCsv() throws Exception {
        long merchantId = 3013L;
        long storeId = 4013L;
        long userId = 5013L;

        String orderNo = createPaidOrderWithItems(
                merchantId,
                storeId,
                userId,
                "callback-order-export",
                new OrderItemSeed(93401L, 94401L, 2, new BigDecimal("16.50")),
                new OrderItemSeed(93402L, 94402L, 1, new BigDecimal("8.80")));

        mockMvc.perform(get("/api/order/orders")
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].order_no").value(orderNo))
                .andExpect(jsonPath("$.data.list[0].customer_identifier").value(String.valueOf(userId)))
                .andExpect(jsonPath("$.data.list[0].channel").value("wechat_pay"))
                .andExpect(jsonPath("$.data.list[0].product_summary").value(containsString("商品#93401 / SKU#94401 x2")))
                .andExpect(jsonPath("$.data.list[0].product_summary").value(containsString("商品#93402 / SKU#94402 x1")));

        MvcResult exportResult = mockMvc.perform(get("/api/order/orders/export")
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("orders-export.csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        String csv = exportResult.getResponse().getContentAsString();
        assertThat(csv).contains("订单号,客户标识,渠道,商品摘要");
        assertThat(csv).contains(orderNo);
        assertThat(csv).contains(String.valueOf(userId));
        assertThat(csv).contains("wechat_pay");
        assertThat(csv).contains("商品#93401 / SKU#94401 x2");
        assertThat(csv).contains("商品#93402 / SKU#94402 x1");
    }

    @Test
    void shouldReturnAfterSaleDetailAndLogs() throws Exception {
        long merchantId = 3014L;
        long storeId = 4014L;
        long userId = 5014L;

        String orderNo = createPaidOrderWithItems(
                merchantId,
                storeId,
                userId,
                "callback-after-sale-detail",
                new OrderItemSeed(93501L, 94501L, 1, new BigDecimal("25.00")));

        JsonNode orderDetail = readData(mockMvc.perform(get("/api/order/orders/{order_no}", orderNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId))
                        .param("user_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andReturn());
        long itemId = orderDetail.path("items").get(0).path("id").asLong();

        String afterSaleNo = readData(mockMvc.perform(post("/api/order/orders/{order_no}/after-sales", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "order_item_id", itemId,
                                "after_sale_type", "return_refund",
                                "refund_quantity", 1,
                                "refund_amount", new BigDecimal("25.00"),
                                "reason_code", "quality_issue",
                                "reason_desc", "商品质量问题",
                                "proof_urls", new String[]{"https://img.example.com/a.jpg"}))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("pending_merchant"))
                .andReturn()).path("after_sale_no").asText();

        mockMvc.perform(post("/api/order/after-sales/{after_sale_no}/review", afterSaleNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "operator_id", 90014L,
                                "review_action", "agree",
                                "merchant_result", "请寄回商品"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("waiting_return"));

        String refundNo = readData(mockMvc.perform(post("/api/order/after-sales/{after_sale_no}/return", afterSaleNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "return_company", "SF",
                                "return_logistics_no", "SF3014"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.after_sale_status").value("refunding"))
                .andReturn()).path("refund_no").asText();

        mockMvc.perform(get("/api/order/after-sales/{after_sale_no}", afterSaleNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.after_sale_no").value(afterSaleNo))
                .andExpect(jsonPath("$.data.order_no").value(orderNo))
                .andExpect(jsonPath("$.data.product_name").value("商品#93501"))
                .andExpect(jsonPath("$.data.sku_name").value("SKU#94501"))
                .andExpect(jsonPath("$.data.proof_urls[0]").value("https://img.example.com/a.jpg"))
                .andExpect(jsonPath("$.data.refund_no").value(refundNo))
                .andExpect(jsonPath("$.data.refund_status").value("processing"))
                .andExpect(jsonPath("$.data.pay_status").value("paid"))
                .andExpect(jsonPath("$.data.return_company").value("SF"))
                .andExpect(jsonPath("$.data.return_logistics_no").value("SF3014"));

        mockMvc.perform(get("/api/order/after-sales/{after_sale_no}/logs", afterSaleNo)
                        .param("merchant_id", String.valueOf(merchantId))
                        .param("store_id", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].log_type").value("apply"))
                .andExpect(jsonPath("$.data[1].log_type").value("merchant_review"))
                .andExpect(jsonPath("$.data[2].log_type").value("refund_apply"))
                .andExpect(jsonPath("$.data[3].log_type").value("return_submit"))
                .andExpect(jsonPath("$.data[1].operator_id").value(90014L))
                .andExpect(jsonPath("$.data[1].message").value("请寄回商品"));
    }

    private String createPaidOrderByCallback(long merchantId,
                                             long storeId,
                                             long userId,
                                             BigDecimal amount,
                                             String callbackEventId) throws Exception {
        return createPaidOrderWithItems(
                merchantId,
                storeId,
                userId,
                callbackEventId,
                new OrderItemSeed(93001L, 94001L, 2, amount.divide(new BigDecimal("2.00"))));
    }

    private String createPaidOrderWithItems(long merchantId,
                                            long storeId,
                                            long userId,
                                            String callbackEventId,
                                            OrderItemSeed... items) throws Exception {
        for (OrderItemSeed item : items) {
            mockMvc.perform(post("/api/order/cart/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(writeJson(Map.of(
                                    "merchant_id", merchantId,
                                    "store_id", storeId,
                                    "user_id", userId,
                                    "product_id", item.productId(),
                                    "sku_id", item.skuId(),
                                    "quantity", item.quantity(),
                                    "selected", true,
                                    "snapshot_price", item.snapshotPrice()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        MvcResult checkoutResult = mockMvc.perform(post("/api/order/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String snapshotToken = readData(checkoutResult).path("snapshot_token").asText();
        assertThat(snapshotToken).isNotBlank();

        MvcResult createOrderResult = mockMvc.perform(post("/api/order/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "snapshot_token", snapshotToken,
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "buyer_remark", "用于回调测试",
                                "receiver_name", "李四",
                                "receiver_mobile", "13900000000",
                                "receiver_address", "上海市徐汇区"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String orderNo = readData(createOrderResult).path("order_no").asText();
        assertThat(orderNo).isNotBlank();

        MvcResult payingResult = mockMvc.perform(post("/api/order/orders/{order_no}/paying", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "pay_channel", "wechat_pay",
                                "pay_method", "jsapi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pay_status").value("paying"))
                .andReturn();

        String payNo = readData(payingResult).path("pay_no").asText();
        assertThat(payNo).isNotBlank();

        mockMvc.perform(post("/api/order/pay/callbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "pay_no", payNo,
                                "callback_event_id", callbackEventId,
                                "paid_amount", orderAmount(items),
                                "third_party_trade_no", "wx_trade_" + callbackEventId,
                                "pay_channel", "wechat_pay",
                                "callback_payload_json", "{\"status\":\"success\"}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.order_no").value(orderNo))
                .andExpect(jsonPath("$.data.order_status").value("paid"))
                .andExpect(jsonPath("$.data.pay_status").value("paid"))
                .andExpect(jsonPath("$.data.callback_status").value("processed"))
                .andExpect(jsonPath("$.data.duplicated").value(false));

        return orderNo;
    }

    private BigDecimal orderAmount(OrderItemSeed... items) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemSeed item : items) {
            total = total.add(item.snapshotPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        return total;
    }

    private String createRefund(String orderNo,
                                long merchantId,
                                long storeId,
                                long userId,
                                BigDecimal refundAmount,
                                String refundReason) throws Exception {
        MvcResult refundResult = mockMvc.perform(post("/api/order/orders/{order_no}/refunds", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "refund_amount", refundAmount,
                                "refund_reason", refundReason))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String refundNo = readData(refundResult).path("refund_no").asText();
        assertThat(refundNo).isNotBlank();
        return refundNo;
    }

    private org.springframework.test.web.servlet.ResultActions completeRefund(String refundNo,
                                                                              long merchantId,
                                                                              long storeId,
                                                                              String callbackEventId,
                                                                              String thirdPartyRefundNo) throws Exception {
        return mockMvc.perform(post("/api/order/refunds/{refund_no}/callbacks", refundNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "callback_event_id", callbackEventId,
                                "callback_status", "success",
                                "third_party_refund_no", thirdPartyRefundNo,
                                "callback_payload_json", "{\"status\":\"success\"}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String writeJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private record OrderItemSeed(long productId, long skuId, int quantity, BigDecimal snapshotPrice) {
    }
}

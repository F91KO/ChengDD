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
                .andExpect(jsonPath("$.data[0].order_no").value(orderNo));

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

    private String createPaidOrderByCallback(long merchantId,
                                             long storeId,
                                             long userId,
                                             BigDecimal amount,
                                             String callbackEventId) throws Exception {
        mockMvc.perform(post("/api/order/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "user_id", userId,
                                "product_id", 93001L,
                                "sku_id", 94001L,
                                "quantity", 2,
                                "selected", true,
                                "snapshot_price", amount.divide(new BigDecimal("2.00"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

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
                                "paid_amount", amount,
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
}

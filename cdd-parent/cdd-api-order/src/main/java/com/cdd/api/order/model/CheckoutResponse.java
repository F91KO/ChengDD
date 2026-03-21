package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record CheckoutResponse(
        @JsonProperty("snapshot_token")
        String snapshotToken,
        @JsonProperty("item_count")
        int itemCount,
        @JsonProperty("total_amount")
        BigDecimal totalAmount,
        @JsonProperty("expired_at")
        Instant expiredAt) {
}

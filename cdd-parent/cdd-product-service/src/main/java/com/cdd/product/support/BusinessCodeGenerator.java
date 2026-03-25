package com.cdd.product.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class BusinessCodeGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int SEQUENCE_MASK = 999;

    private final AtomicInteger sequence = new AtomicInteger();

    public String nextProductCode() {
        return "SPU" + nextToken();
    }

    public String nextSkuCode() {
        return "SKU" + nextToken();
    }

    private String nextToken() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int currentSequence = sequence.updateAndGet(value -> (value + 1) % (SEQUENCE_MASK + 1));
        return timestamp + String.format("%03d", currentSequence);
    }
}

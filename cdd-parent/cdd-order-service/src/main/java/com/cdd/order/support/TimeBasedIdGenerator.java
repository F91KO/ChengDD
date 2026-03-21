package com.cdd.order.support;

import java.util.concurrent.atomic.AtomicInteger;

public class TimeBasedIdGenerator implements IdGenerator {

    private static final int SEQUENCE_MASK = (1 << 20) - 1;

    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public long nextId() {
        long millis = System.currentTimeMillis();
        int currentSequence = sequence.updateAndGet(value -> (value + 1) & SEQUENCE_MASK);
        return (millis << 20) | currentSequence;
    }
}

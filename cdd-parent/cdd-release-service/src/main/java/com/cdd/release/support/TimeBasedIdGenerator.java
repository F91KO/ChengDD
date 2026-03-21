package com.cdd.release.support;

import java.util.concurrent.ThreadLocalRandom;

public class TimeBasedIdGenerator implements IdGenerator {

    @Override
    public long nextId() {
        long millis = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return millis * 10_000 + suffix;
    }
}

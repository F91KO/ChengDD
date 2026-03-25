package com.cdd.common.core.page;

import java.util.List;

public record PageResult<T>(
        List<T> list,
        long total) {
}

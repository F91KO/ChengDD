package com.cdd.common.core.page;

public record PageQuery(int page, int pageSize) {

    public PageQuery {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
    }
}

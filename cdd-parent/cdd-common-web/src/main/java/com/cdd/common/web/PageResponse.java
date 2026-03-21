package com.cdd.common.web;

import java.util.List;

public class PageResponse<T> {

    private final List<T> list;
    private final long page;
    private final long pageSize;
    private final long total;

    private PageResponse(List<T> list, long page, long pageSize, long total) {
        this.list = list;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public static <T> PageResponse<T> of(List<T> list, long page, long pageSize, long total) {
        return new PageResponse<>(list, page, pageSize, total);
    }

    public List<T> getList() {
        return list;
    }

    public long getPage() {
        return page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }
}

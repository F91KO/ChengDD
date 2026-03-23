package com.cdd.api.report.model.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DashboardTodoItemResponse(
        @JsonProperty("title")
        String title,
        @JsonProperty("detail")
        String detail,
        @JsonProperty("tone")
        String tone) {
}

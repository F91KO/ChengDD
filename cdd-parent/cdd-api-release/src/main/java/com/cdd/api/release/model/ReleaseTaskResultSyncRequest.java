package com.cdd.api.release.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReleaseTaskResultSyncRequest(
        @JsonProperty("mapping_status")
        String mappingStatus) {
}

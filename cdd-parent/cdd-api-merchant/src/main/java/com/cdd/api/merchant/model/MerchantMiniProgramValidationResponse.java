package com.cdd.api.merchant.model;

import java.util.List;

public record MerchantMiniProgramValidationResponse(
        boolean passed,
        List<String> issues) {
}

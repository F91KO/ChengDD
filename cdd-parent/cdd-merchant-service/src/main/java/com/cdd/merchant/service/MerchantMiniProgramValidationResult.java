package com.cdd.merchant.service;

import java.util.List;

public record MerchantMiniProgramValidationResult(
        boolean passed,
        List<String> issues) {
}

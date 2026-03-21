package com.cdd.merchant.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MerchantMiniProgramValidatorTest {

    private final MerchantMiniProgramValidator validator = new MerchantMiniProgramValidator();

    @Test
    void shouldReturnIssuesWhenPayloadInvalid() {
        MerchantMiniProgramValidationResult result = validator.validate(
                "invalid_appid",
                "short",
                "mch_x",
                "http://example.com");
        assertFalse(result.passed());
        assertTrue(result.issues().size() >= 3);
    }

    @Test
    void shouldPassWhenPayloadValid() {
        MerchantMiniProgramValidationResult result = validator.validate(
                "wx1234567890abcdef",
                "abcdefghijklmnop123456",
                "123456789012",
                "https://example.com");
        assertTrue(result.passed());
        assertTrue(result.issues().isEmpty());
    }
}

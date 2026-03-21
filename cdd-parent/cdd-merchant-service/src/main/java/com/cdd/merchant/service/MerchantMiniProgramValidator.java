package com.cdd.merchant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MerchantMiniProgramValidator {

    private static final Pattern APP_ID_PATTERN = Pattern.compile("^wx[0-9a-zA-Z]{16}$");
    private static final Pattern MCH_ID_PATTERN = Pattern.compile("^[0-9]{6,20}$");

    public MerchantMiniProgramValidationResult validate(String appId,
                                                        String appSecret,
                                                        String paymentMchId,
                                                        String serverDomain) {
        List<String> issues = new ArrayList<>();
        if (!StringUtils.hasText(appId) || !APP_ID_PATTERN.matcher(appId.trim()).matches()) {
            issues.add("小程序AppID格式不正确，应以wx开头且总长度为18位");
        }
        if (!StringUtils.hasText(appSecret) || appSecret.trim().length() < 16) {
            issues.add("小程序AppSecret长度不能小于16位");
        }
        if (StringUtils.hasText(paymentMchId) && !MCH_ID_PATTERN.matcher(paymentMchId.trim()).matches()) {
            issues.add("支付商户号格式不正确，应为6到20位数字");
        }
        if (StringUtils.hasText(serverDomain)) {
            String normalized = serverDomain.trim().toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("https://")) {
                issues.add("服务域名必须使用https协议");
            }
        }
        return new MerchantMiniProgramValidationResult(issues.isEmpty(), List.copyOf(issues));
    }
}

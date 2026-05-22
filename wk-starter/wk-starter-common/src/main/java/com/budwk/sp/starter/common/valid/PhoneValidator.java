package com.budwk.sp.starter.common.valid;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<IsPhone, String> {

    // 简单的中国大陆手机号正则
    private static final String REGEX_PHONE = "^1[3-9]\\d{9}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果值为空，通常交给 @NotBlank 处理，这里直接放行
        if (value == null || value.isEmpty()) {
            return true;
        }
        return Pattern.matches(REGEX_PHONE, value);
    }
}
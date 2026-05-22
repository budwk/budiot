package com.budwk.sp.starter.common.valid;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneValidator.class) // 指定校验逻辑类
@Target({ElementType.FIELD, ElementType.PARAMETER}) // 可以用在字段或参数上
@Retention(RetentionPolicy.RUNTIME)
public @interface IsPhone {

    // 默认错误消息
    String message() default "手机号格式不正确";

    // 分组校验（可选）
    Class<?>[] groups() default {};

    // 负载（可选）
    Class<? extends Payload>[] payload() default {};
}
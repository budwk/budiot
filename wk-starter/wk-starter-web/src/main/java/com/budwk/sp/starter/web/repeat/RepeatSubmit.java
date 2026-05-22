package com.budwk.sp.starter.web.repeat;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {
    /**
     * 锁定时间（默认 5 秒内不允许重复提交）
     */
    int interval() default 5000;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * 提示消息
     */
    String message() default "请勿重复提交，请稍候再试";
}
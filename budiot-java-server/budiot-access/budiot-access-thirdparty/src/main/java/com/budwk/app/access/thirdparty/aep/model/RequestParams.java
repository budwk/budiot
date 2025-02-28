package com.budwk.app.access.thirdparty.aep.model;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface RequestParams {
    /**
     * 属性名
     *
     * @return
     */
    String name() default "";

    /**
     * 是否必填
     *
     * @return
     */
    boolean required() default false;

    Position position() default Position.BODY;

    enum Position {
        /**
         * 请求头
         */
        HEAD,
        /**
         * 表单参数
         */
        QUERY,
        /**
         * 请求体
         */
        BODY
    }
}
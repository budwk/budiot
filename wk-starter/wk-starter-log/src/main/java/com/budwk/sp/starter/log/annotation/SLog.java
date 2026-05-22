package com.budwk.sp.starter.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author wizzer@qq.com
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SLog {

    String tag() default "";

    String type() default "";

    String msg() default "";

    boolean saveParams() default true;

    boolean saveResult() default true;
}

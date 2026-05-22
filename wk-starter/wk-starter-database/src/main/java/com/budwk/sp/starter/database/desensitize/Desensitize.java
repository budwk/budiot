package com.budwk.sp.starter.database.desensitize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.*;

/**
 * 数据脱敏
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside // 允许在该注解上使用其他 Jackson 注解
@JsonSerialize(using = DesensitizerSerializer.class) // 指定自定义序列化器
public @interface Desensitize {
    DesensitizeStrategy strategy();
}
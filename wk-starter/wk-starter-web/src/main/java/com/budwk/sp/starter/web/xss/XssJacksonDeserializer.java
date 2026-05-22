package com.budwk.sp.starter.web.xss;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * Jackson 反序列化器：在读取 JSON 字符串时自动转义
 */
public class XssJacksonDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        return XssUtils.clean(value);
    }
}
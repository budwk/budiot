package com.budwk.sp.starter.database.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

public class DesensitizerSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeStrategy strategy;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 执行脱敏逻辑
        gen.writeString(strategy.getDesensitizer().apply(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        // 获取字段上的注解信息
        Desensitize annotation = property.getAnnotation(Desensitize.class);
        if (annotation != null) {
            this.strategy = annotation.strategy();
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }
}
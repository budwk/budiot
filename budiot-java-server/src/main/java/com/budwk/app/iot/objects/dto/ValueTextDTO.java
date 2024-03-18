package com.budwk.app.iot.objects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nutz.lang.Strings;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValueTextDTO<T> implements Serializable {


    private static final long serialVersionUID = 9165710856851695530L;
    private T value;
    private String text;

    public static ValueTextDTO<?> fromMap(Map<String, Object> map) {
        return new ValueTextDTO<>(map.get("value"), Strings.sBlank(map.get("text")));
    }
}

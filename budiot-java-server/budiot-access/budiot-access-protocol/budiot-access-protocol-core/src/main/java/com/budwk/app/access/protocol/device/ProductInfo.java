package com.budwk.app.access.protocol.device;

import lombok.Data;
import org.nutz.lang.util.NutMap;

import java.io.Serializable;
import java.util.Map;

/**
 * 产品信息
 */
@Data
public class ProductInfo implements Serializable {
    private static final long serialVersionUID = 1L;


    private String id;
    private String name;
    private String classifyId;
    private String protocolType;
    private String iotPlatform;
    private String dataFormat;
    private String deviceType;
    /**
     * 计费方式(1-表端计费，2-平台计费）
     */
    private Integer payMode;
    private Map<String, String> properties;
}

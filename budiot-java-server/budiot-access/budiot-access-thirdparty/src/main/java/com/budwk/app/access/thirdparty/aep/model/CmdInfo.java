package com.budwk.app.access.thirdparty.aep.model;

/**
 * @author zyang  2022/7/21 19:18
 */

import java.io.Serializable;
import java.util.Map;

/**
 * 指令信息
 */
public class CmdInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 设备ID，必填
     */
    private String deviceId;
    /**
     * 操作者，必填
     */
    private String operator;
    /**
     * 产品ID，必填
     */
    private String productId;
    /**
     * 指令在缓存时长默认60秒，选填
     */
    private int ttl = 60;

    /**
     * 内容（分透传和非透传）
     * 透传:
     * {
     * payload:指令内容,数据格式为十六进制时需要填十六进制字符串,
     * dataType:数据类型：1字符串，2十六进制
     * }
     * 非透传：
     * {
     * params：指令参数（一个json对象，参数名：参数值）
     * serviceIdentifier：服务定义时的服务标识
     * }
     */
    private Map content;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public Map getContent() {
        return content;
    }

    public void setContent(Map content) {
        this.content = content;
    }


}

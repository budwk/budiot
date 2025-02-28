package com.budwk.app.access.thirdparty.aep.model;

/**
 * @author zyang  2022/7/21 19:19
 */

import org.nutz.lang.util.NutMap;

import java.io.Serializable;

/**
 * 请求报文体
 */
public class CmdBody implements Serializable {
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
     * 指令在缓存时长默认7200秒，选填
     */
    private int ttl = 7200;
    /**
     * 指令内容,必填，格式为Json,参数如下：
     * {
     * serviceId:命令对应的服务ID,
     * method:命令服务下具体的命令名称
     * paras:指令参数，格式为json,
     * }
     */
    private NutMap command;

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

    public NutMap getCommand() {
        return command;
    }

    public void setCommand(NutMap command) {
        this.command = command;
    }
}

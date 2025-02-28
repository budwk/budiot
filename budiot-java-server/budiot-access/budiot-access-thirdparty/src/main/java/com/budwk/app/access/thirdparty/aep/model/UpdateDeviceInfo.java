package com.budwk.app.access.thirdparty.aep.model;

import org.nutz.lang.util.NutMap;

import java.io.Serializable;

public class UpdateDeviceInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public UpdateDeviceInfo() {
        /**
         * 必须设置自动订阅参数，否则会提示参数验证失败，默认为 自动订阅
         */
        //this.setAutoObserver(true);
    }

    /**
     * 设备名称，必填
     */
    private String deviceName;
    /**
     * 操作者，必填
     */
    private String operator;
    /**
     * 产品ID，必填
     */
    private String productId;

    /**
     * other: 选填，LWM2M协议选填参数,其他协议不填：
     * {
     * autoObserver:0.自动订阅 1.取消自动订阅，选填;
     * imsi:imsi号,选填;
     * pskValue:由大小写字母加0-9数字组成的32位字符串,选填
     * }
     */
    private transient boolean autoObserver = true;

    private NutMap other = NutMap.NEW();

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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

    public NutMap getOther() {
        return other;
    }


    public boolean isAutoObserver() {
        return this.other.getInt("autoObserver") == 0;
    }

    public void setAutoObserver(boolean autoObserver) {
        this.other.put("autoObserver", autoObserver ? 0 : 1);
    }
}

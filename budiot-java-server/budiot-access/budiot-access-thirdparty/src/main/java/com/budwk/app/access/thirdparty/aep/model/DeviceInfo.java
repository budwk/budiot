package com.budwk.app.access.thirdparty.aep.model;

import org.nutz.lang.util.NutMap;

import java.io.Serializable;

/**
 * @author zyang  2022/7/21 19:17
 */

public class DeviceInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public DeviceInfo() {
        /**
         * 必须设置自动订阅参数，否则会提示参数验证失败，默认为 自动订阅
         */
        this.setAutoObserver(true);
    }

    /**
     * 设备名称，必填
     */
    private String deviceName;
    /**
     * 设备编号，MQTT,T_Link协议必填
     */
    private String deviceSn;
    /**
     * imei号，LWM2M,NB网关必填
     */
    private String imei;
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
    private transient String imsi;
    private transient String pskValue;

    private NutMap other = NutMap.NEW();

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceSn() {
        return deviceSn;
    }

    public void setDeviceSn(String deviceSn) {
        this.deviceSn = deviceSn;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
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

    public String getImsi() {
        return this.other.getString("imsi");
    }

    public void setImsi(String imsi) {
        this.other.put("imsi", imsi);
    }

    public String getPskValue() {
        return this.other.getString("pskValue");
    }

    public void setPskValue(String pskValue) {
        this.other.put("pskValue", pskValue);
    }
}
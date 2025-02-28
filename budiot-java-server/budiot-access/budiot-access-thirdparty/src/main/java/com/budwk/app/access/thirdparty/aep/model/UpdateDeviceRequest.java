package com.budwk.app.access.thirdparty.aep.model;

import org.nutz.http.Request;

public class UpdateDeviceRequest extends BaseRequest {

    private String masterKey;
    private String deviceId;
    private UpdateDeviceInfo deviceInfo;

    public UpdateDeviceRequest(String appKey, String appSecret) {
        super(appKey, appSecret);
    }

    public UpdateDeviceRequest(String baseUrl, String appKey, String appSecret) {
        super(baseUrl, appKey, appSecret);
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getMasterKey() {
        return header.getString("MasterKey");
    }

    public void setMasterKey(String masterKey) {
        header.put("MasterKey", masterKey);
    }

    public UpdateDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(UpdateDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public UpdateDeviceInfo createDevice() {
        return new UpdateDeviceInfo();
    }

    @Override
    public Request.METHOD getMethod() {
        return Request.METHOD.PUT;
    }

    @Override
    public String getPath() {
        return String.format("/aep_device_management/device?deviceId=%s", getDeviceId());
    }

}

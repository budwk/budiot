package com.budwk.app.access.thirdparty.aep.model;

import org.nutz.http.Request;

public class CreateDeviceRequest extends BaseRequest {

    private String masterKey;
    private DeviceInfo deviceInfo;

    public CreateDeviceRequest(String baseUrl, String appKey, String appSecret) {
        super(baseUrl, appKey, appSecret);
    }

    public CreateDeviceRequest(String appKey, String appSecret) {
        super(appKey, appSecret);
    }

    public String getMasterKey() {
        return header.getString("MasterKey");
    }

    public void setMasterKey(String masterKey) {
        header.put("MasterKey", masterKey);
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public DeviceInfo createDevice() {
        return new DeviceInfo();
    }

    @Override
    public Request.METHOD getMethod() {
        return Request.METHOD.POST;
    }

    @Override
    public String getPath() {
        return "/aep_device_management/device";
    }

}

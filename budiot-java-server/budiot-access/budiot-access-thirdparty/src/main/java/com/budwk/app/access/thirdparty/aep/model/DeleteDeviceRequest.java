package com.budwk.app.access.thirdparty.aep.model;

import org.nutz.http.Request;
import org.nutz.lang.Strings;

public class DeleteDeviceRequest extends BaseRequest {

    private String productId;
    private String deviceIds;

    public DeleteDeviceRequest(String baseUrl, String appKey, String appSecret) {
        super(baseUrl, appKey, appSecret);
    }

    public DeleteDeviceRequest(String appKey, String appSecret) {
        super(appKey, appSecret);
    }

    public String[] getDeviceIds() {
        return Strings.splitIgnoreBlank(this.deviceIds);
    }

    public void setDeviceIds(String[] deviceIds) {
        this.deviceIds = Strings.join(",", deviceIds);
    }

    public void setDeviceId(String deviceId) {
        this.deviceIds = deviceId;
    }

    public String getMasterKey() {
        return header.getString("MasterKey");
    }

    public void setMasterKey(String masterKey) {
        header.put("MasterKey", masterKey);
    }

    public String getProductId() {
        return this.productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    @Override
    public String getPath() {
        return String.format("/aep_device_management/device?productId=%s&deviceIds=%s", this.productId, this.deviceIds);
    }

    @Override
    public Request.METHOD getMethod() {
        return Request.METHOD.DELETE;
    }
}

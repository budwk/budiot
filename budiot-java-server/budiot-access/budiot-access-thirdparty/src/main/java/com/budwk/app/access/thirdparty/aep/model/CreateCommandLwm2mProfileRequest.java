package com.budwk.app.access.thirdparty.aep.model;

import org.nutz.http.Request;

/**
 * lwm2m协议有profile指令下发请求
 */
public class CreateCommandLwm2mProfileRequest extends BaseRequest {

    /**
     * 报文请求体
     */
    private CmdBody cmdBody;

    public CreateCommandLwm2mProfileRequest(String appKey, String appSecret) {
        super(appKey, appSecret);
    }

    public CreateCommandLwm2mProfileRequest(String baseUrl, String appKey, String appSecret) {
        super(baseUrl, appKey, appSecret);
    }

    /**
     * MasterKey 必填
     *
     * @param masterKey
     */
    public void setMasterKey(String masterKey) {
        header.put("MasterKey", masterKey);
    }

    public String getMasterKey() {
        return header.getString("MasterKey");
    }

    @Override
    public Request.METHOD getMethod() {
        return Request.METHOD.POST;
    }

    @Override
    public String getPath() {
        return "/aep_device_command_lwm_profile/commandLwm2mProfile";
    }

    public CmdBody createCmdBody() {
        return new CmdBody();
    }

    public CmdBody getCmdBody() {
        return cmdBody;
    }

    public void setCmdBody(CmdBody cmdBody) {
        this.cmdBody = cmdBody;
    }

}

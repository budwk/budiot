package com.budwk.app.access.protocol.demo;

import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.thirdparty.aep.ApiCtwingLwm2mServiceClient;
import com.budwk.app.access.thirdparty.aep.model.*;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import java.util.HashMap;
import java.util.Map;

public class AepPlatformHelper {
    private static final Log log = Logs.get();
    private final ApiCtwingLwm2mServiceClient apiCtwingLwm2mServiceClient;

    private final AepApiConfig apiConfig;

    public AepPlatformHelper(Map<String, String> configs) {
        this.apiConfig = newApiConfig(configs);
        this.apiCtwingLwm2mServiceClient = getApiCtwingLwm2mServiceClient();
    }

    /**
     * 注册设备
     */
    public String registerDevice(DeviceOperator deviceOperator) {
        CreateDeviceRequest request = new CreateDeviceRequest(apiConfig.apiBaseUrl, apiConfig.appKey,
                apiConfig.appSecret);
        request.setMasterKey(apiConfig.masterKey);
        DeviceInfo apiDeviceInfo = request.createDevice();
        apiDeviceInfo.setProductId(apiConfig.productId);
        apiDeviceInfo.setAutoObserver(true);
        apiDeviceInfo.setDeviceName(Strings.sBlank(deviceOperator.getProperty("deviceNo"), deviceOperator.getDeviceId()));
        apiDeviceInfo.setDeviceSn(deviceOperator.getDeviceId());
        apiDeviceInfo.setImei((String) deviceOperator.getProperty("imei"));
        apiDeviceInfo.setOperator(apiConfig.appId);
        request.setDeviceInfo(apiDeviceInfo);
        ApiResult apiResult = apiCtwingLwm2mServiceClient.createDevice(request);
        if (apiResult.getCode() == 0) {
            NutMap result = Lang.obj2nutmap(apiResult.getData());
            return result.getString("deviceId");
        } else {
            log.warnf("AEP 设备 %s 注册失败，错误信息：%s", deviceOperator.getDeviceId(), apiResult.getMsg());
        }
        return null;
    }

    /**
     * 删除设备
     *
     * @param deviceId AEP平台设备ID=IotDeviceId
     * @return
     */
    public void deleteDevice(String deviceId) {
        DeleteDeviceRequest request = new DeleteDeviceRequest(apiConfig.apiBaseUrl, apiConfig.appKey,
                apiConfig.appSecret);
        request.setMasterKey(apiConfig.masterKey);
        request.setDeviceId(deviceId);
        request.setProductId(apiConfig.productId);
        ApiResult apiResult = apiCtwingLwm2mServiceClient.deleteDevice(request);
        log.debugf("AEP 删除设备结果: %s", apiResult.toString());
    }


    /**
     * 创建指令(不带profile)
     *
     * @param iotDeviceId 设备对应平台的id
     * @param payload     指令内容
     * @return 返回指令id
     */
    public String createCommand(String iotDeviceId, Object payload) {
        CreateCommandRequest request = new CreateCommandRequest(apiConfig.apiBaseUrl, apiConfig.appKey,
                apiConfig.appSecret);
        request.setMasterKey(apiConfig.masterKey);
        CmdInfo cmdInfo = request.createCmdInfo();
        cmdInfo.setProductId(apiConfig.productId);
        cmdInfo.setDeviceId(iotDeviceId);
        cmdInfo.setOperator(Strings.sBlank(apiConfig.appId, "API"));
        Map<String, Object> content = new HashMap<>();
        content.put("dataType", 2);
        content.put("payload", payload);
        cmdInfo.setContent(content);
        request.setCmdInfo(cmdInfo);
        ApiResult apiResult = apiCtwingLwm2mServiceClient.createCommand(request);
        log.debugf("AEP 平台指令下发结果: %s", apiResult.toString());
        if (apiResult.getCode() == 0) {
            NutMap data = Lang.obj2nutmap(apiResult.getData());
            return String.format("%s_%s", data.getString("deviceId"), data.getString("commandId"));
        } else {
            return null;
        }
    }

    /**
     * 创建指令(带profile)
     *
     * @param iotDeviceId
     * @param command
     * @return
     */
    public String createCommandProfile(String iotDeviceId, Object command) {
        CreateCommandLwm2mProfileRequest request = new CreateCommandLwm2mProfileRequest(apiConfig.apiBaseUrl, apiConfig.appKey,
                apiConfig.appSecret);
        request.setMasterKey(apiConfig.masterKey);
        CmdBody cmdBody = request.createCmdBody();
        cmdBody.setProductId(apiConfig.productId);
        cmdBody.setDeviceId(iotDeviceId);
        cmdBody.setOperator(Strings.sBlank(apiConfig.appId, "API"));
        NutMap map = NutMap.NEW();
        map.addv("serviceId", "BaseParam");
        map.addv("method", "SEND_DEVICE_CMD");
        /**
         * {"paraName":"data","dataType":"string","required":true,"min":"1","max":"2147483646","step":0,"maxLength":512,"unit":"null","enumList":null}
         */
        map.addv("paras", NutMap.NEW().addv("data", command));
        cmdBody.setCommand(map);
        cmdBody.setTtl(60);
        request.setCmdBody(cmdBody);
        ApiResult apiResult = apiCtwingLwm2mServiceClient.createCommandLwm2mProfile(request);
        log.debugf("AEP 平台指令下发结果: %s", apiResult.toString());
        if (apiResult.getCode() == 0) {
            NutMap data = Lang.obj2nutmap(apiResult.getData());
            return String.format("%s_%s", data.getString("deviceId"), data.getString("commandId"));
        } else {
            return null;
        }
    }

    private ApiCtwingLwm2mServiceClient getApiCtwingLwm2mServiceClient() {
        return new ApiCtwingLwm2mServiceClient();
    }

    public AepApiConfig newApiConfig(Map<String, String> conf) {
        return new AepApiConfig(conf);
    }

    static class AepApiConfig {
        public AepApiConfig() {
        }
        public AepApiConfig(Map<String, String> conf) {
            this.apiBaseUrl = conf.get("url");
            this.appId = conf.get("appId");
            this.appKey = conf.get("appKey");
            this.appSecret = conf.get("appSecret");
            this.masterKey = conf.get("masterKey");
            this.productId = conf.get("productId");
            this.hasProfile = Boolean.parseBoolean(conf.get("hasProfile"));
        }

        /**
         * api 相关配置信息
         */
        String apiBaseUrl;
        String appId;
        String appKey;
        String appSecret;
        /**
         * 对应的电信平台产品型号
         */
        String masterKey;
        String productId;
        boolean hasProfile;
    }
}
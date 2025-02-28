package com.budwk.app.access.thirdparty.aep;

import com.budwk.app.access.thirdparty.aep.model.*;
import com.budwk.app.access.thirdparty.aep.utils.AepHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.util.Arrays;

/**
 * Ctwing Lwm2m 协议接口
 */
@Slf4j
public class ApiCtwingLwm2mServiceClient {


    // =============================================================
    // 设备相关


    /**
     * 创建设备 <br />
     * 成功返回设备信息
     * <pre>
     *     {
     *       "tupIsProfile": 1,
     *       "deviceId": "d78ee99d39454b3dbe80e31ee4c34ae3",// 设备id
     *       "deviceName": "TEST_001", // 设备名
     *       "deviceStatus": 0, // 设备状态
     *       "autoObserver": 0, // 自动订阅
     *       "productId": 10002118, // 产品id
     *       "createBy": "riic5q4kf0idgrlrk84ofugfru",
     *       "tenantId": "10090247", // 租户id
     *       "imei": "868000000000001"
     *    }
     * </pre>
     *
     * @param request 设备请求信息
     * @return
     */
    public ApiResult createDevice(CreateDeviceRequest request) {
        try {
            if (Lang.isEmpty(request.getDeviceInfo())) {
                return new ApiResult(-1, "设备信息不能为空", null);
            }
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            String jsonStr = Json.toJson(Lang.obj2map(request.getDeviceInfo()), JsonFormat.tidy());
            if (jsonStr != null) {
                request.setBody(jsonStr.getBytes());
            }
            NutMap signInput = NutMap.NEW();
            signInput.put("MasterKey", request.getMasterKey());
            String signature = AepHttpUtil.sign(request.getAppSecret(), request.getHeader(), signInput, request.getBody());
            // 指定本次接口版本
            request.setHeader("version", "20181031202117");
            request.setHeader("signature", signature);
            return AepHttpUtil.doRequest(request);
        } catch (Exception e) {
            log.error("接口调用失败", e);
            return new ApiResult(-1, e.getMessage(), null);
        }
    }


    /**
     * 删除设备
     *
     * @param request
     * @return
     */
    public ApiResult deleteDevice(DeleteDeviceRequest request) {
        try {
            NutMap signInput = NutMap.NEW();
            signInput.put("MasterKey", request.getMasterKey());
            signInput.put("productId", request.getProductId());
            signInput.putAll(request.getParams());
            signInput.put("deviceIds", Arrays.asList(request.getDeviceIds()));
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            String signature = AepHttpUtil.sign(request.getAppSecret(), request.getHeader(), signInput, request.getBody());
            // 指定本次接口版本
            request.setHeader("version", "20181031202131");
            request.setHeader("signature", signature);
            return AepHttpUtil.doRequest(request);
        } catch (Exception e) {
            log.error("接口调用失败", e);
            return new ApiResult(-1, e.getMessage(), null);
        }
    }


    /**
     * 更新设备信息
     *
     * @param request
     * @return
     */
    public ApiResult updateDevice(UpdateDeviceRequest request) {
        try {
            if (Strings.isBlank(request.getMasterKey()) || Strings.isBlank(request.getDeviceId())) {
                return new ApiResult(-1, "缺少必要参数", null);
            }
            if (Lang.isEmpty(request.getDeviceInfo())) {
                return new ApiResult(-1, "设备信息不能为空", null);
            }
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            String jsonStr = Json.toJson(Lang.obj2map(request.getDeviceInfo()), JsonFormat.tidy());
            if (jsonStr != null) {
                request.setBody(jsonStr.getBytes());
            }
            NutMap signInput = NutMap.NEW();
            signInput.put("MasterKey", request.getMasterKey());
            signInput.put("deviceId", request.getDeviceId());
            String signature = AepHttpUtil.sign(request.getAppSecret(), request.getHeader(), signInput, request.getBody());
            // 指定本次接口版本
            request.setHeader("version", "20191231141545");
            request.setHeader("signature", signature);
            return AepHttpUtil.doRequest(request);
        } catch (Exception e) {
            log.error("接口调用失败", e);
            return new ApiResult(-1, e.getMessage(), null);
        }
    }

    // ==============================================
    // 设备指令相关


    /**
     * {
     * "code": 0,
     * "msg": "ok",
     * "data": {
     * "finishTime": null,
     * "productId": 10002145,
     * "level": 0,
     * "resultPayload": null,
     * "dataType": null,
     * "resultCode": null,
     * "serviceName": "控阀",
     * "type": 6,
     * "deviceId": "b02110e915244e5c865634fd547651a4",
     * "deviceSn": null,
     * "ttl": 7200, // 过期时间
     * "productProtocol": 3,
     * "content": "{\"isTr\":1,\"datasetId\":\"8001\",\"value\":{\"ValveStatus\":2},\"taskId\":7}",
     * "deviceTaskId": 7, // 指令id，单一设备唯一
     * "operator": "s8hns55jnkgbroq7l0bnktaaam",
     * "controlType": null,
     * "payloadType": 0,
     * "createTime": 1548061417829,
     * "tenantId": "10090247",
     * "imei": "860000000000002",
     * "isTr": null,
     * "serviceId": "8001",
     * "taskId": 2074545,
     * "status": 1
     * }
     * }
     *
     * @param request
     * @return
     */
    public ApiResult createCommand(CreateCommandRequest request) {
        try {
            if (Strings.isBlank(request.getMasterKey())) {
                return new ApiResult(-1, "缺少必要参数", null);
            }
            if (Lang.isEmpty(request.getCmdInfo())) {
                return new ApiResult(-1, "指令信息不能为空", null);
            }
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            String jsonStr = Json.toJson(Lang.obj2map(request.getCmdInfo()), JsonFormat.tidy());
            request.setBody(jsonStr.getBytes());
            NutMap signInput = NutMap.NEW();
            signInput.put("MasterKey", request.getMasterKey());
            String signature = AepHttpUtil.sign(request.getAppSecret(), request.getHeader(), signInput, request.getBody());
            // 指定本次接口版本
            request.setHeader("version", "20190712225145");
            request.setHeader("signature", signature);
            return AepHttpUtil.doRequest(request);
        } catch (Exception e) {
            log.error("接口调用失败", e);
            return new ApiResult(-1, e.getMessage(), null);
        }
    }

    /**
     * lwm2m有profile指令下发
     *
     * @param request
     * @return
     */
    public ApiResult createCommandLwm2mProfile(CreateCommandLwm2mProfileRequest request) {
        try {
            if (Strings.isBlank(request.getMasterKey())) {
                return new ApiResult(-1, "缺少必要参数", null);
            }
            if (Lang.isEmpty(request.getCmdBody())) {
                return new ApiResult(-1, "指令信息不能为空", null);
            }
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            String jsonStr = Json.toJson(Lang.obj2map(request.getCmdBody()), JsonFormat.tidy());
            log.debug("jsonStr:{}", jsonStr);
            request.setBody(jsonStr.getBytes());
            NutMap signInput = NutMap.NEW();
            signInput.put("MasterKey", request.getMasterKey());
            String signature = AepHttpUtil.sign(request.getAppSecret(), request.getHeader(), signInput, request.getBody());
            // 指定本次接口版本
            request.setHeader("version", "20191231141545");
            request.setHeader("signature", signature);
            return AepHttpUtil.doRequest(request);
        } catch (Exception e) {
            log.error("接口调用失败", e);
            return new ApiResult(-1, e.getMessage(), null);
        }
    }

}

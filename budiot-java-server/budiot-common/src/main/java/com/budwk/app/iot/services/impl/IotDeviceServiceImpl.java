package com.budwk.app.iot.services.impl;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.iot.caches.DeviceCacheStore;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_device_prop;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.models.Iot_protocol;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import com.budwk.app.iot.services.*;
import com.budwk.app.sys.enums.SysMsgType;
import com.budwk.app.sys.services.SysMsgService;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.database.service.BaseServiceImpl;
import com.budwk.starter.security.utils.SecurityUtil;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@IocBean(args = {"refer:dao"})
public class IotDeviceServiceImpl extends BaseServiceImpl<Iot_device> implements IotDeviceService {
    public IotDeviceServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private SysMsgService sysMsgService;
    @Inject
    private IotProductService iotProductService;
    @Inject
    private IotProtocolService iotProtocolService;
    @Inject
    private IotDeviceLogService iotDeviceLogService;
    @Inject
    private IotDevicePropService iotDevicePropService;
    @Inject
    private MessageTransfer messageTransfer;
    @Inject
    private DeviceCacheStore deviceCacheStore;

    public void importData(String productId, String fileName, List<Iot_device> list, boolean over, String userId, String loginname) {
        if (list == null || list.size() == 0) {
            throw new BaseException("导入数据不能为空！");
        }
        Iot_product product = iotProductService.fetch(productId);
        if (product == null) {
            throw new BaseException("产品不存在");
        }
        Iot_protocol protocol = iotProtocolService.fetch(product.getProtocolId());
        if (protocol == null) {
            throw new BaseException("设备协议不存在");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder resultMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Iot_device device : list) {
            device.setDeviceType(product.getDeviceType());
            device.setClassifyId(product.getClassifyId());
            device.setProductId(productId);
            device.setProtocolId(product.getProtocolId());
            device.setProtocolCode(protocol.getCode());
            device.setAbnormal(false);
            device.setOnline(false);
            device.setCreatedBy(userId);
            device.setUpdatedBy(userId);
            try {
                this.fastInsert(device);
                messageTransfer.publish(new Message<>(TopicConstant.DEVICE_EVENT,
                        NutMap.NEW()
                                .setv("event", "device-register")
                                .setv("productId", device.getProductId())
                                .setv("iotPlatformId", device.getIotPlatformId())
                                .setv("protocolCode", device.getProtocolCode())));
                iotDeviceLogService.log("新增设备", device.getId(), userId, loginname);
                successNum++;
            } catch (Exception e) {
                String duplicate = "";
                if (e.getMessage().contains("Duplicate")) {
                    duplicate = "已存在";
                } else {
                    duplicate = e.getMessage();
                }
                failureNum++;
                String msg = "<br/>" + failureNum + "、设备通信号：" + device.getDeviceNo() + " " + duplicate + "<br/>";
                failureMsg.append(msg);
            }
        }
        resultMsg.insert(0, "导入结果：共成功 " + successNum + " 条");
        if (failureNum > 0) {
            resultMsg.append("，失败 " + failureNum + " 条，失败数据如下：<br/>");
            resultMsg.append(failureMsg);
        }
        sysMsgService.sendMsg(userId, SysMsgType.USER, "设备信息表 " + fileName + " 导入完成", "", resultMsg.toString(), userId);

    }

    public void create(Iot_device device, Map<String, String> props) {
        Iot_product product = iotProductService.fetch(device.getProductId());
        if (product == null) {
            throw new BaseException("产品不存在");
        }
        Iot_protocol protocol = iotProtocolService.fetch(product.getProtocolId());
        if (protocol == null) {
            throw new BaseException("设备协议不存在");
        }
        device.setDeviceType(product.getDeviceType());
        device.setClassifyId(product.getClassifyId());
        device.setProtocolId(product.getProtocolId());
        device.setProtocolCode(protocol.getCode());
        device.setCreatedBy(SecurityUtil.getUserId());
        device.setUpdatedBy(SecurityUtil.getUserId());
        this.insert(device);
        Iot_device_prop prop = new Iot_device_prop();
        prop.setDeviceId(device.getId());
        prop.setProperties(props);
        iotDevicePropService.insert(prop);
        messageTransfer.publish(new Message<>(TopicConstant.DEVICE_EVENT,
                NutMap.NEW()
                        .setv("event", "device-register")
                        .setv("productId", device.getProductId())
                        .setv("iotPlatformId", device.getIotPlatformId())
                        .setv("protocolCode", device.getProtocolCode())));
        iotDeviceLogService.log("新增设备", device.getId(), SecurityUtil.getUserId(), SecurityUtil.getUserLoginname());
    }

    public void delete(Iot_device device) {
        if (device == null) {
            throw new BaseException("设备不存在");
        }
        this.delete(device.getId());
        iotDevicePropService.clear(Cnd.where("deviceId", "=", device.getId()));
        messageTransfer.publish(new Message<>(TopicConstant.DEVICE_EVENT,
                NutMap.NEW()
                        .setv("event", "device-unregister")
                        .setv("productId", device.getProductId())
                        .setv("iotPlatformId", device.getIotPlatformId())
                        .setv("protocolCode", device.getProtocolCode())));
        iotDeviceLogService.log("删除设备", device.getId(), SecurityUtil.getUserId(), SecurityUtil.getUserLoginname());
    }

    public void saveExtraProperties(String deviceId, Map<String, Object> properties) {
        String sqlStr = "UPDATE iot_device_prop SET properties = JSON_SET(properties,$fields) WHERE deviceId=@deviceId";
        Sql sql = Sqls.create(sqlStr);
        // 移除时序字段
        properties.remove("ts");
        List<String> fields = new ArrayList<>();
        properties.forEach((k, v) -> {
            fields.add("'$." + k + "'");
            if (v == null) {
                fields.add("NULL");
            } else if (v instanceof Number) {
                fields.add(Strings.safeToString(v, "NULL"));
            } else {
                fields.add("'" + Strings.sBlank(v) + "'");
            }
        });

        sql.setParam("deviceId", deviceId)
                .setVar("fields", String.join(",", fields.toArray(new String[0])));
        this.execute(sql);
    }

    public DeviceProcessCache getCache(String deviceId) {
        DeviceProcessCache device = deviceCacheStore.getDevice(deviceId);
        if (device == null || (device != null && Strings.isBlank(device.getProtocolCode()))) {
            Iot_device deviceInfo = this.fetch(deviceId);
            if (deviceInfo == null)
                return null;
            Iot_product product = iotProductService.fetch(Cnd.where("id", "=", deviceInfo.getProductId()));
            if (product == null)
                return null;
            deviceCacheStore.cache(deviceInfo, product);
            device = deviceCacheStore.getDevice(deviceId);
        }
        return device;
    }

    public DeviceProcessCache doRefreshCache(String deviceId) {
        Iot_device deviceInfo = this.fetch(deviceId);
        if (deviceInfo == null)
            return null;
        Iot_product product = iotProductService.fetch(Cnd.where("id", "=", deviceInfo.getProductId()));
        if (product == null)
            return null;
        deviceCacheStore.cache(deviceInfo, product);
        return deviceCacheStore.getDevice(deviceId);
    }

    public void doUpdateCache(DeviceProcessCache device){
        deviceCacheStore.update(device);
    }
}

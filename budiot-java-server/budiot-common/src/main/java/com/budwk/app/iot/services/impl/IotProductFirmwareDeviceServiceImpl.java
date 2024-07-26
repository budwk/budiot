package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product_firmware_device;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.iot.services.IotProductFirmwareDeviceService;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;

import java.util.List;

/**
 * @author wizzer
 */
@IocBean(args = {"refer:dao"})
public class IotProductFirmwareDeviceServiceImpl extends BaseServiceImpl<Iot_product_firmware_device> implements IotProductFirmwareDeviceService {
    public IotProductFirmwareDeviceServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private IotDeviceService iotDeviceService;


    public String importData(String productId, String firmwareId, String fileName, List<Iot_product_firmware_device> list, boolean over, String userId, String loginname) {
        if (list == null || list.size() == 0) {
            throw new BaseException("导入数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder resultMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Iot_product_firmware_device attr : list) {
            Cnd cnd = Cnd.NEW();
            cnd.and("productId", "=", productId);
            cnd.and(Cnd.exps("deviceNo", "=", Strings.trim(attr.getDeviceNo())).or("imei", "=", Strings.trim(attr.getImei())));
            Iot_device device = iotDeviceService.fetch(cnd);
            if (device != null) {
                attr.setProductId(productId);
                attr.setDeviceNo(device.getDeviceNo());
                attr.setImei(device.getImei());
                attr.setFirmwareId(firmwareId);
                attr.setDeviceId(device.getId());
                attr.setCreatedBy(userId);
                attr.setUpdatedBy(userId);
                try {
                    if (over) {
                        Cnd cnd2 = Cnd.NEW();
                        cnd2.and("productId", "=", productId);
                        cnd2.and("firmwareId", "=", firmwareId);
                        cnd2.and("deviceId", "=", device.getId());
                        Iot_product_firmware_device dbAttr = this.fetch(cnd2);
                        if (dbAttr != null) {
                            attr.setId(dbAttr.getId());
                            this.updateIgnoreNull(attr);
                        } else {
                            this.insert(attr);
                        }
                    } else {
                        this.insert(attr);
                    }
                    successNum++;
                } catch (Exception e) {
                    String duplicate = "";
                    if (e.getMessage().contains("Duplicate")) {
                        duplicate = "已存在";
                    } else {
                        duplicate = e.getMessage();
                    }
                    failureNum++;
                    String msg = "<br/>" + failureNum + "、deviceNo=" + attr.getDeviceNo() + " imei=" + attr.getImei() + " " + duplicate + "<br/>";
                    failureMsg.append(msg);
                }
            } else {
                failureNum++;
                String msg = "<br/>" + failureNum + "、产品设备不存在：deviceNo=" + attr.getDeviceNo() + " imei=" + attr.getImei() + "<br/>";
                failureMsg.append(msg);
            }
        }
        resultMsg.insert(0, "导入结果：共成功 " + successNum + " 条");
        if (failureNum > 0) {
            resultMsg.append("，失败 " + failureNum + " 条，失败数据如下：<br/>");
            resultMsg.append(failureMsg);
        }
        return resultMsg.toString();
    }

}
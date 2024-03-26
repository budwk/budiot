package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.models.Iot_protocol;
import com.budwk.app.iot.services.IotDeviceLogService;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.iot.services.IotProductService;
import com.budwk.app.iot.services.IotProtocolService;
import com.budwk.app.sys.enums.SysMsgType;
import com.budwk.app.sys.services.SysMsgService;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.database.service.BaseServiceImpl;
import com.budwk.starter.security.utils.SecurityUtil;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

import java.util.List;

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
}

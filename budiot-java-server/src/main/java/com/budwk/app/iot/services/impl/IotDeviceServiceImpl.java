package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.sys.enums.SysMsgType;
import com.budwk.app.sys.services.SysMsgService;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;

import java.util.Arrays;
import java.util.List;

@IocBean(args = {"refer:dao"})
public class IotDeviceServiceImpl extends BaseServiceImpl<Iot_device> implements IotDeviceService {
    public IotDeviceServiceImpl(Dao dao) {
        super(dao);
    }
    @Inject
    private SysMsgService sysMsgService;
    public void importData(String fileName, List<Iot_device> list, boolean over, String userId, String loginname) {
        if (list == null || list.size() == 0) {
            throw new BaseException("导入数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder resultMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        String[] strategicStrs = new String[]{
                "设备通信号",
                "设备编号",
                "IMEI",
                "ICCID"
        };
        for (Iot_device device : list) {
            device.setAbnormal(false);
            device.setOnline(false);
            device.setCreatedBy(userId);
            device.setUpdatedBy(userId);
            if (over) {
                int have = this.count(Cnd.where("deviceNo", "=", device.getDeviceNo()));
                if (have > 0) {
                    this.update(Chain.make("openNo", itl.getOpenNo())
                            .add("title", itl.getTitle())
                            .add("holder", itl.getHolder())
                            .add("ipc", itl.getIpc())
                            .add("strategic1", itl.getStrategic1())
                            .add("strategic2", itl.getStrategic2())
                            .add("strategic3", itl.getStrategic3())
                            .add("rightMustNum", itl.getRightMustNum())
                            .add("patentType", itl.getPatentType())
                            .add("legalStatus", itl.getLegalStatus())
                            .add("patentValid", itl.getPatentValid())
                            .add("endDate", itl.getEndDate())
                            .add("head", itl.getHead())
                            .add("status", 0)
                            .add("updatedBy", userId)
                            .add("updatedAt", System.currentTimeMillis()), Cnd.where("patentNo", "=", itl.getPatentNo())
                    );
                    successNum++;
                } else {
                    try {
                        this.fastInsert(itl);
                        successNum++;
                    } catch (Exception e) {
                        failureNum++;
                        String msg = "<br/>" + failureNum + "、专利申请号：" + itl.getPatentNo() + "<br/>";
                        failureMsg.append(msg + e.getMessage());
                    }
                }
            } else {
                try {
                    this.fastInsert(itl);
                    successNum++;
                } catch (Exception e) {
                    failureNum++;
                    String msg = "<br/>" + failureNum + "、专利申请号：" + itl.getPatentNo() + "<br/>";
                    failureMsg.append(msg + e.getMessage());
                }
            }
        }
        resultMsg.insert(0, "导入结果：共成功 " + successNum + " 条");
        if (failureNum > 0) {
            resultMsg.append("，失败 " + failureNum + " 条，失败数据如下：<br/>");
            resultMsg.append(failureMsg);
        }
        sysMsgService.sendMsg(userId, SysMsgType.USER, "专利评估备选 " + fileName + " 导入完成", "", resultMsg.toString(), userId);

    }
}

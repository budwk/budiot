package com.budwk.app.iot.services.impl;

import com.budwk.app.access.objects.dto.DeviceCmdDTO;
import com.budwk.app.access.storage.DeviceCmdDataStorage;
import com.budwk.app.iot.enums.DeviceCmdStatus;
import com.budwk.app.iot.models.Iot_device_cmd;
import com.budwk.app.iot.services.IotDeviceCmdService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;

@IocBean(args = {"refer:dao"})
public class IotDeviceCmdServiceImpl extends BaseServiceImpl<Iot_device_cmd> implements IotDeviceCmdService {
    public IotDeviceCmdServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private DeviceCmdDataStorage deviceCmdDataStorage;

    public int getWaitCount(String deviceId) {
        return this.count(Cnd.where("deviceId", "=", deviceId).and("status", "=", DeviceCmdStatus.WAIT.value()));
    }

    public void makeCommandSend(String cmdId) {
        this.update(Chain.make("status", DeviceCmdStatus.SEND.value()).add("sendTime", System.currentTimeMillis()), Cnd.where("id", "=", cmdId));
    }

    public void makeCommandFinish(String cmdId, DeviceCmdStatus status, String result) {
        Iot_device_cmd cmd = this.fetch(cmdId);
        if (cmd != null) {
            DeviceCmdDTO deviceCmdDTO = new DeviceCmdDTO();
            deviceCmdDTO.setCmdId(cmdId);
            deviceCmdDTO.setDeviceId(cmd.getDeviceId());
            deviceCmdDTO.setDeviceNo(cmd.getDeviceNo());
            deviceCmdDTO.setProductId(cmd.getProductId());
            deviceCmdDTO.setParams(cmd.getParams());
            deviceCmdDTO.setCode(cmd.getCode());
            deviceCmdDTO.setTs(System.currentTimeMillis());
            deviceCmdDTO.setFinishTime(System.currentTimeMillis());
            if (Lang.isEmpty(deviceCmdDTO.getSendTime())) {
                deviceCmdDTO.setSendTime(deviceCmdDTO.getFinishTime());
            }
            deviceCmdDTO.setStatus(status.value());
            deviceCmdDTO.setRespResult(result);
            deviceCmdDTO.setSerialNo(cmd.getSerialNo());
            if (status == DeviceCmdStatus.CANCELED) {
                deviceCmdDTO.setFinishTime(null);
            }
            deviceCmdDataStorage.save(deviceCmdDTO);
            this.delete(cmdId);
        }
    }
}

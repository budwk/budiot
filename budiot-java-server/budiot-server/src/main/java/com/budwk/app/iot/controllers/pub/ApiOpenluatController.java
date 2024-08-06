package com.budwk.app.iot.controllers.pub;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product_firmware;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.iot.services.IotProductFirmwareService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.log.annotation.SLog;
import com.budwk.starter.storage.StorageServer;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.GET;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.Param;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@IocBean
@At("/iot/api/openluat")
@SLog(tag = "合宙设备API接口")
@ApiDefinition(tag = "合宙设备API接口")
@Slf4j
public class ApiOpenluatController {
    @Inject("java:$conf.get('api.openluat.project_key')")
    private String project_key;

    @Inject
    private IotProductFirmwareService iotProductFirmwareService;
    @Inject
    private IotDeviceService iotDeviceService;
    @Inject
    private StorageServer storageServer;

    @At
    @Ok("void")
    @GET
    @ApiOperation(name = "获取初始化数据")
    @ApiImplicitParams
    @ApiResponses(
            value = {
                    @ApiResponse(name = "version", description = "设备版本号"),
                    @ApiResponse(name = "imei", description = "设备IMEI"),
                    @ApiResponse(name = "project_key", description = "设备校验KEY")
            }
    )
    public void upgrade(@Param("version") String version, @Param("imei") String imei, @Param("project_key") String device_project_key, HttpServletRequest req, HttpServletResponse resp) {
        log.info("version - {}", version);
        log.info("imei - {}", imei);
        log.info("project_key - {}", project_key);
        // 项目key 不匹配
        if (!Strings.sNull(project_key).equals(device_project_key)) {
            resp.setStatus(-5);
            return;
        }
        // imei 不存在
        Iot_device iotDevice = iotDeviceService.fetch(Cnd.where("imei", "=", imei));
        if (iotDevice == null) {
            resp.setStatus(-5);
            return;
        }
        List<Iot_product_firmware> listAll = new ArrayList<>();
        // 查找更新所有设备固件
        List<Iot_product_firmware> listAllUpgrade = iotProductFirmwareService.query(Cnd.where("productId", "=", iotDevice.getProductId()).and("enabled", "=", true).and("allUpgrade", "=", true).and("version", ">", version));
        // 查找指定设备固件
        Sql sql = Sqls.create("SELECT a.* from iot_product_firmware a,iot_product_firmware_device b where " +
                " a.id = b.firmwareId and a.productId=@productId and b.imei =@imei and a.enabled=true and allUpgrade=false and " +
                " a.version > @version ");
        sql.setParam("productId", iotDevice.getProductId());
        sql.setParam("imei", imei);
        sql.setParam("version", version);
        List<Iot_product_firmware> listImeiUpgrade = iotProductFirmwareService.listEntity(sql);
        // 将listAllUpgrade 和 listImeiUpgrade 合并
        listAll.addAll(listAllUpgrade);
        listAll.addAll(listImeiUpgrade);
        if (!listAll.isEmpty()) {
            // 按version倒序排序
            listAll.sort((o1, o2) -> o2.getVersion().compareTo(o1.getVersion()));
            // 取version最大的
            Iot_product_firmware firmware = listAll.get(0);
            try {
                storageServer.download(firmware.getPath(), resp.getOutputStream());
                resp.setStatus(200);
            } catch (IOException e) {
                resp.setStatus(-5);
            }
        } else {
            resp.setStatus(-5);
        }
    }
}

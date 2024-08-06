package com.budwk.app.iot.controllers.pub;

import com.budwk.app.iot.services.IotProductFirmwareService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.log.annotation.SLog;
import lombok.extern.slf4j.Slf4j;
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
        if(!Strings.sNull(project_key).equals(device_project_key)) {
            resp.setStatus(-5);
            return;
        }

        resp.setStatus(404);
    }
}

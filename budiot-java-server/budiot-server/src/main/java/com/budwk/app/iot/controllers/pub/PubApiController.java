package com.budwk.app.iot.controllers.pub;

import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.log.annotation.SLog;
import lombok.extern.slf4j.Slf4j;
import org.nutz.ioc.loader.annotation.IocBean;
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
public class PubApiController {
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
    public void upgrade(@Param("version") String version, @Param("imei") String imei, @Param("project_key") String project_key, HttpServletRequest req, HttpServletResponse resp) {
        log.info("version - {}", version);
        log.info("imei - {}", imei);
        log.info("project_key - {}", project_key);
        resp.setStatus(404);
    }
}

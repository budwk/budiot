package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.models.Iot_protocol;
import com.budwk.app.iot.services.IotProtocolService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.page.PageUtil;
import com.budwk.starter.common.page.Pagination;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.log.annotation.SLog;
import com.budwk.starter.security.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.mvc.annotation.*;

import javax.servlet.http.HttpServletRequest;

@IocBean
@At("/iot/admin/config/protocol")
@SLog(tag = "设备协议管理")
@ApiDefinition(tag = "设备协议")
@Slf4j
public class IotProtocolController {
    @Inject
    private IotProtocolService iotProtocolService;

    @At
    @POST
    @Ok("json:full")
    @SaCheckPermission("iot.config.protocol")
    @ApiOperation(name = "设备协议列表")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "name", example = "", description = "名称"),
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式")
            }
    )
    public Result<?> list(@Param("name") String name, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        cnd.and("delFlag", "=", false);
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        Pagination pagination = iotProtocolService.listPage(pageNo, pageSize, cnd);
        return Result.success(pagination);
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.config.protocol.create")
    @SLog(value = "新增设备协议:${protocol.name}")
    @ApiOperation(name = "新增设备协议")
    @ApiFormParams(
            implementation = Iot_protocol.class
    )
    @ApiResponses
    public Result<?> create(@Param("..") Iot_protocol protocol, HttpServletRequest req) {
        protocol.setCreatedBy(SecurityUtil.getUserId());
        protocol.setUpdatedBy(SecurityUtil.getUserId());
        if (iotProtocolService.count(Cnd.where("id", "=", protocol.getId())) > 0) {
            return Result.error("设备协议已存在");
        }
        iotProtocolService.insert(protocol);
        return Result.success();
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.config.protocol.update")
    @SLog(value = "修改设备协议:${protocol.name}")
    @ApiOperation(name = "修改设备协议")
    @ApiFormParams(
            implementation = Iot_protocol.class
    )
    @ApiResponses
    public Result<?> update(@Param("..") Iot_protocol protocol, HttpServletRequest req) {
        protocol.setUpdatedBy(SecurityUtil.getUserId());
        iotProtocolService.updateIgnoreNull(protocol);
        return Result.success();
    }


    @At("/delete")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.config.protocol.delete")
    @SLog(value = "删除设备协议:${name}")
    @ApiOperation(name = "删除设备协议")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "id", description = "主键ID", required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> delete(@Param("id") String id,@Param("name") String name, HttpServletRequest req) {
        iotProtocolService.delete(id);
        return Result.success();
    }

    @At("/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取设备协议信息")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> get(String id, HttpServletRequest req) {
        return Result.success(iotProtocolService.fetch(id));
    }

}

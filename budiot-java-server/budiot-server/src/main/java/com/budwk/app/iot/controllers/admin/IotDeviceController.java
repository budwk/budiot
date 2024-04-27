package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.enums.DeviceType;
import com.budwk.app.iot.enums.IotPlatform;
import com.budwk.app.iot.enums.ProtocolType;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.services.IotClassifyService;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.iot.services.IotProductService;
import com.budwk.app.iot.services.IotProtocolService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.page.PageUtil;
import com.budwk.starter.common.page.Pagination;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.excel.utils.ExcelUtil;
import com.budwk.starter.log.annotation.SLog;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.mvc.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@IocBean
@At("/iot/admin/device/device")
@SLog(tag = "设备列表")
@ApiDefinition(tag = "设备列表")
@Slf4j
public class IotDeviceController {
    @Inject
    private IotClassifyService iotClassifyService;
    @Inject
    private IotProtocolService iotProtocolService;
    @Inject
    private IotProductService iotProductService;
    @Inject
    private IotDeviceService iotDeviceService;

    @At("/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "设备列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "classifyId", example = "", description = "分类ID"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "filedName", example = "", description = "查询字段"),
                    @ApiFormParam(name = "filedValue", example = "", description = "字段值")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.device")
    public Result<?> list(@Param("classifyId") String classifyId, @Param("productId") String productId, @Param("filedName") String filedName, @Param("filedValue") String filedValue, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(classifyId)) {
            cnd.and("classifyId", "=", classifyId);
        }
        if (Strings.isNotBlank(productId)) {
            cnd.and("productId", "=", productId);
        }
        if (Strings.isNotBlank(filedValue)) {
            cnd.and(Cnd.likeEX(filedName, filedValue));
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(iotDeviceService.listPage(pageNo, pageSize, cnd));
    }

    @At("/export")
    @Ok("void")
    @POST
    @ApiOperation(name = "导出设备数据")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "productId", description = "产品ID"),
                    @ApiFormParam(name = "fieldNames", description = "字段名"),
                    @ApiFormParam(name = "ids", description = "数据ID数组")
            }
    )
    @ApiResponses
    @SaCheckPermission("iot.device.device.export")
    public void deviceExport(@Param("ids") String[] ids,@Param("classifyId") String classifyId, @Param("fieldNames") String[] fieldNames, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy, HttpServletRequest req, HttpServletResponse response) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(classifyId)) {
            cnd.and("classifyId", "=", classifyId);
        }
        if (ids != null) {
            cnd.and("id", "in", ids);
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        try {
            List<Iot_device> list = iotDeviceService.query(cnd);
            ExcelUtil<Iot_device> util = new ExcelUtil<>(Iot_device.class);
            util.exportExcel(response, list, "设备数据", fieldNames);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @At("/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取设备信息")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> get(String id, HttpServletRequest req) {
        Iot_device device = iotDeviceService.fetch(id);
        iotDeviceService.fetchLinks(device, "^(propList)$");
        return Result.success(device);
    }
}

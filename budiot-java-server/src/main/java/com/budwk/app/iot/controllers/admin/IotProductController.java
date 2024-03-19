package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.enums.DeviceType;
import com.budwk.app.iot.enums.IotPlatform;
import com.budwk.app.iot.enums.ProtocolType;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.services.IotClassifyService;
import com.budwk.app.iot.services.IotProductService;
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
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.*;

import javax.servlet.http.HttpServletRequest;

@IocBean
@At("/iot/admin/device/product")
@SLog(tag = "产品管理")
@ApiDefinition(tag = "产品管理")
@Slf4j
public class IotProductController {

    @Inject
    private IotClassifyService iotClassifyService;

    @Inject
    private IotProtocolService iotProtocolService;

    @Inject
    private IotProductService iotProductService;

    @At
    @Ok("json")
    @GET
    @ApiOperation(name = "获取初始化数据")
    @ApiImplicitParams
    @ApiResponses(
            value = {
                    @ApiResponse(name = "typeList", description = "设备类型"),
                    @ApiResponse(name = "supplierList", description = "厂家列表"),
                    @ApiResponse(name = "iotPlatform", description = "接入平台")
            }
    )
    @SaCheckLogin
    public Result<?> init(HttpServletRequest req) {
        NutMap map = NutMap.NEW();
        map.addv("classifyList", iotClassifyService.query(Cnd.NEW()));
        map.addv("protocolList", iotProtocolService.query(Cnd.NEW()));
        map.addv("iotPlatform", IotPlatform.values());
        map.addv("protocolType", ProtocolType.values());
        map.addv("deviceType", DeviceType.values());
        return Result.success(map);
    }

    @At
    @Ok("json")
    @POST
    @ApiOperation(name = "分页查询")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "classifyId", example = "", description = "设备分类"),
                    @ApiFormParam(name = "name", example = "", description = "产品名称")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> list(@Param("classifyId") String classifyId, @Param("name") String name, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(classifyId)) {
            cnd.and("classifyId", "=", classifyId);
        }
        if (Strings.isNotBlank(name)) {
            cnd.and(Cnd.likeEX("name", name));
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(iotProductService.listPage(pageNo, pageSize, cnd));
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.create")
    @SLog(value = "新增产品:${product.name}")
    @ApiOperation(name = "新增产品")
    @ApiFormParams(
            implementation = Iot_product.class
    )
    @ApiResponses
    public Result<?> create(@Param("..") Iot_product product, HttpServletRequest req) {
        product.setCreatedBy(SecurityUtil.getUserId());
        product.setUpdatedBy(SecurityUtil.getUserId());
        iotProductService.insert(product);
        return Result.success();
    }


    @At("/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取产品信息")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> get(String id, HttpServletRequest req) {
        return Result.success(iotProductService.fetch(id));
    }

}

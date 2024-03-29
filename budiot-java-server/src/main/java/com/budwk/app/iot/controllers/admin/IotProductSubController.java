package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.models.Iot_product_sub;
import com.budwk.app.iot.services.IotProductSubService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.page.PageUtil;
import com.budwk.starter.common.page.Pagination;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.log.annotation.SLog;
import com.budwk.starter.security.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.mvc.annotation.*;

import javax.servlet.http.HttpServletRequest;

@IocBean
@At("/iot/admin/device/sub/sub")
@SLog(tag = "订阅-订阅列表")
@ApiDefinition(tag = "订阅-订阅列表")
@Slf4j
public class IotProductSubController {
    @Inject
    private IotProductSubService iotProductSubService;

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
                    @ApiFormParam(name = "productId", example = "", description = "订阅ID")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> list(@Param("productId") String productId,  @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        cnd.and("productId","=",productId);
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(iotProductSubService.listPage(pageNo, pageSize, cnd));
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.sub")
    @SLog(value = "新增订阅:${sub.url}")
    @ApiOperation(name = "新增订阅")
    @ApiFormParams(
            implementation = Iot_product_sub.class
    )
    @ApiResponses
    public Result<?> create(@Param("..") Iot_product_sub sub, HttpServletRequest req) {
        sub.setCreatedBy(SecurityUtil.getUserId());
        sub.setUpdatedBy(SecurityUtil.getUserId());
        iotProductSubService.insert(sub);
        return Result.success();
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.sub")
    @SLog(value = "修改订阅:${sub.url}")
    @ApiOperation(name = "修改订阅")
    @ApiFormParams(
            implementation = Iot_product_sub.class
    )
    @ApiResponses
    public Result<?> update(@Param("..") Iot_product_sub sub, HttpServletRequest req) {
        sub.setUpdatedBy(SecurityUtil.getUserId());
        iotProductSubService.updateIgnoreNull(sub);
        return Result.success();
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.sub")
    @SLog(value = "删除订阅:${url}")
    @ApiOperation(name = "删除订阅")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "name", description = "订阅URL")
            }
    )
    @ApiResponses
    public Result<?> delete(@Param("id") String id, @Param("url") String url, HttpServletRequest req) {
        iotProductSubService.delete(id);
        return Result.success();
    }

    @At("/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取订阅信息")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> get(String id, HttpServletRequest req) {
        Iot_product_sub sub = iotProductSubService.fetch(id);
        return Result.success(sub);
    }

    @At("/enabled")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.sub")
    @ApiOperation(name = "启用禁用订阅")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "url", description = "订阅URL")
            }
    )
    @ApiResponses
    public Result<?> subEnabled(@Param("id") String id, @Param("url") String url, @Param("enabled") boolean enabled, HttpServletRequest req) {
        iotProductSubService.update(Chain.make("enabled", enabled), Cnd.where("id", "=", id));
        return Result.success();
    }
}

package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.models.Iot_product_dtu;
import com.budwk.app.iot.models.Iot_product_firmware;
import com.budwk.app.iot.services.IotProductDtuService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.log.annotation.SLog;
import com.budwk.starter.security.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.*;

import javax.servlet.http.HttpServletRequest;

@IocBean
@At("/iot/admin/device/product/dtu")
@SLog(tag = "产品-DTU参数管理")
@ApiDefinition(tag = "产品-DTU参数管理")
@Slf4j
public class IotProductDtuController {
    @Inject
    private IotProductDtuService iotProductDtuService;

    @At("/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取DTU参数信息")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "productId", description = "产品ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getDtu(String productId, HttpServletRequest req) {
        Iot_product_dtu productDtu = iotProductDtuService.fetch(Cnd.where("productId", "=", productId));
        if (productDtu == null) {
            return Result.data(NutMap.NEW().addv("enabled", false).addv("version", 0));
        }
        return Result.data(productDtu);
    }

    @At("/enabled")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.dtuparam")
    @ApiOperation(name = "启用禁用DTU参数")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "productId", description = "产品ID"),
                    @ApiFormParam(name = "enabled", description = "是否启用")
            }
    )
    @ApiResponses
    public Result<?> dtuEnabled(@Param("productId") String productId, @Param("enabled") boolean enabled, HttpServletRequest req) {
        if (iotProductDtuService.count(Cnd.where("productId", "=", productId)) < 1) {
            return Result.error("DTU参数配置未保存");
        }
        iotProductDtuService.update(Chain.make("enabled", enabled), Cnd.where("productId", "=", productId));
        return Result.success();
    }

    @At("/save")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.dtuparam")
    @SLog(value = "保存配置:${productId}")
    @ApiOperation(name = "保存配置")
    @ApiFormParams(
            implementation = Iot_product_dtu.class
    )
    @ApiResponses
    public Result<?> firmwareCreate(@Param("..") Iot_product_dtu productDtu, HttpServletRequest req) {
        productDtu.setCreatedBy(SecurityUtil.getUserId());
        productDtu.setUpdatedBy(SecurityUtil.getUserId());
        iotProductDtuService.save(productDtu);
        return Result.success();
    }
}

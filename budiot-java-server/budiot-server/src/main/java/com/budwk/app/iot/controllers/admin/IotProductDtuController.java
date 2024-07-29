package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.budwk.app.iot.models.Iot_product_dtu;
import com.budwk.app.iot.services.IotProductDtuService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.log.annotation.SLog;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.GET;
import org.nutz.mvc.annotation.Ok;

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
}

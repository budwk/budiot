package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.models.Iot_product_cmd;
import com.budwk.app.iot.models.Iot_scene;
import com.budwk.app.iot.models.Iot_scene_space;
import com.budwk.app.iot.services.*;
import com.budwk.starter.common.openapi.annotation.ApiDefinition;
import com.budwk.starter.common.page.PageUtil;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.common.result.ResultCode;
import com.budwk.starter.log.annotation.SLog;
import com.budwk.starter.security.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.mvc.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@IocBean
@At("/iot/admin/scene")
@SLog(tag = "场景联动-场景管理")
@ApiDefinition(tag = "场景联动-场景管理")
@Slf4j
public class IotSceneController {
    @Inject
    private IotSceneService iotSceneService;
    @Inject
    private IotProductService iotProductService;
    @Inject
    private IotDeviceService iotDeviceService;
    @Inject
    private IotProductAttrService iotProductAttrService;
    @Inject
    private IotProductCmdAttrService iotProductCmdAttrService;
    @Inject
    private IotProductCmdService iotProductCmdService;
    @Inject
    private IotSceneSpaceService iotSceneSpaceService;

    @At
    @Ok("json")
    @POST
    @SaCheckPermission("iot.device.scene")
    public Result<?> list(@Param("spaceId") String spaceId, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        // 查询所有场景空间
        Iot_scene_space iotSceneSpace = iotSceneSpaceService.fetch(spaceId);
        if (iotSceneSpace != null) {
            List<Iot_scene_space> list = iotSceneSpaceService.query(Cnd.where("path", "like", iotSceneSpace.getPath() + "%"));
            // 得到is数组
            String[] ids = list.stream().map(Iot_scene_space::getId).toArray(String[]::new);
            if (!Lang.isEmpty(ids)) {
                cnd.and("spaceId", "in", ids);
            }
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        } else {
            cnd.desc("createdAt");
        }
        return Result.data(iotSceneService.listPage(pageNo, pageSize, cnd));
    }

    @At
    @Ok("json")
    @POST
    @SLog("新增场景:${iotScene.name}")
    @SaCheckPermission("iot.device.scene.create")
    public Result<?> create(@Param("..") Iot_scene iotScene, HttpServletRequest req) {
        iotScene.setCreatedBy(SecurityUtil.getUserId());
        iotScene.setUpdatedBy(SecurityUtil.getUserId());
        iotSceneService.save(iotScene);
        return Result.success();
    }

    @At
    @Ok("json")
    @POST
    @SLog("修改场景:${iotScene.name}")
    @SaCheckPermission("iot.device.scene.update")
    public Result<?> update(@Param("..") Iot_scene iotScene, HttpServletRequest req) {
        iotScene.setUpdatedBy(SecurityUtil.getUserId());
        iotSceneService.update(iotScene);
        return Result.success();
    }

    @At("/get/{id}")
    @Ok("json")
    @GET
    @SaCheckPermission("iot.device.scene")
    public Result<?> getData(String id, HttpServletRequest req) {
        Iot_scene iotScene = iotSceneService.fetch(id);
        if (iotScene == null) {
            return Result.error(ResultCode.NULL_DATA_ERROR);
        }
        return Result.data(iotSceneService.fetchLinks(iotScene, "^(terms|actions)$"));
    }

    @At("/delete")
    @Ok("json")
    @POST
    @SLog("删除场景:${iotScene.name}")
    @SaCheckPermission("iot.device.scene.delete")
    public Result<?> delete(@Param("..") Iot_scene iotScene, HttpServletRequest req) {
        iotSceneService.delete(iotScene);
        return Result.success();
    }

    @At("/enabled_change")
    @Ok("json")
    @POST
    @SLog("修改场景:${iotScene.name}状态")
    @SaCheckPermission("iot.device.scene.update")
    public Result<?> enabledChange(@Param("id") String id, @Param("name") String name, @Param("enabled") boolean enabled, HttpServletRequest req) {
        iotSceneService.update(Chain.make("enabled", enabled), Cnd.where("id", "=", id));
        return Result.success();
    }

    @At("/product/list")
    @Ok("json")
    @POST
    @SaCheckPermission("iot.device.scene")
    public Result<?> productList() {
        Cnd cnd = Cnd.NEW();
        cnd.desc("createdAt");
        return Result.data(iotProductService.query(cnd, "^(attrs|cmds)$"));
    }

    @At("/device/list")
    @Ok("json")
    @POST
    @SaCheckPermission("iot.device.scene")
    public Result<?> deviceList(@Param("productId") String productId,@Param("filedName") String filedName,@Param("filedValue") String filedValue,@Param("pageNo") int pageNo, @Param("pageSize") int pageSize) {
        Cnd cnd = Cnd.NEW();
        cnd.and("productId","=",productId);
        if(Strings.isNotBlank(filedName)&&Strings.isNotBlank(filedValue)){
            cnd.and(filedName,"like","%"+filedValue+"%");
        }
        cnd.desc("createdAt");
        return Result.data(iotDeviceService.listPage(pageNo,pageSize,cnd));
    }

    @At("/cmd/params")
    @Ok("json")
    @POST
    @SaCheckPermission("iot.device.scene")
    public Result<?> cmdParams(@Param("productId") String productId, @Param("cmdCode") String cmdCode) {
        Iot_product_cmd productCmd = iotProductCmdService.fetch(Cnd.where("productId", "=", productId).and("code", "=", cmdCode));
        if (productCmd == null) {
            return Result.error(ResultCode.NULL_DATA_ERROR, "指令不存在");
        }
        Cnd cnd = Cnd.NEW();
        cnd.and("productId", "=", productId);
        cnd.and("cmdId", "=", productCmd.getId());
        cnd.asc("id");
        return Result.data(iotProductCmdAttrService.query(cnd));
    }
}
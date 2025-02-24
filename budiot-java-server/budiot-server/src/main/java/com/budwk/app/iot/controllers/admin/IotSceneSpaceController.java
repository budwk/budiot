package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.models.Iot_classify;
import com.budwk.app.iot.models.Iot_scene_space;
import com.budwk.app.iot.services.IotSceneSpaceService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.page.PageUtil;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.common.result.ResultCode;
import com.budwk.starter.log.annotation.SLog;
import com.budwk.starter.security.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;


@IocBean
@At("/iot/admin/scene/space")
@SLog(tag = "场景联动-场景空间")
@ApiDefinition(tag = "场景联动-场景空间")
@Slf4j
public class IotSceneSpaceController {
    @Inject
    private IotSceneSpaceService iotSceneSpaceService;

    @At
    @Ok("json")
    @POST
    @SaCheckPermission("iot.device.scene")
    public Result<?> list() {
        Cnd cnd = Cnd.NEW();
        cnd.asc("location");
        cnd.asc("path");
        return Result.data(iotSceneSpaceService.query(cnd));
    }

    @At
    @Ok("json")
    @POST
    @SLog("新增空间:${iotSceneSpace.name}")
    @SaCheckPermission("iot.device.scene.create")
    @ApiOperation(name = "新增空间")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "parentId", description = "父级ID")
            },
            implementation = Iot_classify.class
    )
    @ApiResponses
    public Result<?> create(@Param("..") Iot_scene_space iotSceneSpace, @Param("parentId") String parentId, HttpServletRequest req) {
        if ("root".equals(parentId) || parentId == null) {
            parentId = "";
        }
        iotSceneSpace.setCreatedBy(SecurityUtil.getUserId());
        iotSceneSpace.setUpdatedBy(SecurityUtil.getUserId());
        iotSceneSpaceService.save(iotSceneSpace, parentId);
        return Result.success();
    }

    @At
    @Ok("json")
    @POST
    @SLog("修改空间:${iotSceneSpace.name}")
    @SaCheckPermission("iot.device.scene.update")
    @ApiOperation(name = "修改空间")
    @ApiFormParams(
            implementation = Iot_classify.class
    )
    @ApiResponses
    public Result<?> update(@Param("..") Iot_scene_space iotSceneSpace, HttpServletRequest req) {
        iotSceneSpace.setUpdatedBy(SecurityUtil.getUserId());
        iotSceneSpaceService.updateIgnoreNull(iotSceneSpace);
        return Result.success();
    }

    @At("/get/{id}")
    @Ok("json")
    @GET
    @SaCheckPermission("iot.device.scene")
    public Result<?> getData(String id, HttpServletRequest req) {
        Iot_scene_space iotSceneSpace = iotSceneSpaceService.fetch(id);
        if (iotSceneSpace == null) {
            return Result.error(ResultCode.NULL_DATA_ERROR);
        }
        return Result.data(iotSceneSpace);
    }

    @At("/delete")
    @Ok("json")
    @POST
    @SLog("删除空间:${name}")
    @SaCheckPermission("iot.device.scene.delete")
    public Result<?> delete(@Param("id") String id, @Param("name") String name, HttpServletRequest req) {
        Iot_scene_space iotSceneSpace = iotSceneSpaceService.fetch(id);
        if (iotSceneSpace == null) {
            return Result.error(ResultCode.NULL_DATA_ERROR);
        }
        req.setAttribute("_slog_msg", iotSceneSpace.getName());
        iotSceneSpaceService.deleteAndChild(iotSceneSpace);
        return Result.success();
    }

    @At("/get_sort_tree")
    @Ok("json")
    @GET
    @SaCheckPermission("iot.device.scene")
    @ApiOperation(name = "获取待排序数据")
    @ApiImplicitParams
    @ApiResponses
    public Result<?> getSortTree(HttpServletRequest req) {
        List<Iot_scene_space> list = iotSceneSpaceService.query(Cnd.NEW().asc("location").asc("path"));
        NutMap nutMap = NutMap.NEW();
        for (Iot_scene_space sceneSpace : list) {
            List<Iot_scene_space> list1 = nutMap.getList(sceneSpace.getParentId(), Iot_scene_space.class);
            if (list1 == null) {
                list1 = new ArrayList<>();
            }
            list1.add(sceneSpace);
            nutMap.put(Strings.sNull(sceneSpace.getParentId()), list1);
        }
        return Result.data(getTree(nutMap, ""));
    }

    private List<NutMap> getTree(NutMap nutMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Iot_scene_space> subList = nutMap.getList(pid, Iot_scene_space.class);
        for (Iot_scene_space sceneSpace : subList) {
            NutMap map = Lang.obj2nutmap(sceneSpace);
            map.put("label", sceneSpace.getName());
            if (sceneSpace.isHasChildren() || (nutMap.get(sceneSpace.getId()) != null)) {
                map.put("children", getTree(nutMap, sceneSpace.getId()));
            }
            treeList.add(map);
        }
        return treeList;
    }

    @At("/sort")
    @Ok("json")
    @POST
    @SaCheckPermission("iot.device.scene.update")
    @ApiOperation(name = "保存排序数据")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "ids", description = "ids数组")
            }
    )
    @ApiResponses
    public Result<?> sortDo(@Param("ids") String ids, HttpServletRequest req) {
        String[] unitIds = StringUtils.split(ids, ",");
        int i = 0;
        iotSceneSpaceService.update(Chain.make("location", 0), Cnd.NEW());
        for (String id : unitIds) {
            if (!Strings.isBlank(id)) {
                iotSceneSpaceService.update(Chain.make("location", i), Cnd.where("id", "=", id));
                i++;
            }
        }
        return Result.success();
    }
}
package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.models.Iot_classify;
import com.budwk.app.iot.services.IotClassifyService;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.page.Pagination;
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
@At("/iot/admin/classify")
@SLog(tag = "设备分类管理")
@ApiDefinition(tag = "设备分类管理")
@Slf4j
public class IotClassifyController {
    @Inject
    private IotClassifyService iotClassifyService;

    @At
    @Ok("json")
    @GET
    @ApiOperation(name = "Vue3树形列表查询")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "name", example = "", description = "单位名称")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.config.classify")
    public Result<?> list(@Param("name") String name) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        cnd.asc("location");
        cnd.asc("path");
        return Result.success().addData(iotClassifyService.query(cnd));
    }

    @At("/tree")
    @Ok("json")
    @GET
    @SaCheckPermission("iot.config.classify")
    @ApiOperation(name = "获取待选择树型数据")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "pid", description = "父级ID")
            }
    )
    @ApiResponses
    public Result<?> getTree(@Param("pid") String pid, HttpServletRequest req) {
        List<Iot_classify> list = new ArrayList<>();
        List<NutMap> treeList = new ArrayList<>();
        if (Strings.isBlank(pid)) {
            NutMap root = NutMap.NEW().addv("value", "root").addv("label", "默认顶级").addv("leaf", true);
            treeList.add(root);
        }
        Cnd cnd = Cnd.NEW();
        if (Strings.isBlank(pid)) {
            cnd.and("parentId", "=", "").or("parentId", "is", null);
        } else {
            cnd.and("parentId", "=", pid);
        }
        cnd.asc("location").asc("path");
        list = iotClassifyService.query(cnd);
        for (Iot_classify dict : list) {
            NutMap map = NutMap.NEW().addv("value", dict.getId()).addv("label", dict.getName());
            if (dict.isHasChildren()) {
                map.addv("children", new ArrayList<>());
                map.addv("leaf", false);
            } else {
                map.addv("leaf", true);
            }
            treeList.add(map);
        }
        return Result.data(treeList);
    }

    @At("/create")
    @Ok("json")
    @POST
    @SaCheckPermission("iot.config.classify.create")
    @SLog(value = "新增设备分类:${dict.name}")
    @ApiOperation(name = "新增设备分类")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "parentId", description = "父级ID")
            },
            implementation = Iot_classify.class
    )
    @ApiResponses
    public Result<?> create(@Param("..") Iot_classify classify, @Param("parentId") String parentId, HttpServletRequest req) {
        if ("root".equals(parentId) || parentId == null) {
            parentId = "";
        }
        classify.setCreatedBy(SecurityUtil.getUserId());
        iotClassifyService.save(classify, parentId);
        return Result.success();
    }

    @At("/delete/{id}")
    @Ok("json")
    @DELETE
    @SaCheckPermission("iot.config.classify.delete")
    @SLog(value = "删除设备分类:")
    @ApiOperation(name = "删除设备分类")
    @ApiImplicitParams(
            value = {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH)
            }
    )
    @ApiResponses
    public Result<?> delete(String id, HttpServletRequest req) {
        Iot_classify classify = iotClassifyService.fetch(id);
        if (classify == null) {
            return Result.error(ResultCode.NULL_DATA_ERROR);
        }
        req.setAttribute("_slog_msg", classify.getName());
        iotClassifyService.deleteAndChild(classify);
        return Result.success();
    }

    @At("/get/{id}")
    @Ok("json")
    @GET
    @ApiOperation(name = "获取设备分类")
    @ApiImplicitParams(
            value = {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH)
            }
    )
    @ApiResponses
    public Result<?> getData(String id, HttpServletRequest req) {
        Iot_classify classify = iotClassifyService.fetch(id);
        if (classify == null) {
            return Result.error(ResultCode.NULL_DATA_ERROR);
        }
        return Result.data(classify);
    }

    @At
    @Ok("json")
    @POST
    @SaCheckPermission("iot.config.classify.update")
    @SLog(value = "修改设备分类:${dict.name}")
    @ApiOperation(name = "修改设备分类")
    @ApiFormParams(
            implementation = Iot_classify.class
    )
    @ApiResponses
    public Result<?> update(@Param("..") Iot_classify classify, HttpServletRequest req) {
        classify.setUpdatedBy(SecurityUtil.getUserId());
        iotClassifyService.updateIgnoreNull(classify);
        return Result.success();
    }

    @At("/get_sort_tree")
    @Ok("json")
    @GET
    @SaCheckPermission("iot.config.classify")
    @ApiOperation(name = "获取待排序数据")
    @ApiImplicitParams
    @ApiResponses
    public Result<?> getSortTree(HttpServletRequest req) {
        List<Iot_classify> list = iotClassifyService.query(Cnd.NEW().asc("location").asc("path"));
        NutMap nutMap = NutMap.NEW();
        for (Iot_classify classify : list) {
            List<Iot_classify> list1 = nutMap.getList(classify.getParentId(), Iot_classify.class);
            if (list1 == null) {
                list1 = new ArrayList<>();
            }
            list1.add(classify);
            nutMap.put(Strings.sNull(classify.getParentId()), list1);
        }
        return Result.data(getTree(nutMap, ""));
    }

    private List<NutMap> getTree(NutMap nutMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Iot_classify> subList = nutMap.getList(pid, Iot_classify.class);
        for (Iot_classify classify : subList) {
            NutMap map = Lang.obj2nutmap(classify);
            map.put("label", classify.getName());
            if (classify.isHasChildren() || (nutMap.get(classify.getId()) != null)) {
                map.put("children", getTree(nutMap, classify.getId()));
            }
            treeList.add(map);
        }
        return treeList;
    }

    @At("/sort")
    @Ok("json")
    @POST
    @SaCheckPermission("iot.config.classify.update")
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
        iotClassifyService.update(Chain.make("location", 0), Cnd.NEW());
        for (String id : unitIds) {
            if (!Strings.isBlank(id)) {
                iotClassifyService.update(Chain.make("location", i), Cnd.where("id", "=", id));
                i++;
            }
        }
        return Result.success();
    }
}

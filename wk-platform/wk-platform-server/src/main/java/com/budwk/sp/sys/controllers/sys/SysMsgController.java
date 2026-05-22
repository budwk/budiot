package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.sys.dto.SysMsgDTO;
import com.budwk.sp.sys.entity.Sys_msg;
import com.budwk.sp.sys.enums.SysMsgScope;
import com.budwk.sp.sys.enums.SysMsgType;
import com.budwk.sp.sys.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Lang;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/msg")
@SLog(tag = "消息管理")
@Tag(name = "消息管理", description = "站内消息管理接口")
public class SysMsgController {

    @Autowired
    private SysMsgService sysMsgService;
    @Autowired
    private SysAppService sysAppService;
    @Autowired
    private SysMsgUserService sysMsgUserService;
    @Autowired
    private SysUnitService sysUnitService;
    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/data")
    @Operation(summary = "获取配置数据")
    @SaCheckLogin
    public Result<?> data() {
        Map<String, Object> map = new HashMap<>();
        map.put("apps", sysAppService.listAll());
        map.put("types", List.of(SysMsgType.values()).stream().map(type -> {
            Map<String, String> item = new HashMap<>();
            item.put("text", type.getText());
            item.put("value", type.getValue());
            return item;
        }).toList());
        map.put("scopes", List.of(SysMsgScope.values()).stream().map(scope -> {
            Map<String, String> item = new HashMap<>();
            item.put("text", scope.getText());
            item.put("value", scope.getValue());
            return item;
        }).toList());
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "获取信息列表")
    @SaCheckPermission("sys.manage.msg")
    public Result<?> list(
            @RequestBody(required = false) Map<String, Object> body,
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "消息类型") @RequestParam(required = false) String type,
            @Parameter(description = "消息标题") @RequestParam(required = false) String title,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (StringUtils.hasText(appId)) {
            cnd.and("appId", "=", appId);
        }
        if (StringUtils.hasText(type)) {
            cnd.and("type", "=", type);
        }
        if (StringUtils.hasText(title)) {
            cnd.and(Cnd.likeEX("title", title));
        }
        if (StringUtils.hasText(pageOrderName) && StringUtils.hasText(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        List<NutMap> mapList = new ArrayList<>();
        Pagination pagination = sysMsgService.listPageLinks(pageNo, pageSize, cnd, "^(createdByUser)$");
        for (Sys_msg msg : pagination.getList(Sys_msg.class)) {
            NutMap map = Lang.obj2nutmap(msg);
            map.put("all_num", sysMsgUserService.count(Cnd.where("msgId", "=", msg.getId())));
            map.put("unread_num", sysMsgUserService.count(Cnd.where("msgId", "=", msg.getId()).and("status", "=", 0)));
            mapList.add(map);
        }
        pagination.setList(mapList);
        return Result.data(pagination);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取消息详情")
    @SaCheckPermission("sys.manage.msg")
    public Result<?> get(@Parameter(description = "消息ID") @PathVariable String id) {
        Sys_msg msg = sysMsgService.fetch(id);
        if (msg == null) {
            return Result.error("数据不存在");
        }
        sysMsgService.fetchLinks(msg, "^(createdByUser)$");
        return Result.data(msg);
    }

    @PostMapping("/get_user_view_list")
    @Operation(summary = "获取发送用户列表")
    @SaCheckPermission("sys.manage.msg")
    public Result<?> getUserViewList(
            @RequestBody(required = false) Map<String, Object> body,
            @Parameter(description = "消息类型: all-全部/unread-未读") @RequestParam(required = false) String type,
            @Parameter(description = "消息ID") @RequestParam(required = false) String id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        Sql sql = Sqls.create("SELECT a.loginname,a.username,a.mobile,a.email,a.disabled,a.unitid,b.name as unitname,c.status,c.readat FROM sys_user a,sys_unit b,sys_msg_user c WHERE a.unitid=b.id and a.loginname=c.loginname and c.msgId=@msgId $condition");
        sql.setParam("msgId", id);
        if ("unread".equals(type)) {
            sql.setCondition(Cnd.where("c.status", "=", 0));
        }
        if (StringUtils.hasText(pageOrderName) && StringUtils.hasText(pageOrderBy)) {
            sql.setCondition(Cnd.orderBy().orderBy("a." + pageOrderName, PageUtil.getOrder(pageOrderBy)));
        }
        return Result.data(sysMsgService.listPage(pageNo, pageSize, sql));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "撤回消息")
    @SaCheckPermission("sys.manage.msg.delete")
    public Result<?> delete(@Parameter(description = "消息ID") @PathVariable String id) {
        Sys_msg msg = sysMsgService.fetch(id);
        if (msg == null) {
            return Result.error("数据不存在");
        }
        sysMsgService.deleteMsg(id);
        return Result.success();
    }

    @PostMapping("/select_user_list")
    @Operation(summary = "获取选择用户列表")
    @SaCheckPermission("sys.manage.msg")
    public Result<?> selectUserList(
            @RequestBody(required = false) Map<String, Object> body,
            @Parameter(description = "查询字段") @RequestParam(required = false) String searchName,
            @Parameter(description = "查询关键词") @RequestParam(required = false) String searchKeyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (!StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            String unitId = StpUtil.getSession().getString("unitId");
            cnd.and("unitPath", "like", sysUnitService.getMasterCompanyPath(unitId) + "%");
        }
        if (StringUtils.hasText(searchName) && StringUtils.hasText(searchKeyword)) {
            cnd.and(searchName, "like", "%" + searchKeyword + "%");
        }
        if (StringUtils.hasText(pageOrderName) && StringUtils.hasText(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(sysUserService.listPageLinks(pageNo, pageSize, cnd, "unit"));
    }

    @PostMapping("/create")
    @Operation(summary = "发送消息")
    @SaCheckPermission("sys.manage.msg.create")
    public Result<?> create(@RequestBody @Validated SysMsgDTO dto) {
        Sys_msg msg = new Sys_msg();
        BeanUtils.copyProperties(dto, msg);
        msg.setSendAt(System.currentTimeMillis());
        msg.setCreatedBy(StpUtil.getLoginIdAsString());
        msg.setUpdatedBy(StpUtil.getLoginIdAsString());
        msg.setTenantId(StpUtil.getSession().getString("tenantId"));
        sysMsgService.saveMsg(msg, dto.getUsers());
        return Result.success();
    }
}

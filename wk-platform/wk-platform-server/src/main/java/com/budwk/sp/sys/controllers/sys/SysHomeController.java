package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.utils.PwdUtil;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.enums.SysMsgScope;
import com.budwk.sp.sys.enums.SysMsgType;
import com.budwk.sp.sys.providers.ISysConfigProvider;
import com.budwk.sp.sys.providers.ISysMsgProvider;
import com.budwk.sp.sys.services.SysLogService;
import com.budwk.sp.sys.services.SysMsgService;
import com.budwk.sp.sys.services.SysMsgUserService;
import com.budwk.sp.sys.services.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.nutz.lang.Times;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/home")
@SLog(tag = "用户中心")
@Tag(name = "用户中心", description = "用户中心接口")
public class SysHomeController {
    @Autowired
    private SysMsgService sysMsgService;
    @Autowired
    private SysMsgUserService sysMsgUserService;
    @Autowired
    private ISysConfigProvider sysConfigProvider;
    @Autowired
    private ISysMsgProvider sysMsgProvider;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysLogService sysLogService;

    @GetMapping("/msg/wsmsg")
    @Operation(summary = "获取websocket消息")
    @SaCheckLogin
    public Result<?> wsmsg() {
        sysMsgProvider.getMsg(StpUtil.getLoginIdAsString(), true);
        return Result.success();
    }

    @GetMapping("/msg/data")
    @Operation(summary = "获取消息配置数据")
    @SaCheckLogin
    public Result<?> data() {
        NutMap map = NutMap.NEW();
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

    @PostMapping("/msg/my_msg_list")
    @Operation(summary = "查询消息列表")
    @SaCheckLogin
    public Result<?> myMsgList(
            @RequestBody(required = false) NutMap body,
            @Parameter(description = "读取状态") @RequestParam(required = false) String status,
            @Parameter(description = "消息类型") @RequestParam(required = false) String type,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        if (body != null) {
            status = Strings.sBlank(status, body.getString("status"));
            type = Strings.sBlank(type, body.getString("type"));
            pageOrderName = Strings.sBlank(pageOrderName, body.getString("pageOrderName"));
            pageOrderBy = Strings.sBlank(pageOrderBy, body.getString("pageOrderBy"));
            pageNo = body.getInt("pageNo", pageNo);
            pageSize = body.getInt("pageSize", pageSize);
        }
        String userId = StpUtil.getLoginIdAsString();
        StringBuilder query = new StringBuilder(" WHERE a.userId=@userId AND a.delFlag=@delFlag");
        StringBuilder order = new StringBuilder(" ORDER BY a.createdAt DESC");
        if (Strings.isNotBlank(status) && "read".equals(status)) {
            query.append(" AND a.status=@status");
        }
        if (Strings.isNotBlank(status) && "unread".equals(status)) {
            query.append(" AND a.status=@status");
        }
        if (Strings.isNotBlank(type) && !"all".equals(type)) {
            query.append(" AND b.type=@type");
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            order = new StringBuilder(" ORDER BY b." + pageOrderName + " " + PageUtil.getOrder(pageOrderBy));
        }
        Sql sql = Sqls.create("SELECT b.type,b.title,b.sendat,a.* FROM sys_msg b LEFT JOIN sys_msg_user a ON b.id=a.msgid $query $order");
        sql.vars().set("query", query.toString());
        sql.vars().set("order", order.toString());
        sql.params().set("userId", userId);
        sql.params().set("delFlag", false);
        if (Strings.isNotBlank(status) && ("read".equals(status) || "unread".equals(status))) {
            sql.params().set("status", "read".equals(status) ? 1 : 0);
        }
        if (Strings.isNotBlank(type) && !"all".equals(type)) {
            sql.params().set("type", type);
        }
        Sql sqlCount = Sqls.create("SELECT count(*) FROM sys_msg b LEFT JOIN sys_msg_user a ON b.id=a.msgid $query");
        sqlCount.vars().set("query", query.toString());
        sqlCount.params().set("userId", userId);
        sqlCount.params().set("delFlag", false);
        if (Strings.isNotBlank(status) && ("read".equals(status) || "unread".equals(status))) {
            sqlCount.params().set("status", "read".equals(status) ? 1 : 0);
        }
        if (Strings.isNotBlank(type) && !"all".equals(type)) {
            sqlCount.params().set("type", type);
        }
        return Result.data(sysMsgService.listPage(pageNo, pageSize, sql, sqlCount));
    }

    @PostMapping("/msg/status/read_more")
    @Operation(summary = "消息批量设置已读")
    @SaCheckPermission("home.msg.read")
    public Result<?> readMore(
            @RequestBody(required = false) NutMap body,
            @Parameter(description = "ID数组") @RequestParam(required = false) String[] ids) {
        String userId = StpUtil.getLoginIdAsString();
        sysMsgUserService.update(Chain.make("status", 1).add("readAt", Times.now().getTime())
                .add("updatedAt", Times.now().getTime()).add("updatedBy", userId), Cnd.where("id", "in", ids).and("userId", "=", userId).and("status", "=", 0));
        sysMsgProvider.getMsg(userId, false);
        return Result.success();
    }

    @PostMapping("/msg/status/read_all")
    @Operation(summary = "消息全部设置已读")
    @SaCheckPermission("home.msg.read")
    public Result<?> readAll() {
        String userId = StpUtil.getLoginIdAsString();
        sysMsgUserService.update(Chain.make("status", 1).add("readAt", Times.now().getTime())
                .add("updatedAt", Times.now().getTime()).add("updatedBy", userId), Cnd.where("userId", "=", userId).and("status", "=", 0));
        sysMsgProvider.getMsg(userId, false);
        return Result.success();
    }

    @PostMapping("/msg/get/{id}")
    @Operation(summary = "获取一条消息内容")
    @SaCheckLogin
    public Result<?> get(@Parameter(description = "消息ID") @PathVariable String id) {
        String userId = StpUtil.getLoginIdAsString();
        int num = sysMsgUserService.count(Cnd.where("msgid", "=", id).and("userId", "=", userId));
        if (num > 0) {
            return Result.data(sysMsgService.fetch(id));
        }
        return Result.error("没有权限");
    }

    @PostMapping("/msg/status/read_one/{id}")
    @Operation(summary = "设置一条消息已读")
    @SaCheckPermission("home.msg.read")
    public Result<?> readOne(@Parameter(description = "消息ID") @PathVariable String id) {
        String userId = StpUtil.getLoginIdAsString();
        sysMsgUserService.update(Chain.make("status", 1).add("readAt", Times.now().getTime())
                .add("updatedAt", Times.now().getTime()).add("updatedBy", userId), Cnd.where("msgid", "=", id).and("userId", "=", userId).and("status", "=", 0));
        sysMsgProvider.getMsg(userId, false);
        return Result.success();
    }

    @PostMapping("/user/avatar")
    @Operation(summary = "设置用户头像")
    @SaCheckPermission("home.user.update")
    public Result<?> setUserAvatar(
            @Parameter(description = "头像路径") @RequestParam String avatar) {
        String userId = StpUtil.getLoginIdAsString();
        String loginname = StpUtil.getSession().getString("loginname");
        String appId = StpUtil.getSession().getString("appId");
        if (GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME.equals(loginname) && sysConfigProvider.getBoolean(appId, "AppDemoEnv")) {
            return Result.error("演示环境禁止操作");
        }
        sysUserService.update(Chain.make("avatar", avatar), Cnd.where("id", "=", userId));
        sysUserService.cacheClear();
        return Result.success();
    }

    @PostMapping("/user/pwd")
    @Operation(summary = "修改用户密码")
    @SaCheckPermission("home.user.resetPwd")
    public Result<?> setUserPassword(
            @Parameter(description = "原密码") @RequestParam String oldPassword,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        String userId = StpUtil.getLoginIdAsString();
        String loginname = StpUtil.getSession().getString("loginname");
        String appId = StpUtil.getSession().getString("appId");
        if (GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME.equals(loginname) && sysConfigProvider.getBoolean(appId, "AppDemoEnv")) {
            return Result.error("演示环境禁止操作");
        }
        Sys_user user = sysUserService.fetch(userId);
        if (user == null) {
            return Result.error("数据不存在");
        }
        if (!user.getPassword().equals(PwdUtil.getPassword(oldPassword, user.getSalt()))) {
            return Result.error("原密码不正确");
        }
        sysUserService.resetPwd(user.getId(), newPassword, false);
        return Result.success();
    }

    @PostMapping("/user/info")
    @Operation(summary = "设置用户资料")
    @SaCheckPermission("home.user.update")
    public Result<?> setUserInfo(
            @Parameter(description = "姓名") @RequestParam String username,
            @Parameter(description = "EMail") @RequestParam(required = false) String email,
            @Parameter(description = "手机号码") @RequestParam(required = false) String mobile) {
        String userId = StpUtil.getLoginIdAsString();
        String loginname = StpUtil.getSession().getString("loginname");
        String appId = StpUtil.getSession().getString("appId");
        if (GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME.equals(loginname) && sysConfigProvider.getBoolean(appId, "AppDemoEnv")) {
            return Result.error("演示环境禁止操作");
        }
        if (Strings.isNotBlank(email)) {
            if (sysUserService.count(Cnd.where("email", "=", email).and("id", "<>", userId)) > 0) {
                return Result.error("邮箱地址已存在");
            }
        }
        if (Strings.isNotBlank(mobile)) {
            if (sysUserService.count(Cnd.where("mobile", "=", mobile).and("id", "<>", userId)) > 0) {
                return Result.error("手机号码已存在");
            }
        }
        sysUserService.update(Chain.make("username", username)
                        .add("email", email)
                        .add("mobile", mobile),
                Cnd.where("id", "=", userId));
        sysUserService.cacheClear();
        return Result.success();
    }

    @GetMapping("/user/get")
    @Operation(summary = "获取用户信息")
    @SaCheckLogin
    public Result<?> getUserInfo() {
        String userId = StpUtil.getLoginIdAsString();
        Sys_user user = sysUserService.fetch(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        sysUserService.fetchLinks(user, "^(unit|post|roles)$");
        return Result.success().addData(user);
    }

    @PostMapping("/user/log")
    @Operation(summary = "用户操作日志(最近2个月)")
    @SaCheckLogin
    public Result<?> getUserLog(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        return Result.data(sysLogService.getUserLogPage(StpUtil.getLoginIdAsString(), pageNo, pageSize, pageOrderName, pageOrderBy));
    }
}

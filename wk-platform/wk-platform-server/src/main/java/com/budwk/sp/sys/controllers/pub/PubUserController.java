package com.budwk.sp.sys.controllers.pub;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.sys.entity.Sys_unit;
import com.budwk.sp.sys.services.SysPostService;
import com.budwk.sp.sys.services.SysUnitService;
import com.budwk.sp.sys.services.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * 用户选择控制器
 *
 * @author wizzer.cn
 */
@RestController
@RequestMapping("/pub/user")
@Tag(name = "用户选择", description = "用户选择接口")
public class PubUserController {
    @Autowired
    private SysUnitService sysUnitService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysPostService sysPostService;

    @GetMapping("/unitlist")
    @Operation(summary = "获取单位列表")
    @SaCheckLogin
    public Result<?> getUnitList(
            @Parameter(description = "单位名称") @RequestParam(required = false) String name) {
        Cnd cnd = Cnd.NEW();
        if (!StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            Sys_unit unit = getCurrentUnit();
            if (unit == null || !Strings.isNotBlank(unit.getPath())) {
                return Result.data(Collections.emptyList());
            }
            cnd.and("path", "like", unit.getPath() + "%");
        }
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        cnd.asc("location");
        cnd.asc("path");
        return Result.data(sysUnitService.query(cnd));
    }

    @GetMapping("/post")
    @Operation(summary = "获取职务列表")
    @SaCheckLogin
    public Result<?> post() {
        return Result.data(sysPostService.query());
    }

    @PostMapping("/list")
    @Operation(summary = "获取用户列表")
    @SaCheckLogin
    public Result<?> list(
            @RequestBody(required = false) Map<String, Object> body,
            @Parameter(description = "已选用户名(英文,分隔)") @RequestParam(required = false) String users,
            @Parameter(description = "手机号码") @RequestParam(required = false) String mobile,
            @Parameter(description = "单位PATH") @RequestParam(required = false) String unitPath,
            @Parameter(description = "职务ID") @RequestParam(required = false) String postId,
            @Parameter(description = "用户姓名/用户名") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {

        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(unitPath)) {
            cnd.and("unitPath", "like", unitPath + "%");
        }
        if (!StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            Sys_unit unit = getCurrentUnit();
            if (unit == null || !Strings.isNotBlank(unit.getPath())) {
                return Result.data(sysUserService.listPageLinks(pageNo, pageSize, Cnd.where("id", "=", "__NO_USER__"), "^(unit)$"));
            }
            cnd.and("unitPath", "like", unit.getPath() + "%");
        }
        if (Strings.isNotBlank(postId)) {
            cnd.and("postId", "=", postId);
        }
        if (Strings.isNotBlank(users)) {
            String[] usersArray = Strings.splitIgnoreBlank(users, ",");
            if (usersArray.length > 0) {
                cnd.and("loginname", "not in", Arrays.asList(usersArray));
            }
        }
        if (Strings.isNotBlank(keyword)) {
            cnd.and(Cnd.exps("loginname", "like", "%" + keyword + "%").or("username", "like", "%" + keyword + "%"));
        }
        if (Strings.isNotBlank(mobile)) {
            cnd.and("mobile", "like", "%" + mobile + "%");
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(sysUserService.listPageLinks(pageNo, pageSize, cnd, "^(unit)$"));
    }

    private Sys_unit getCurrentUnit() {
        String unitId = StpUtil.getSession().getString("unitId");
        if (!StringUtils.hasText(unitId)) {
            return null;
        }
        return sysUnitService.fetch(unitId);
    }
}

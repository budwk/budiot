package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysUserSecurityDTO;
import com.budwk.sp.sys.entity.Sys_user_security;
import com.budwk.sp.sys.services.SysUserSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/security")
@SLog(tag = "账户安全")
@Tag(name = "账户安全", description = "账户安全配置接口")
public class SysSecurityController {

    @Autowired
    private SysUserSecurityService sysUserSecurityService;

    @PostMapping("/save")
    @Operation(summary = "保存账户安全配置")
    @RepeatSubmit
    @SaCheckPermission("sys.config.security.save")
    public Result<?> save(@RequestBody @Validated SysUserSecurityDTO dto) {
        Sys_user_security security = new Sys_user_security();
        BeanUtils.copyProperties(dto, security);
        security.setCreatedBy(StpUtil.getLoginIdAsString());
        security.setUpdatedBy(StpUtil.getLoginIdAsString());
        sysUserSecurityService.insertOrUpdate(security);
        return Result.success();
    }

    @GetMapping("/get")
    @Operation(summary = "获取账户安全配置")
    @SaCheckPermission("sys.config.security")
    public Result<?> getData() {
        return Result.data(sysUserSecurityService.fetch("MAIN"));
    }
}

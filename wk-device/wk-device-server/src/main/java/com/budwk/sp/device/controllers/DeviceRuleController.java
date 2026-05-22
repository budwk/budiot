package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceRuleDTO;
import com.budwk.sp.device.services.DeviceRuleService;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iot/rule")
@SLog(tag = "设备规则")
@Tag(name = "设备规则", description = "设备规则接口")
public class DeviceRuleController {
    private final DeviceRuleService deviceRuleService;

    public DeviceRuleController(DeviceRuleService deviceRuleService) {
        this.deviceRuleService = deviceRuleService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取规则基础数据")
    @SaCheckPermission("iot.manage.rule")
    public Result<?> data() {
        return Result.data(deviceRuleService.getBaseData(StpUtil.getSession().getString("tenantId")));
    }

    @GetMapping("/meta/{productId}")
    @Operation(summary = "获取产品规则元数据")
    @SaCheckPermission("iot.manage.rule")
    public Result<?> meta(@PathVariable String productId) {
        return Result.data(deviceRuleService.getProductMeta(productId, StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询规则")
    @SaCheckPermission("iot.manage.rule")
    public Result<?> list(@RequestParam(required = false) String name,
                          @RequestParam(required = false) String code,
                          @RequestParam(required = false) String triggerScene,
                          @RequestParam(required = false) String targetType,
                          @RequestParam(required = false) String sourceProductId,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false);
        if (Strings.isNotBlank(name)) cnd.and("name", "like", "%" + name + "%");
        if (Strings.isNotBlank(code)) cnd.and("code", "like", "%" + code + "%");
        if (Strings.isNotBlank(triggerScene)) cnd.and("triggerScene", "=", triggerScene);
        if (Strings.isNotBlank(targetType)) cnd.and("targetType", "=", targetType);
        if (Strings.isNotBlank(sourceProductId)) cnd.and("sourceProductId", "=", sourceProductId);
        if (disabled != null) cnd.and("disabled", "=", disabled);
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        else cnd.desc("updatedAt");
        return Result.data(deviceRuleService.listPage(pageNo, pageSize, cnd));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取规则详情")
    @SaCheckPermission("iot.manage.rule")
    public Result<?> get(@PathVariable String id) {
        return Result.data(deviceRuleService.getRule(id, StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/create")
    @Operation(summary = "新增规则")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.rule.create")
    public Result<?> create(@RequestBody @Validated DeviceRuleDTO dto) {
        return Result.data(deviceRuleService.createRule(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/update")
    @Operation(summary = "修改规则")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.rule.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) DeviceRuleDTO dto) {
        return Result.data(deviceRuleService.updateRule(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除规则")
    @SaCheckPermission("iot.manage.rule.delete")
    public Result<?> delete(@PathVariable String id) {
        deviceRuleService.deleteRule(id, StpUtil.getSession().getString("tenantId"));
        return Result.success();
    }
}

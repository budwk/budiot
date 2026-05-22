package com.budwk.sp.msg.controllers.msg;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.msg.dto.MsgTemplateDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.msg.services.MsgTemplateService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msg/template")
@SLog(tag = "消息模板")
@Tag(name = "消息模板", description = "消息模板配置接口")
public class MsgTemplateController {
    private final MsgTemplateService msgTemplateService;
    private final MsgChannelService msgChannelService;

    public MsgTemplateController(MsgTemplateService msgTemplateService, MsgChannelService msgChannelService) {
        this.msgTemplateService = msgTemplateService;
        this.msgChannelService = msgChannelService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取模板配置数据")
    @SaCheckPermission("msg.manage.template")
    public Result<?> data() {
        String tenantId = StpUtil.getSession().getString("tenantId");
        List<Msg_channel> channels = msgChannelService.getEnabledChannels(tenantId);
        return Result.data(Map.of(
                "bizTypes", List.of(MsgTemplateBizType.values()).stream().map(type -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("text", type.getText());
                    item.put("value", type.getValue());
                    return item;
                }).toList(),
                "channels", channels
        ));
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询模板")
    @SaCheckPermission("msg.manage.template")
    public Result<?> list(@RequestBody(required = false) Map<String, Object> body,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String bizType,
                          @RequestParam(required = false) String channelId,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = Cnd.where("tenantId", "=", StpUtil.getSession().getString("tenantId"));
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        if (Strings.isNotBlank(bizType)) {
            cnd.and("bizType", "=", bizType);
        }
        if (Strings.isNotBlank(channelId)) {
            cnd.and("channelId", "=", channelId);
        }
        if (disabled != null) {
            cnd.and("disabled", "=", disabled);
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        } else {
            cnd.desc("updatedAt");
        }
        return Result.data(msgTemplateService.listPageLinks(pageNo, pageSize, cnd, "channel"));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取模板详情")
    @SaCheckPermission("msg.manage.template")
    public Result<?> get(@PathVariable String id) {
        return Result.data(msgTemplateService.getTemplate(id, StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/create")
    @Operation(summary = "新增模板")
    @RepeatSubmit
    @SaCheckPermission("msg.manage.template.create")
    public Result<?> create(@RequestBody @Validated MsgTemplateDTO dto) {
        return Result.data(msgTemplateService.createTemplate(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/update")
    @Operation(summary = "修改模板")
    @RepeatSubmit
    @SaCheckPermission("msg.manage.template.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) MsgTemplateDTO dto) {
        return Result.data(msgTemplateService.updateTemplate(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除模板")
    @SaCheckPermission("msg.manage.template.delete")
    public Result<?> delete(@PathVariable String id) {
        msgTemplateService.deleteTemplate(id, StpUtil.getSession().getString("tenantId"));
        return Result.success();
    }
}

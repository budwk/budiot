package com.budwk.sp.msg.controllers.msg;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.msg.dto.MsgSendDTO;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.msg.services.MsgSendService;
import com.budwk.sp.msg.services.MsgTemplateService;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msg/send")
@SLog(tag = "消息发送")
@Tag(name = "消息发送", description = "消息发送接口")
public class MsgSendController {
    private final MsgSendService msgSendService;
    private final MsgChannelService msgChannelService;
    private final MsgTemplateService msgTemplateService;

    public MsgSendController(MsgSendService msgSendService, MsgChannelService msgChannelService, MsgTemplateService msgTemplateService) {
        this.msgSendService = msgSendService;
        this.msgChannelService = msgChannelService;
        this.msgTemplateService = msgTemplateService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取发送页面数据")
    @SaCheckPermission("msg.manage.send")
    public Result<?> data() {
        String tenantId = StpUtil.getSession().getString("tenantId");
        return Result.data(Map.of(
                "channels", msgChannelService.getEnabledChannels(tenantId),
                "templates", msgTemplateService.getEnabledTemplates(tenantId),
                "bizTypes", List.of(MsgTemplateBizType.values()).stream().map(type -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("text", type.getText());
                    item.put("value", type.getValue());
                    return item;
                }).toList())
        );
    }

    @PostMapping("/create")
    @Operation(summary = "发送消息")
    @RepeatSubmit
    @SaCheckPermission("msg.manage.send")
    public Result<?> create(@RequestBody @Valid MsgSendDTO dto) {
        return Result.data(msgSendService.send(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }
}

package com.budwk.sp.msg.controllers.msg;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.msg.dto.MsgHistoryQueryDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.enums.MsgSendStatus;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.msg.services.MsgHistoryService;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msg/history")
@SLog(tag = "消息历史")
@Tag(name = "消息历史", description = "消息发送历史接口")
public class MsgHistoryController {
    private final MsgHistoryService msgHistoryService;
    private final MsgChannelService msgChannelService;

    public MsgHistoryController(MsgHistoryService msgHistoryService, MsgChannelService msgChannelService) {
        this.msgHistoryService = msgHistoryService;
        this.msgChannelService = msgChannelService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取历史页面数据")
    @SaCheckPermission("msg.manage.history")
    public Result<?> data() {
        String tenantId = StpUtil.getSession().getString("tenantId");
        List<Msg_channel> channels = msgChannelService.getEnabledChannels(tenantId);
        return Result.data(Map.of(
                "channels", channels,
                "channelTypes", List.of(MsgChannelType.values()).stream().map(type -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("text", type.getText());
                    item.put("value", type.getValue());
                    return item;
                }).toList(),
                "providerTypes", List.of(MsgProviderType.values()).stream().map(type -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("text", type.getText());
                    item.put("value", type.getValue());
                    return item;
                }).toList(),
                "statuses", List.of(MsgSendStatus.values()).stream().map(type -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("text", type.getText());
                    item.put("value", type.getValue());
                    return item;
                }).toList()
        ));
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询发送历史")
    @SaCheckPermission("msg.manage.history")
    public Result<?> list(@RequestBody MsgHistoryQueryDTO dto) {
        return Result.data(msgHistoryService.listPage(StpUtil.getSession().getString("tenantId"), dto));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取发送历史详情")
    @SaCheckPermission("msg.manage.history")
    public Result<?> get(@PathVariable String id, @RequestParam Long createdAt) {
        return Result.data(msgHistoryService.fetchDetail(id, createdAt, StpUtil.getSession().getString("tenantId")));
    }
}

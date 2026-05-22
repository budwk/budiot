package com.budwk.sp.msg.controllers.msg;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.msg.dto.MsgChannelDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.enums.SysLogType;
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
@RequestMapping("/msg/channel")
@SLog(tag = "消息渠道")
@Tag(name = "消息渠道", description = "消息渠道配置接口")
public class MsgChannelController {
    private final MsgChannelService msgChannelService;

    public MsgChannelController(MsgChannelService msgChannelService) {
        this.msgChannelService = msgChannelService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取渠道配置数据")
    @SaCheckPermission("msg.manage.channel")
    public Result<?> data() {
        Map<String, Object> map = new HashMap<>();
        map.put("channelTypes", List.of(MsgChannelType.values()).stream().map(type -> {
            Map<String, String> item = new HashMap<>();
            item.put("text", type.getText());
            item.put("value", type.getValue());
            return item;
        }).toList());
        map.put("providerTypes", List.of(MsgProviderType.values()).stream().map(type -> {
            Map<String, String> item = new HashMap<>();
            item.put("text", type.getText());
            item.put("value", type.getValue());
            return item;
        }).toList());
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询消息渠道")
    @SaCheckPermission("msg.manage.channel")
    public Result<?> list(@RequestBody(required = false) Map<String, Object> body,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String channelType,
                          @RequestParam(required = false) String providerType,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = Cnd.where("tenantId", "=", StpUtil.getSession().getString("tenantId"));
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        if (Strings.isNotBlank(channelType)) {
            cnd.and("channelType", "=", channelType);
        }
        if (Strings.isNotBlank(providerType)) {
            cnd.and("providerType", "=", providerType);
        }
        if (disabled != null) {
            cnd.and("disabled", "=", disabled);
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        } else {
            cnd.desc("updatedAt");
        }
        return Result.data(msgChannelService.listPage(pageNo, pageSize, cnd));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取渠道详情")
    @SaCheckPermission("msg.manage.channel")
    public Result<?> get(@PathVariable String id) {
        return Result.data(msgChannelService.getChannel(id, StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/create")
    @Operation(summary = "新增渠道")
    @RepeatSubmit
    @SaCheckPermission("msg.manage.channel.create")
    public Result<?> create(@RequestBody @Validated MsgChannelDTO dto) {
        return Result.data(msgChannelService.createChannel(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/update")
    @Operation(summary = "修改渠道")
    @RepeatSubmit
    @SaCheckPermission("msg.manage.channel.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) MsgChannelDTO dto) {
        return Result.data(msgChannelService.updateChannel(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/default")
    @Operation(summary = "设为默认渠道")
    @SaCheckPermission("msg.manage.channel.update")
    public Result<?> setDefault(@RequestParam String id) {
        msgChannelService.setDefault(id, StpUtil.getSession().getString("tenantId"), StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除渠道")
    @SaCheckPermission("msg.manage.channel.delete")
    public Result<?> delete(@PathVariable String id) {
        msgChannelService.deleteChannel(id, StpUtil.getSession().getString("tenantId"));
        return Result.success();
    }
}

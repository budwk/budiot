package com.budwk.sp.sys.providers;

import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.sys.entity.Sys_msg;
import com.budwk.sp.sys.entity.Sys_msg_user;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.enums.SysMsgType;
import com.budwk.sp.sys.enums.SysMsgScope;
import com.budwk.sp.sys.services.SysMsgService;
import com.budwk.sp.sys.services.SysUserService;
import com.budwk.sp.sys.websocket.WkWebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wizzer@qq.com
 */
@DubboService(interfaceClass = ISysMsgProvider.class)
@Service
public class SysMsgProvider implements ISysMsgProvider {
    private static final DateTimeFormatter NOTICE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private WkWebSocketSessionRegistry sessionRegistry;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    @Lazy
    private SysMsgService sysMsgService;
    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Override
    public void wsSendMsg(String userId, String token, String msg) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(token) || !StringUtils.hasText(msg)) {
            return;
        }
        sessionRegistry.sendToUserToken(userId, token, msg);
    }

    @Override
    public void wsSendMsg(String userId, String msg) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(msg)) {
            return;
        }
        sessionRegistry.sendToUser(userId, msg);
    }

    @Override
    public void wsSendMsg(List<String> userId, String msg) {
        if (userId == null || userId.isEmpty() || !StringUtils.hasText(msg)) {
            return;
        }
        userId.stream().filter(StringUtils::hasText).distinct().forEach(id -> sessionRegistry.sendToUser(id, msg));
    }

    @Override
    public void wsCheckLogin(String userId, String token) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        sessionRegistry.sendOffline(userId, token, offlinePayload());
    }

    @Override
    public void sendMsg(String userId, SysMsgType type, String title, String url, String note, String sendUserId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        sendMsg(new String[]{userId}, type, title, url, note, sendUserId);
    }

    @Override
    public void sendMsg(String[] userId, SysMsgType type, String title, String url, String note, String sendUserId) {
        if (userId == null || userId.length == 0) {
            return;
        }
        Sys_msg msg = new Sys_msg();
        msg.setTenantId(resolveTenantId(sendUserId));
        msg.setType(type == null ? SysMsgType.SYSTEM : type);
        msg.setScope(SysMsgScope.SCOPE);
        msg.setTitle(title);
        msg.setUrl(url);
        msg.setNote(note);
        msg.setSendAt(System.currentTimeMillis());
        msg.setCreatedBy(StringUtils.hasText(sendUserId) ? sendUserId : "");
        msg.setUpdatedBy(msg.getCreatedBy());
        sysMsgService.saveMsg(msg, userId);
    }

    @Override
    public void getMsg(String userId, boolean notify) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        int size = sysMsgService.getUnreadNum(userId);
        List<Sys_msg_user> unreadList = sysMsgService.getUnreadList(userId, 1, 5);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Sys_msg_user item : unreadList) {
            if (item.getMsg() == null) {
                continue;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("msgId", item.getMsgId());
            data.put("title", item.getMsg().getTitle());
            data.put("time", formatTime(item.getMsg().getSendAt()));
            data.put("url", item.getMsg().getUrl());
            list.add(data);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "notice");
        payload.put("size", size);
        payload.put("notify", notify);
        payload.put("list", list);
        wsSendMsg(userId, toJson(payload));
    }

    private String resolveTenantId(String sendUserId) {
        if (StringUtils.hasText(sendUserId)) {
            Sys_user user = sysUserService.fetch(sendUserId);
            if (user != null && StringUtils.hasText(user.getTenantId())) {
                return user.getTenantId();
            }
        }
        return GlobalConstant.TENANT_ID_DEFAULT;
    }

    private String formatTime(Long sendAt) {
        if (sendAt == null) {
            return "";
        }
        return NOTICE_TIME_FORMATTER.format(Instant.ofEpochMilli(sendAt).atZone(ZoneId.systemDefault()));
    }

    private String offlinePayload() {
        return "{\"action\":\"offline\"}";
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("serialize websocket payload error", e);
        }
    }
}

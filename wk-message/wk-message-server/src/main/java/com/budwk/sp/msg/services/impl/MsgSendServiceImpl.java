package com.budwk.sp.msg.services.impl;

import com.budwk.sp.msg.dto.MsgSendDTO;
import com.budwk.sp.msg.dto.MsgSendResultDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.entity.Msg_history;
import com.budwk.sp.msg.entity.Msg_template;
import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgSendStatus;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.msg.sender.MsgResolvedReceiver;
import com.budwk.sp.msg.sender.MsgSenderContext;
import com.budwk.sp.msg.sender.MsgSenderFactory;
import com.budwk.sp.msg.sender.support.MsgContentHelper;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.msg.services.MsgHistoryService;
import com.budwk.sp.msg.services.MsgSendService;
import com.budwk.sp.msg.services.MsgTemplateService;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.providers.ISysUserProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MsgSendServiceImpl implements MsgSendService {
    private final MsgChannelService msgChannelService;
    private final MsgTemplateService msgTemplateService;
    private final MsgHistoryService msgHistoryService;
    private final MsgSenderFactory msgSenderFactory;
    private final ISysUserProvider sysUserProvider;
    private final ObjectMapper objectMapper;

    public MsgSendServiceImpl(MsgChannelService msgChannelService,
                              MsgTemplateService msgTemplateService,
                              MsgHistoryService msgHistoryService,
                              MsgSenderFactory msgSenderFactory,
                              ISysUserProvider sysUserProvider,
                              ObjectMapper objectMapper) {
        this.msgChannelService = msgChannelService;
        this.msgTemplateService = msgTemplateService;
        this.msgHistoryService = msgHistoryService;
        this.msgSenderFactory = msgSenderFactory;
        this.sysUserProvider = sysUserProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MsgSendResultDTO> send(MsgSendDTO dto, String operatorId, String tenantId) {
        String resolvedTenantId = Strings.sBlank(tenantId, Strings.sBlank(dto.getTenantId(), GlobalConstant.TENANT_ID_DEFAULT));
        Msg_channel channel = msgChannelService.getChannel(dto.getChannelId(), resolvedTenantId);
        if (channel.isDisabled()) {
            throw new BaseException("渠道已禁用");
        }
        Msg_template template = resolveTemplate(dto, resolvedTenantId, channel);
        Map<String, Object> params = resolveParams(template, dto.getParamsJson());
        String title = resolveTitle(template, dto, params);
        String content = resolveContent(template, dto, params);
        if (Strings.isBlank(content)) {
            throw new BaseException("发送内容不能为空");
        }
        List<MsgResolvedReceiver> receivers = resolveReceivers(channel, dto);
        if (receivers.isEmpty()) {
            throw new BaseException("没有可发送的接收地址");
        }
        MsgSenderContext context = MsgSenderContext.builder()
                .channel(channel)
                .template(template)
                .title(title)
                .content(content)
                .params(params)
                .mentions(resolveMentions(channel, dto))
                .build();
        List<MsgSendResultDTO> results = new ArrayList<>();
        for (MsgResolvedReceiver receiver : receivers) {
            MsgSendResultDTO result;
            try {
                result = msgSenderFactory.get(channel.getProviderType()).send(context, receiver);
            } catch (Exception e) {
                result = new MsgSendResultDTO();
                result.setSuccess(false);
                result.setReceiver(receiver.getReceiver());
                result.setMessage(e.getMessage());
                result.setCode("EXCEPTION");
                result.setSendAt(System.currentTimeMillis());
            }
            results.add(result);
            msgHistoryService.save(buildHistory(result, receiver, channel, template, dto, title, content, params, resolvedTenantId, operatorId));
        }
        return results;
    }

    private Msg_history buildHistory(MsgSendResultDTO result,
                                     MsgResolvedReceiver receiver,
                                     Msg_channel channel,
                                     Msg_template template,
                                     MsgSendDTO dto,
                                     String title,
                                     String content,
                                     Map<String, Object> params,
                                     String tenantId,
                                     String operatorId) {
        long now = result.getSendAt() == null ? System.currentTimeMillis() : result.getSendAt();
        Msg_history history = new Msg_history();
        history.setTenantId(tenantId);
        history.setRequestNo(UUID.randomUUID().toString().replace("-", ""));
        history.setChannelId(channel.getId());
        history.setTemplateId(template == null ? null : template.getId());
        history.setChannelType(channel.getChannelType());
        history.setProviderType(channel.getProviderType());
        history.setBizType(template != null ? template.getBizType() : dto.getBizType());
        history.setReceiver(receiver.getReceiver());
        history.setReceiverName(receiver.getReceiverName());
        history.setTitle(title);
        history.setContent(content);
        history.setParamsJson(writeJson(params));
        history.setStatus(result.isSuccess() ? MsgSendStatus.SUCCESS : MsgSendStatus.FAIL);
        history.setProviderCode(result.getCode());
        history.setProviderMsg(result.getMessage());
        history.setProviderRequestId(result.getRequestId());
        history.setSendAt(now);
        if (result.isSuccess()) {
            history.setSuccessAt(now);
        } else {
            history.setFailAt(now);
        }
        history.setCreatedAt(now);
        history.setUpdatedAt(now);
        history.setCreatedBy(operatorId);
        history.setUpdatedBy(operatorId);
        history.setDelFlag(false);
        return history;
    }

    private Msg_template resolveTemplate(MsgSendDTO dto, String tenantId, Msg_channel channel) {
        Msg_template template = null;
        if (Strings.isNotBlank(dto.getTemplateId())) {
            template = msgTemplateService.getTemplate(dto.getTemplateId(), tenantId);
            if (!channel.getId().equals(template.getChannelId())) {
                throw new BaseException("模板与渠道不匹配");
            }
        } else if (dto.getBizType() != null) {
            template = msgTemplateService.getDefaultTemplate(tenantId, channel.getId(), dto.getBizType());
        }
        if (template != null && template.isDisabled()) {
            throw new BaseException("模板已禁用");
        }
        return template;
    }

    private Map<String, Object> resolveParams(Msg_template template, String requestParamsJson) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.putAll(readJsonAsMap(template == null ? null : template.getParamsJson()));
        params.putAll(readJsonAsMap(requestParamsJson));
        return params;
    }

    private String resolveTitle(Msg_template template, MsgSendDTO dto, Map<String, Object> params) {
        String title = dto.getTitle();
        return MsgContentHelper.render(title, params);
    }

    private String resolveContent(Msg_template template, MsgSendDTO dto, Map<String, Object> params) {
        String content = template != null && Strings.isNotBlank(template.getContent()) ? template.getContent() : dto.getContent();
        return MsgContentHelper.render(content, params);
    }

    private List<MsgResolvedReceiver> resolveReceivers(Msg_channel channel, MsgSendDTO dto) {
        if (channel.getChannelType() == MsgChannelType.DINGTALK || channel.getChannelType() == MsgChannelType.WECOM) {
            String joined = String.join(",", resolveMentions(channel, dto));
            return List.of(new MsgResolvedReceiver(null, Strings.sBlank(joined, "webhook"), channel.getName()));
        }
        LinkedHashMap<String, MsgResolvedReceiver> map = new LinkedHashMap<>();
        if (dto.getUserIds() != null) {
            for (String userId : dto.getUserIds()) {
                if (Strings.isBlank(userId)) {
                    continue;
                }
                Sys_user user = sysUserProvider.getUserById(userId);
                if (user == null) {
                    continue;
                }
                String receiver = channel.getChannelType() == MsgChannelType.SMS ? user.getMobile() : user.getEmail();
                if (Strings.isBlank(receiver)) {
                    continue;
                }
                map.put(receiver, new MsgResolvedReceiver(userId, receiver, user.getUsername()));
            }
        }
        if (dto.getReceivers() != null) {
            for (String receiver : dto.getReceivers()) {
                if (Strings.isBlank(receiver)) {
                    continue;
                }
                map.put(receiver, new MsgResolvedReceiver(null, receiver.trim(), receiver.trim()));
            }
        }
        return new ArrayList<>(map.values());
    }

    private List<String> resolveMentions(Msg_channel channel, MsgSendDTO dto) {
        LinkedHashSet<String> mentions = new LinkedHashSet<>();
        if (dto.getUserIds() != null) {
            for (String userId : dto.getUserIds()) {
                if (Strings.isBlank(userId)) {
                    continue;
                }
                Sys_user user = sysUserProvider.getUserById(userId);
                if (user != null && Strings.isNotBlank(user.getMobile())) {
                    mentions.add(user.getMobile());
                }
            }
        }
        if (dto.getReceivers() != null) {
            for (String receiver : dto.getReceivers()) {
                if (Strings.isNotBlank(receiver)) {
                    mentions.add(receiver.trim());
                }
            }
        }
        return new ArrayList<>(mentions);
    }

    private Map<String, Object> readJsonAsMap(String json) {
        if (Strings.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BaseException("JSON参数格式不正确: " + e.getMessage());
        }
    }

    private String writeJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Collections.emptyMap() : data);
        } catch (Exception e) {
            throw new IllegalStateException("serialize params error", e);
        }
    }
}

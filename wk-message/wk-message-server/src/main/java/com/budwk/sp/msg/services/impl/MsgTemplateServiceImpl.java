package com.budwk.sp.msg.services.impl;

import com.budwk.sp.msg.dto.MsgTemplateDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.entity.Msg_template;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.msg.services.MsgTemplateService;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MsgTemplateServiceImpl extends BaseServiceImpl<Msg_template> implements MsgTemplateService {
    private final MsgChannelService msgChannelService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public MsgTemplateServiceImpl(Dao dao, MsgChannelService msgChannelService) {
        super(dao);
        this.msgChannelService = msgChannelService;
    }

    @Override
    public Msg_template createTemplate(MsgTemplateDTO dto, String operatorId, String tenantId) {
        Msg_channel channel = msgChannelService.getChannel(dto.getChannelId(), tenantId);
        Msg_template template = new Msg_template();
        template.setTenantId(tenantId);
        template.setChannelId(channel.getId());
        template.setChannelType(channel.getChannelType());
        template.setProviderType(channel.getProviderType());
        template.setBizType(dto.getBizType());
        template.setName(Strings.sNull(dto.getName()).trim());
        template.setTemplateCode(Strings.sNull(dto.getTemplateCode()).trim());
        template.setContent(dto.getContent());
        template.setParamsJson(dto.getParamsJson());
        template.setDisabled(dto.isDisabled());
        template.setCreatedBy(operatorId);
        template.setUpdatedBy(operatorId);
        this.insert(template);
        return template;
    }

    @Override
    public Msg_template updateTemplate(MsgTemplateDTO dto, String operatorId, String tenantId) {
        Msg_template template = getTemplate(dto.getId(), tenantId);
        Msg_channel channel = msgChannelService.getChannel(dto.getChannelId(), tenantId);
        template.setChannelId(channel.getId());
        template.setChannelType(channel.getChannelType());
        template.setProviderType(channel.getProviderType());
        template.setBizType(dto.getBizType());
        template.setName(Strings.sNull(dto.getName()).trim());
        template.setTemplateCode(Strings.sNull(dto.getTemplateCode()).trim());
        template.setContent(dto.getContent());
        template.setParamsJson(dto.getParamsJson());
        template.setDisabled(dto.isDisabled());
        template.setUpdatedBy(operatorId);
        this.updateIgnoreNull(template);
        return template;
    }

    @Override
    public void deleteTemplate(String id, String tenantId) {
        Msg_template template = getTemplate(id, tenantId);
        this.delete(template.getId());
    }

    @Override
    public Msg_template getTemplate(String id, String tenantId) {
        Msg_template template = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (template == null) {
            throw new BaseException("消息模板不存在");
        }
        return template;
    }

    @Override
    public List<Msg_template> getEnabledTemplates(String tenantId) {
        return this.query(Cnd.where("tenantId", "=", tenantId).and("disabled", "=", false).and("delFlag", "=", false).asc("createdAt"));
    }

    @Override
    public Msg_template getDefaultTemplate(String tenantId, String channelId, MsgTemplateBizType bizType) {
        return this.fetch(Cnd.where("tenantId", "=", tenantId)
                .and("channelId", "=", channelId)
                .and("bizType", "=", bizType)
                .and("disabled", "=", false)
                .and("delFlag", "=", false)
                .asc("createdAt"));
    }
}

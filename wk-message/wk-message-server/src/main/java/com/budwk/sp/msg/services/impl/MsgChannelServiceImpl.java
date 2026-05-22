package com.budwk.sp.msg.services.impl;

import com.budwk.sp.msg.dto.MsgChannelDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MsgChannelServiceImpl extends BaseServiceImpl<Msg_channel> implements MsgChannelService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public MsgChannelServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    public Msg_channel createChannel(MsgChannelDTO dto, String operatorId, String tenantId) {
        String resolvedTenantId = Strings.sBlank(tenantId, dto.getTenantId());
        checkCodeUnique(null, resolvedTenantId, dto.getCode());
        Msg_channel channel = new Msg_channel();
        channel.setTenantId(resolvedTenantId);
        channel.setName(Strings.sNull(dto.getName()).trim());
        channel.setCode(Strings.sNull(dto.getCode()).trim());
        channel.setChannelType(dto.getChannelType());
        channel.setProviderType(dto.getProviderType());
        channel.setConfigJson(dto.getConfigJson());
        channel.setDefaultFlag(dto.isDefaultFlag());
        channel.setDisabled(dto.isDisabled());
        channel.setCreatedBy(operatorId);
        channel.setUpdatedBy(operatorId);
        this.insert(channel);
        if (channel.isDefaultFlag()) {
            clearOthersDefault(channel.getId(), resolvedTenantId, channel.getChannelType());
        }
        return channel;
    }

    @Override
    public Msg_channel updateChannel(MsgChannelDTO dto, String operatorId, String tenantId) {
        Msg_channel channel = getChannel(dto.getId(), tenantId);
        checkCodeUnique(channel.getId(), channel.getTenantId(), dto.getCode());
        channel.setName(Strings.sNull(dto.getName()).trim());
        channel.setCode(Strings.sNull(dto.getCode()).trim());
        channel.setChannelType(dto.getChannelType());
        channel.setProviderType(dto.getProviderType());
        channel.setConfigJson(dto.getConfigJson());
        channel.setDefaultFlag(dto.isDefaultFlag());
        channel.setDisabled(dto.isDisabled());
        channel.setUpdatedBy(operatorId);
        this.updateIgnoreNull(channel);
        if (channel.isDefaultFlag()) {
            clearOthersDefault(channel.getId(), channel.getTenantId(), channel.getChannelType());
        }
        return channel;
    }

    @Override
    public void deleteChannel(String id, String tenantId) {
        Msg_channel channel = getChannel(id, tenantId);
        this.delete(channel.getId());
    }

    @Override
    public Msg_channel getChannel(String id, String tenantId) {
        Msg_channel channel = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (channel == null) {
            throw new BaseException("消息渠道不存在");
        }
        return channel;
    }

    @Override
    public void setDefault(String id, String tenantId, String operatorId) {
        Msg_channel channel = getChannel(id, tenantId);
        this.update(Chain.make("defaultFlag", true).add("updatedBy", operatorId), Cnd.where("id", "=", id));
        clearOthersDefault(id, tenantId, channel.getChannelType());
    }

    @Override
    public List<Msg_channel> getEnabledChannels(String tenantId) {
        return this.query(Cnd.where("tenantId", "=", tenantId).and("disabled", "=", false).and("delFlag", "=", false).asc("createdAt"));
    }

    @Override
    public Msg_channel getDefaultChannel(String tenantId, String channelType) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("disabled", "=", false).and("delFlag", "=", false);
        if (Strings.isNotBlank(channelType)) {
            cnd.and("channelType", "=", MsgChannelType.valueOf(channelType));
        }
        Msg_channel channel = this.fetch(cnd.and("defaultFlag", "=", true));
        if (channel != null) {
            return channel;
        }
        cnd = Cnd.where("tenantId", "=", tenantId).and("disabled", "=", false).and("delFlag", "=", false);
        if (Strings.isNotBlank(channelType)) {
            cnd.and("channelType", "=", MsgChannelType.valueOf(channelType));
        }
        return this.fetch(cnd.asc("createdAt"));
    }

    private void checkCodeUnique(String id, String tenantId, String code) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("code", "=", Strings.sNull(code).trim()).and("delFlag", "=", false);
        if (Strings.isNotBlank(id)) {
            cnd.and("id", "<>", id);
        }
        if (this.count(cnd) > 0) {
            throw new BaseException("渠道编码已存在");
        }
    }

    private void clearOthersDefault(String currentId, String tenantId, MsgChannelType channelType) {
        this.update(Chain.make("defaultFlag", false), Cnd.where("tenantId", "=", tenantId)
                .and("channelType", "=", channelType)
                .and("id", "<>", currentId));
    }
}

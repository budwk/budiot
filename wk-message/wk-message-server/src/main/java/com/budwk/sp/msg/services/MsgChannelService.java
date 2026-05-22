package com.budwk.sp.msg.services;

import com.budwk.sp.msg.dto.MsgChannelDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;

public interface MsgChannelService extends BaseService<Msg_channel> {
    Msg_channel createChannel(MsgChannelDTO dto, String operatorId, String tenantId);

    Msg_channel updateChannel(MsgChannelDTO dto, String operatorId, String tenantId);

    void deleteChannel(String id, String tenantId);

    Msg_channel getChannel(String id, String tenantId);

    void setDefault(String id, String tenantId, String operatorId);

    List<Msg_channel> getEnabledChannels(String tenantId);

    Msg_channel getDefaultChannel(String tenantId, String channelType);
}

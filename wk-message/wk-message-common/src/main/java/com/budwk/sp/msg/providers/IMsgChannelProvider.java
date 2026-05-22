package com.budwk.sp.msg.providers;

import com.budwk.sp.msg.entity.Msg_channel;

import java.util.List;

public interface IMsgChannelProvider {
    List<Msg_channel> getEnabledChannels(String tenantId);

    Msg_channel getDefaultChannel(String tenantId, String channelType);
}

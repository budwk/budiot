package com.budwk.sp.msg.providers;

import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.services.MsgChannelService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

@DubboService(interfaceClass = IMsgChannelProvider.class)
@Service
public class MsgChannelProvider implements IMsgChannelProvider {
    private final MsgChannelService msgChannelService;

    public MsgChannelProvider(MsgChannelService msgChannelService) {
        this.msgChannelService = msgChannelService;
    }

    @Override
    public List<Msg_channel> getEnabledChannels(String tenantId) {
        return msgChannelService.getEnabledChannels(tenantId);
    }

    @Override
    public Msg_channel getDefaultChannel(String tenantId, String channelType) {
        return msgChannelService.getDefaultChannel(tenantId, channelType);
    }
}

package com.budwk.sp.msg.sender;

import com.budwk.sp.msg.enums.MsgProviderType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MsgSenderFactory {
    private final List<MsgChannelSender> senders;

    public MsgSenderFactory(List<MsgChannelSender> senders) {
        this.senders = senders;
    }

    public MsgChannelSender get(MsgProviderType providerType) {
        return senders.stream()
                .filter(sender -> sender.supports(providerType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的发送渠道: " + providerType));
    }
}

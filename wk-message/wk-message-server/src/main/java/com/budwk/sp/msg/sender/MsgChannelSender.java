package com.budwk.sp.msg.sender;

import com.budwk.sp.msg.dto.MsgSendResultDTO;
import com.budwk.sp.msg.enums.MsgProviderType;

public interface MsgChannelSender {
    boolean supports(MsgProviderType providerType);

    MsgSendResultDTO send(MsgSenderContext context, MsgResolvedReceiver receiver);
}

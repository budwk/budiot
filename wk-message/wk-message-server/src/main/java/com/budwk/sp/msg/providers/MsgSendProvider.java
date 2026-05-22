package com.budwk.sp.msg.providers;

import com.budwk.sp.msg.dto.MsgSendDTO;
import com.budwk.sp.msg.dto.MsgSendResultDTO;
import com.budwk.sp.msg.services.MsgSendService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

@DubboService(interfaceClass = IMsgSendProvider.class)
@Service
public class MsgSendProvider implements IMsgSendProvider {
    private final MsgSendService msgSendService;

    public MsgSendProvider(MsgSendService msgSendService) {
        this.msgSendService = msgSendService;
    }

    @Override
    public List<MsgSendResultDTO> send(MsgSendDTO dto, String operatorId, String tenantId) {
        return msgSendService.send(dto, operatorId, tenantId);
    }
}

package com.budwk.sp.msg.providers;

import com.budwk.sp.msg.dto.MsgSendDTO;
import com.budwk.sp.msg.dto.MsgSendResultDTO;

import java.util.List;

public interface IMsgSendProvider {
    List<MsgSendResultDTO> send(MsgSendDTO dto, String operatorId, String tenantId);
}

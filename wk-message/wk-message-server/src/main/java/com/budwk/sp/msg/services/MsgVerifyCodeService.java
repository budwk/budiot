package com.budwk.sp.msg.services;

import com.budwk.sp.msg.dto.MsgVerifyCodeCheckDTO;
import com.budwk.sp.msg.dto.MsgVerifyCodeSendDTO;

public interface MsgVerifyCodeService {
    void sendSmsCode(MsgVerifyCodeSendDTO dto);

    void sendEmailCode(MsgVerifyCodeSendDTO dto);

    void checkCode(MsgVerifyCodeCheckDTO dto);
}

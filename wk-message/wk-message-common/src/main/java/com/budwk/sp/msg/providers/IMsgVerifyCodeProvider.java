package com.budwk.sp.msg.providers;

import com.budwk.sp.msg.dto.MsgVerifyCodeCheckDTO;
import com.budwk.sp.msg.dto.MsgVerifyCodeSendDTO;

public interface IMsgVerifyCodeProvider {
    void sendSmsCode(MsgVerifyCodeSendDTO dto);

    void sendEmailCode(MsgVerifyCodeSendDTO dto);

    void checkCode(MsgVerifyCodeCheckDTO dto);
}

package com.budwk.sp.msg.providers;

import com.budwk.sp.msg.dto.MsgVerifyCodeCheckDTO;
import com.budwk.sp.msg.dto.MsgVerifyCodeSendDTO;
import com.budwk.sp.msg.services.MsgVerifyCodeService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

@DubboService(interfaceClass = IMsgVerifyCodeProvider.class)
@Service
public class MsgVerifyCodeProvider implements IMsgVerifyCodeProvider {
    private final MsgVerifyCodeService msgVerifyCodeService;

    public MsgVerifyCodeProvider(MsgVerifyCodeService msgVerifyCodeService) {
        this.msgVerifyCodeService = msgVerifyCodeService;
    }

    @Override
    public void sendSmsCode(MsgVerifyCodeSendDTO dto) {
        msgVerifyCodeService.sendSmsCode(dto);
    }

    @Override
    public void sendEmailCode(MsgVerifyCodeSendDTO dto) {
        msgVerifyCodeService.sendEmailCode(dto);
    }

    @Override
    public void checkCode(MsgVerifyCodeCheckDTO dto) {
        msgVerifyCodeService.checkCode(dto);
    }
}

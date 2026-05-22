package com.budwk.sp.msg.controllers.pub;

import com.budwk.sp.msg.dto.MsgVerifyCodeCheckDTO;
import com.budwk.sp.msg.dto.MsgVerifyCodeSendDTO;
import com.budwk.sp.msg.services.MsgVerifyCodeService;
import com.budwk.sp.starter.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/msg/pub")
@Tag(name = "消息公开接口", description = "验证码等公开接口")
public class MsgPubController {
    private final MsgVerifyCodeService msgVerifyCodeService;

    public MsgPubController(MsgVerifyCodeService msgVerifyCodeService) {
        this.msgVerifyCodeService = msgVerifyCodeService;
    }

    @PostMapping("/sms/code/send")
    @Operation(summary = "发送短信验证码")
    public Result<?> sendSmsCode(@RequestBody @Valid MsgVerifyCodeSendDTO dto) {
        msgVerifyCodeService.sendSmsCode(dto);
        return Result.success();
    }

    @PostMapping("/email/code/send")
    @Operation(summary = "发送邮箱验证码")
    public Result<?> sendEmailCode(@RequestBody @Valid MsgVerifyCodeSendDTO dto) {
        msgVerifyCodeService.sendEmailCode(dto);
        return Result.success();
    }

    @PostMapping("/code/check")
    @Operation(summary = "校验验证码")
    public Result<?> checkCode(@RequestBody @Valid MsgVerifyCodeCheckDTO dto) {
        msgVerifyCodeService.checkCode(dto);
        return Result.success();
    }
}

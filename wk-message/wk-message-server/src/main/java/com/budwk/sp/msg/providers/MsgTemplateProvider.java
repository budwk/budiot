package com.budwk.sp.msg.providers;

import com.budwk.sp.msg.entity.Msg_template;
import com.budwk.sp.msg.services.MsgTemplateService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

@DubboService(interfaceClass = IMsgTemplateProvider.class)
@Service
public class MsgTemplateProvider implements IMsgTemplateProvider {
    private final MsgTemplateService msgTemplateService;

    public MsgTemplateProvider(MsgTemplateService msgTemplateService) {
        this.msgTemplateService = msgTemplateService;
    }

    @Override
    public List<Msg_template> getEnabledTemplates(String tenantId) {
        return msgTemplateService.getEnabledTemplates(tenantId);
    }

    @Override
    public Msg_template getTemplate(String id, String tenantId) {
        return msgTemplateService.getTemplate(id, tenantId);
    }
}

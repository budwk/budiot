package com.budwk.sp.msg.providers;

import com.budwk.sp.msg.entity.Msg_template;

import java.util.List;

public interface IMsgTemplateProvider {
    List<Msg_template> getEnabledTemplates(String tenantId);

    Msg_template getTemplate(String id, String tenantId);
}

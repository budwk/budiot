package com.budwk.sp.msg.services;

import com.budwk.sp.msg.dto.MsgTemplateDTO;
import com.budwk.sp.msg.entity.Msg_template;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;

public interface MsgTemplateService extends BaseService<Msg_template> {
    Msg_template createTemplate(MsgTemplateDTO dto, String operatorId, String tenantId);

    Msg_template updateTemplate(MsgTemplateDTO dto, String operatorId, String tenantId);

    void deleteTemplate(String id, String tenantId);

    Msg_template getTemplate(String id, String tenantId);

    List<Msg_template> getEnabledTemplates(String tenantId);

    Msg_template getDefaultTemplate(String tenantId, String channelId, MsgTemplateBizType bizType);
}

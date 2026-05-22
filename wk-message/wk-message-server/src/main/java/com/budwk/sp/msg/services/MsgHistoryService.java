package com.budwk.sp.msg.services;

import com.budwk.sp.msg.dto.MsgHistoryQueryDTO;
import com.budwk.sp.msg.entity.Msg_history;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.database.service.BaseService;

public interface MsgHistoryService extends BaseService<Msg_history> {
    void save(Msg_history history);

    Pagination listPage(String tenantId, MsgHistoryQueryDTO dto);

    Msg_history fetchDetail(String id, Long createdAt, String tenantId);
}

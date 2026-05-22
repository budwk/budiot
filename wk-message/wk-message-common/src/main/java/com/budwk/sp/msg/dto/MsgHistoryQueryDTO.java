package com.budwk.sp.msg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "消息历史查询DTO")
public class MsgHistoryQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String channelId;
    private String channelType;
    private String providerType;
    private String status;
    private String receiver;
    private String title;
    private Long beginTime;
    private Long endTime;
    private int pageNo = 1;
    private int pageSize = 10;
    private String pageOrderName = "createdAt";
    private String pageOrderBy = "descending";
}

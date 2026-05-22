package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "协议脚本缓存刷新消息")
public class DeviceScriptRefreshMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tenantId;
    private String protocolId;
    private String scriptVersion;
    private String action;
}

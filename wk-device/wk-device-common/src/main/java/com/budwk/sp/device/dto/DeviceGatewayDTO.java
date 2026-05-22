package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceGatewayMode;
import com.budwk.sp.device.enums.DeviceGatewayStatus;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备网关DTO")
public class DeviceGatewayDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;

    @NotBlank(message = "网关名称不能为空")
    @Size(max = 120, message = "网关名称长度不能超过120")
    private String name;

    @NotNull(message = "网络协议不能为空")
    private DeviceNetworkProtocol networkProtocol;

    @NotNull(message = "网关模式不能为空")
    private DeviceGatewayMode gatewayMode;

    @NotBlank(message = "设备协议不能为空")
    private String protocolId;

    private String host;
    private Integer port;
    private String path;
    private String remoteHost;
    private Integer remotePort;
    private String clientId;
    private String username;
    private String password;
    private String subscribeTopic;
    private String publishTopic;
    private boolean allowAnonymous = true;
    private boolean autoStart;
    private boolean disabled;

    @Size(max = 255, message = "说明长度不能超过255")
    private String description;

    private DeviceGatewayStatus runtimeStatus;
}

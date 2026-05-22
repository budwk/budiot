package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.enums.DeviceProductType;
import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备产品DTO")
public class DeviceProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;
    private String tenantId;

    @NotBlank(message = "ProductKey不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9]{1,31}$", message = "ProductKey只支持2-32位小写字母和数字，且必须以小写字母开头")
    private String productKey;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 120, message = "产品名称长度不能超过120")
    private String name;

    private String categoryId;
    private String vendorId;

    @NotNull(message = "产品类型不能为空")
    private DeviceProductType productType;

    @NotNull(message = "网络协议不能为空")
    private DeviceNetworkProtocol networkProtocol;

    private String protocolId;
    private String gatewayNodeId;
    private Integer gatewayPort;

    @Size(max = 255, message = "说明长度不能超过255")
    private String description;
    private boolean disabled;
}

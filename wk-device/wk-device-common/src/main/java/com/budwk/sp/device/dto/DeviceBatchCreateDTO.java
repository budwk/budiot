package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量创建设备DTO")
public class DeviceBatchCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tenantId;

    @NotBlank(message = "所属产品不能为空")
    private String productId;

    @NotEmpty(message = "设备编号列表不能为空")
    private List<String> deviceCodes;

    private boolean disabled;
}

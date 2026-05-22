package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备产品物模型DTO")
public class DeviceProductThingModelDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "产品ID不能为空")
    private String id;

    private String thingPropertyJson;
    private String thingServiceJson;
    private String thingEventJson;
}

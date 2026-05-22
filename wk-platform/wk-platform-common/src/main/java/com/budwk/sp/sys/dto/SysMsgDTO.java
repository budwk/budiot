package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.sys.enums.SysMsgScope;
import com.budwk.sp.sys.enums.SysMsgType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
/**
 * 站内消息 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "站内消息传输对象")
public class SysMsgDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "消息类型")
    private SysMsgType type;

    @Schema(description = "发送范围")
    private SysMsgScope scope;

    @NotBlank(message = "消息标题不能为空")
    @Size(max = 255, message = "消息标题长度不能超过255")
    @Schema(description = "消息标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 500, message = "消息内容长度不能超过500")
    @Schema(description = "消息内容")
    private String note;

    @Size(max = 255, message = "跳转链接长度不能超过255")
    @Schema(description = "跳转链接")
    private String url;

    @Schema(description = "发送时间")
    private Long sendAt;

    @Schema(description = "指定用户ID数组")
    private String[] users;
}

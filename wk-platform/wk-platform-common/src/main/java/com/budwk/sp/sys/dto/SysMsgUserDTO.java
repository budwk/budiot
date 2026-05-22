package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
/**
 * 系统消息接收 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "系统消息接收传输对象")
public class SysMsgUserDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "消息ID")
    private String msgId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String loginname;

    @Schema(description = "姓名")
    private String username;

    @Schema(description = "消息状态: 0-未读, 1-已读")
    private int status;

    @Schema(description = "读取时间")
    private Long readAt;
}

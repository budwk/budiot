package com.budwk.app.access.protocol.device;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 指令信息（下发给设备）
 *
 * @author wizzer.cn
 */
@Data
public class CommandInfo implements Serializable {
    private static final long serialVersionUID = 8423308482003927423L;
    /**
     * 指令标识
     */
    private String commandCode;
    /**
     * 设备ID
     */
    private String deviceId;
    /**
     * 指令ID
     */
    private String commandId;
    /**
     * 指令序号
     */
    private Integer commandSerialNo;
    /**
     * 参数
     */
    private Map<String, Object> params;
}

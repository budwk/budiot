package com.budwk.app.access.protocol.codec.result;

import com.budwk.app.access.protocol.message.DeviceMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 默认解码结果
 *
 * @author wizzer.cn
 * @author zyang
 */
@Data
@NoArgsConstructor
public class DefaultDecodeResult implements DecodeResult {

    private static final long serialVersionUID = 1379437915859363399L;
    /**
     * 设备Id
     */
    private String deviceId;
    /**
     * 解码消息
     */
    private List<DeviceMessage> messages;

    public DefaultDecodeResult(String deviceId, List<DeviceMessage> messages) {
        this.deviceId = deviceId;
        this.messages = messages;
    }
}

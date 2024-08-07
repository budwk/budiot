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

    /**
     * 是否最后一帧
     */
    private boolean isLastFrame;

    /**
     * 报文含义（读数上报、开关阀上报、异常上报等）
     */
    private String meaning;

    public DefaultDecodeResult(String deviceId, List<DeviceMessage> messages) {
        this.deviceId = deviceId;
        this.messages = messages;
        this.isLastFrame = true;
    }

    public DefaultDecodeResult(String deviceId, List<DeviceMessage> messages, boolean isLastFrame) {
        this.deviceId = deviceId;
        this.messages = messages;
        this.isLastFrame = isLastFrame;
    }

    public DefaultDecodeResult(String deviceId, List<DeviceMessage> messages, boolean isLastFrame, String meaning) {
        this.deviceId = deviceId;
        this.messages = messages;
        this.isLastFrame = isLastFrame;
        this.meaning = meaning;
    }
}

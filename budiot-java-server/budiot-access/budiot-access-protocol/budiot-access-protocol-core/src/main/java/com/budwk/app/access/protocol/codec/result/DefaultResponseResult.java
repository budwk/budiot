package com.budwk.app.access.protocol.codec.result;

import com.budwk.app.access.protocol.message.DeviceMessage;
import com.budwk.app.access.protocol.message.DeviceResponseMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 指令回复结果
 *
 * @author wizzer.cn
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefaultResponseResult implements DecodeResult {
    private static final long serialVersionUID = -3532992412392691183L;
    /**
     * 指令ID
     */
    private String commandId;
    /**
     * 设备id。注意，可能为空
     */
    private String deviceId;
    /**
     * 执行的指令方法
     */
    private String commandCode;
    /**
     * 指令是否成功执行
     */
    private boolean success;

    /**
     * 响应结果消息
     */
    private List<DeviceMessage> messages;

    /**
     * 设备协议标识
     */
    private String protocolCode;

    private boolean isLastFrame;

    /**
     * 报文含义（读数上报、开关阀上报、异常上报等）
     */
    private String meaning;

    public DefaultResponseResult(String commandId, DeviceResponseMessage responseMessage) {
        this.commandId = commandId;
        this.success = responseMessage.isSuccess();
        this.commandCode = responseMessage.getCommandCode();
        this.deviceId = responseMessage.getDeviceId();
        this.messages = Arrays.asList(responseMessage);
        this.isLastFrame = true;
    }

    public DefaultResponseResult(String commandId, DeviceResponseMessage responseMessage,String meaning) {
        this.commandId = commandId;
        this.success = responseMessage.isSuccess();
        this.commandCode = responseMessage.getCommandCode();
        this.deviceId = responseMessage.getDeviceId();
        this.messages = Arrays.asList(responseMessage);
        this.isLastFrame = true;
        this.meaning = meaning;
    }

    public DefaultResponseResult(String commandId, String deviceId, String commandCode, boolean success, List<DeviceMessage> messages) {
        this.commandId = commandId;
        this.deviceId = deviceId;
        this.commandCode = commandCode;
        this.success = success;
        this.messages = messages;
        this.isLastFrame = true;
    }

    public DefaultResponseResult(String commandId, String deviceId, String commandCode, boolean success, List<DeviceMessage> messages,boolean isLastFrame) {
        this.commandId = commandId;
        this.deviceId = deviceId;
        this.commandCode = commandCode;
        this.success = success;
        this.messages = messages;
        this.isLastFrame = isLastFrame;
    }

    public DefaultResponseResult(String commandId, String deviceId, String commandCode, boolean success, List<DeviceMessage> messages,boolean isLastFrame,String meaning) {
        this.commandId = commandId;
        this.deviceId = deviceId;
        this.commandCode = commandCode;
        this.success = success;
        this.messages = messages;
        this.isLastFrame = isLastFrame;
        this.meaning = meaning;
    }
}

package com.budwk.app.access.protocol.codec.impl;

import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.message.DeviceMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据上报解析结果
 * @author wizzer.cn
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DecodeReportResult implements DecodeResult {
    private static final long serialVersionUID = 6100256851048650501L;
    /**
     * 设备Id
     */
    private String deviceId;
    /**
     * 解码消息
     */
    private List<DeviceMessage> messages;

    /**
     * 协议code
     */
    private String protocolCode;

    /**
     * 是否最后一帧
     */
    private boolean isLastFrame;

    public DecodeReportResult(String deviceId, List<DeviceMessage> messages) {
        this.deviceId = deviceId;
        this.messages = messages;
    }
}

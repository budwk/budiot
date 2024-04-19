package com.budwk.app.access.protocol.demo.codec;

import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.access.protocol.message.DeviceDataMessage;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class DecodeProcessor {
    private final DecodeContext context;
    private final EncodedMessage message;


    public DecodeProcessor(DecodeContext context) {
        this.context = context;
        this.message = context.getMessage();
    }

    public DecodeResult process() {
        byte[] bytes = message.getPayload();
        DeviceOperator deviceOperator = context.getDeviceByNo("demo2");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("data", new String(bytes));
        DeviceDataMessage deviceDataMessage = new DeviceDataMessage();
        deviceDataMessage.setDeviceId(deviceOperator.getDeviceId());
        deviceDataMessage.setTimestamp(System.currentTimeMillis());
        deviceDataMessage.setProperties(properties);
        this.sendToDevice("hi!".getBytes());
        return new DefaultDecodeResult(deviceOperator.getDeviceId(), Arrays.asList(deviceDataMessage));

    }

    public void sendToDevice(byte[] replyBytes) {
        context.send(new TcpMessage(replyBytes, message.getProtocolCode()));
    }

}
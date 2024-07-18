package com.budwk.app.access.protocol.demo.codec;

import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.access.protocol.message.DeviceDataMessage;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import lombok.extern.slf4j.Slf4j;
import org.nutz.json.Json;
import org.nutz.lang.util.NutMap;

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
        String json = new String(bytes);
        NutMap map = Json.fromJson(NutMap.class,json);
        //设备传输数据格式演示 {"valve_status":0,"flow":2.3,"temp":23.5}
        deviceDataMessage.addProperty("valve_status", map.getInt("valve_status"));
        deviceDataMessage.addProperty("flow", map.getDouble("flow"));
        deviceDataMessage.addProperty("temp", map.getDouble("temp"));
        //回复消息（指令）=》发送给设备
        this.sendToDevice("hi!".getBytes());
        return new DefaultDecodeResult(deviceOperator.getDeviceId(), Arrays.asList(deviceDataMessage));

    }

    public void sendToDevice(byte[] replyBytes) {
        context.send(new TcpMessage(replyBytes, message.getProtocolCode()));
    }

}
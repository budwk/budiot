package com.budwk.app.access.protocol.demo.codec;

import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.access.protocol.message.DeviceDataMessage;
import com.budwk.app.access.protocol.message.DeviceEventMessage;
import com.budwk.app.access.protocol.message.DeviceMessage;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import lombok.extern.slf4j.Slf4j;
import org.nutz.json.Json;
import org.nutz.lang.util.NutMap;

import java.util.*;

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
        // 模拟设备上报数据
        DeviceDataMessage deviceDataMessage = new DeviceDataMessage();
        deviceDataMessage.setDeviceId(deviceOperator.getDeviceId());
        deviceDataMessage.setTimestamp(System.currentTimeMillis());
        deviceDataMessage.setProperties(properties);
        String json = new String(bytes);
        NutMap map = Json.fromJson(NutMap.class,json);
        // 数据格式演示 {"valve_status":0,"flow":2.3,"temp":23.5}
        deviceDataMessage.addProperty("valve_status", map.getInt("valve_status"));
        deviceDataMessage.addProperty("flow", map.getDouble("flow"));
        deviceDataMessage.addProperty("temp", map.getDouble("temp"));
        // 模拟事件告警
        DeviceEventMessage deviceEventMessage = new DeviceEventMessage();
        deviceEventMessage.setDeviceId(deviceOperator.getDeviceId());
        deviceEventMessage.setTimestamp(System.currentTimeMillis());
        deviceEventMessage.setEventType(DeviceEventMessage.Type.ALARM.name());
        deviceEventMessage.setEventName("电池电压异常");
        deviceEventMessage.setContent("电压低于3.5V产生的告警");
        deviceEventMessage.setWarningValue("3.5V");
        deviceEventMessage.addProperty("voltage","电压",3.5);
        deviceEventMessage.addProperty("electrical","电量",80.5);
        deviceEventMessage.addProperty("signal_strength","信号强度","弱");

        List<DeviceMessage> messageList=new ArrayList<>();
        messageList.add(deviceDataMessage);
        messageList.add(deviceEventMessage);

        //回复消息（指令）=》发送给设备
        this.sendToDevice("hi!".getBytes());
        return new DefaultDecodeResult(deviceOperator.getDeviceId(), messageList);

    }

    public void sendToDevice(byte[] replyBytes) {
        context.send(new TcpMessage(replyBytes, message.getProtocolCode()));
    }

}
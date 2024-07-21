package com.budwk.app.access.protocol.demo.codec;

import com.budwk.app.access.protocol.codec.CacheStore;
import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultResponseResult;
import com.budwk.app.access.protocol.demo.DemoProtocol;
import com.budwk.app.access.protocol.message.DeviceDataMessage;
import com.budwk.app.access.protocol.message.DeviceEventMessage;
import com.budwk.app.access.protocol.message.DeviceMessage;
import com.budwk.app.access.protocol.message.DeviceResponseMessage;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import lombok.extern.slf4j.Slf4j;
import org.nutz.json.Json;
import org.nutz.lang.util.NutMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

        String json = new String(bytes);
        NutMap map = Json.fromJson(NutMap.class, json);
        // 数据格式演示
        // 1、数据上报 and 事件 {"functionCode":"up_data","deviceNo":"demo2","valve_status":0,"flow":2.3,"temp":23.5}
        // 2、指令回复 {"functionCode":"resp_cmd","deviceNo":"demo2","valve_status":1,"flow":0,"temp":22.5}

        String functionCode = map.getString("functionCode"); //功能码
        String deviceNo = map.getString("deviceNo"); //设备通信号
        DeviceOperator deviceOperator = context.getDeviceByNo(deviceNo);

        switch (functionCode) {
            case "up_data"://数据上报

                Map<String, Object> properties = new LinkedHashMap<>();
                properties.put("data", new String(bytes));
                // 模拟设备上报数据
                DeviceDataMessage deviceDataMessage = new DeviceDataMessage();
                deviceDataMessage.setDeviceId(deviceOperator.getDeviceId());
                deviceDataMessage.setTimestamp(System.currentTimeMillis());
                deviceDataMessage.setProperties(properties);
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
                deviceEventMessage.addProperty("voltage", "电压", 3.5);
                deviceEventMessage.addProperty("electrical", "电量", 80.5);
                deviceEventMessage.addProperty("signal_strength", "信号强度", "弱");

                List<DeviceMessage> messageList = new ArrayList<>();
                messageList.add(deviceDataMessage);
                messageList.add(deviceEventMessage);

                //回复消息（指令）=》发送给设备
                this.sendToDevice("hi!".getBytes());
                return new DefaultDecodeResult(deviceOperator.getDeviceId(), messageList);
            case "resp_cmd"://指令回复
                String cmd = "VALVE_CONTROL";
                DeviceResponseMessage responseMessage = new DeviceResponseMessage();
                responseMessage.setDeviceId(deviceOperator.getDeviceId());
                responseMessage.setCommandCode(cmd);
                responseMessage.setSuccess(true);
                responseMessage.addProperty("flow", map.getDouble("flow"));
                responseMessage.addProperty("temp", map.getDouble("temp"));
                responseMessage.addProperty("electrical",  80.5);
                responseMessage.addProperty("signal_strength",  "弱");
                CacheStore cacheStore = context.getCacheStore(DemoProtocol.PROTOCOL_CODE + ":" + deviceOperator.getDeviceId());
                String commandId = cacheStore.get(DemoProtocol.PROTOCOL_CODE + ":CMD_SEND_ID:" + cmd, String.class);
                DefaultResponseResult cmdRespResult = new DefaultResponseResult(commandId, responseMessage);
                cmdRespResult.setCommandCode(cmd);
                return cmdRespResult;
        }
        return null;
    }

    public void sendToDevice(byte[] replyBytes) {
        context.send(new TcpMessage(replyBytes, message.getProtocolCode()));
    }

}
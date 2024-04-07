package com.budwk.app.access.constants;

public class TopicConstant {
    // 消息上行队列topic
    public static final String DEVICE_DATA_UP = "device.data.up";
    // 指令下行队列topic
    public static final String DEVICE_CMD_DOWN = "device.cmd.down";
    // 指令触发
    public static final String DEVICE_CMD_TRIGGER = "device.cmd.trigger";
    // 指令回复topic
    public static final String DEVICE_CMD_RESP = "device.cmd.resp";
    // 业务处理器topic
    public static final String SERVICE_PROCESSOR = "device.data.processor";
    // 设备告警topic
    public static final String SERVICE_NOTIFY_ALARM = "device.data.alarm";
}

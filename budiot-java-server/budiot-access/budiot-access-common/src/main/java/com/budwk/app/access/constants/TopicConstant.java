package com.budwk.app.access.constants;

public class TopicConstant {
    // 消息上行队列topic
    public static final String DEVICE_DATA_UP = "device_data_up";
    // 指令下行队列topic
    public static final String DEVICE_CMD_DOWN = "device_cmd_down";
    // 设备事件topic
    public static final String DEVICE_EVENT = "device_event";
    // 指令发送topic
    public static final String DEVICE_CMD_TRIGGER = "device_cmd_trigger";
    // 业务处理器topic
    public static final String DEVICE_DATA_PROCESSOR = "device_data_processor";
    // 设备告警topic
    public static final String DEVICE_DATA_ALARM = "device_data_alarm";
}

package com.budwk.sp.device.message.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DeviceMessageTopics {
    private DeviceMessageTopics() {}

    /**
     * 网关收到设备原始上行报文后投递到该主题，供协议解析器消费。
     */
    public static String rawUplink(String prefix) { return prefix + ".uplink.raw"; }

    /**
     * 处理器将原始报文解析为统一结构后投递到该主题，供后续存储和规则链路使用。
     */
    public static String normalizedUplink(String prefix) { return prefix + ".uplink.normalized"; }

    /**
     * 管理端或处理器生成的设备下行指令投递到该主题，由网关消费后实际下发到设备。
     */
    public static String downlinkCommand(String prefix) { return prefix + ".downlink.command"; }

    /**
     * 设备事件、告警和规则触发事件统一投递到该主题，供事件中心和告警中心消费。
     */
    public static String event(String prefix) { return prefix + ".event"; }

    /**
     * 规则引擎向外部平台做转发时使用该主题，承载二次分发消息。
     */
    public static String ruleForward(String prefix) { return prefix + ".rule.forward"; }

    /**
     * 网关集群广播控制消息使用该主题，例如节点刷新、会话同步、配置通知等。
     */
    public static String gatewayBroadcast(String prefix) { return prefix + ".gateway.broadcast"; }

    public static Map<String, String> standardTopics(String prefix) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("gatewayRawUplink", rawUplink(prefix));
        map.put("handlerNormalizedUplink", normalizedUplink(prefix));
        map.put("gatewayDownlinkCommand", downlinkCommand(prefix));
        map.put("handlerEvent", event(prefix));
        map.put("ruleForward", ruleForward(prefix));
        map.put("gatewayBroadcast", gatewayBroadcast(prefix));
        return map;
    }
}

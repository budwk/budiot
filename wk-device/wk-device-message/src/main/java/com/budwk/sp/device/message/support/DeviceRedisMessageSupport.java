package com.budwk.sp.device.message.support;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageScene;
import com.budwk.sp.device.message.config.DeviceMessageProperties;
import org.nutz.lang.Strings;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DeviceRedisMessageSupport {
    private DeviceRedisMessageSupport() {
    }

    public static String streamKey(DeviceMessageProperties properties, String topic) {
        return properties.getRedisStreamPrefix() + topic;
    }

    public static String channel(DeviceMessageProperties properties, String topic) {
        return properties.getRedisChannelPrefix() + topic;
    }

    public static String channelPattern(DeviceMessageProperties properties, String topic) {
        return channel(properties, topic) + "*";
    }

    public static String channel(DeviceMessageProperties properties, String topic, String routingKey) {
        String base = channel(properties, topic);
        return Strings.isBlank(routingKey) ? base : base + ":" + routingKey;
    }

    public static boolean usePubSub(DeviceMessageEnvelope<?> envelope) {
        if (envelope == null) {
            return false;
        }
        if (envelope.getPattern() == DeviceMessagePattern.BROADCAST) {
            return true;
        }
        return envelope.getScene() == DeviceMessageScene.COMMAND_DOWNLINK || envelope.getScene() == DeviceMessageScene.GATEWAY_CONTROL;
    }

    public static Map<String, String> streamPayload(DeviceMessageEnvelope<?> envelope, String payloadJson) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("messageId", Strings.sNull(envelope.getMessageId()).trim());
        map.put("scene", envelope.getScene() == null ? "" : envelope.getScene().getValue());
        map.put("pattern", envelope.getPattern() == null ? "" : envelope.getPattern().getValue());
        map.put("topic", Strings.sNull(envelope.getTopic()).trim());
        map.put("routingKey", Strings.sNull(envelope.getRoutingKey()).trim());
        map.put("payload", Strings.sNull(payloadJson).trim());
        return map;
    }

    public static void ensureGroup(StringRedisTemplate redisTemplate, String streamKey, String group) {
        StreamOperations<String, Object, Object> ops = redisTemplate.opsForStream();
        if (Boolean.FALSE.equals(redisTemplate.hasKey(streamKey))) {
            Map<String, String> init = new LinkedHashMap<>();
            init.put("_init", "1");
            ops.add(MapRecord.create(streamKey, init));
        }
        try {
            ops.createGroup(streamKey, ReadOffset.latest(), group);
        } catch (Exception ignored) {
        }
    }
}

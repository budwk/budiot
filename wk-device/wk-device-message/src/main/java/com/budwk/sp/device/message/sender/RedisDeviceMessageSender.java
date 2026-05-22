package com.budwk.sp.device.message.sender;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceMessageProviderType;
import com.budwk.sp.device.message.config.DeviceMessageProperties;
import com.budwk.sp.device.message.support.DeviceRedisMessageSupport;
import com.budwk.sp.starter.common.exception.BaseException;
import org.nutz.lang.Strings;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisDeviceMessageSender implements DeviceMessageSender {
    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceMessageProperties properties;

    public RedisDeviceMessageSender(StringRedisTemplate stringRedisTemplate, DeviceMessageProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    @Override
    public DeviceMessageProviderType provider() {
        return DeviceMessageProviderType.REDIS;
    }

    @Override
    public void send(DeviceMessageEnvelope<?> envelope, String payloadJson) {
        if (stringRedisTemplate == null) {
            throw new BaseException("RedisTemplate 未配置");
        }
        try {
            if (DeviceRedisMessageSupport.usePubSub(envelope)) {
                String channel = envelope.getPattern() == com.budwk.sp.device.enums.DeviceMessagePattern.BROADCAST
                        ? DeviceRedisMessageSupport.channel(properties, envelope.getTopic())
                        : DeviceRedisMessageSupport.channel(properties, envelope.getTopic(), Strings.sBlank(envelope.getRoutingKey(), envelope.getDeviceCode()));
                stringRedisTemplate.convertAndSend(channel, payloadJson);
            }
            if (envelope.getScene() != com.budwk.sp.device.enums.DeviceMessageScene.GATEWAY_CONTROL) {
                String streamKey = DeviceRedisMessageSupport.streamKey(properties, envelope.getTopic());
                stringRedisTemplate.opsForStream().add(MapRecord.create(streamKey, DeviceRedisMessageSupport.streamPayload(envelope, payloadJson)));
            }
        } catch (Exception e) {
            throw new BaseException(Strings.sBlank(e.getMessage(), "Redis 设备消息发送失败"));
        }
    }
}

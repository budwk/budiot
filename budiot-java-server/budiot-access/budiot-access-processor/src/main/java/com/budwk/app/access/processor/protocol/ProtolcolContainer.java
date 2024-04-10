package com.budwk.app.access.processor.protocol;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.objects.dto.DeviceRawDataDTO;
import com.budwk.app.access.processor.timer.DelayTaskHelper;
import com.budwk.app.access.protocol.codec.MessageCodec;
import com.budwk.app.access.protocol.codec.Protocol;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.castor.Castors;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.util.NutMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

@Slf4j
@IocBean(create = "init")
public class ProtolcolContainer {
    /**
     * 存放会话ID-设备映射
     */
    private RMapCache<String, String> sessionDevice;

    /**
     * 存放设备ID-会话信息映射
     */
    private RMapCache<String, NutMap> deviceSessionCache;

    /**
     * 缓存
     */
    private RMapCache<String, Object> cacheMap;
    @Inject
    private LocalProtocolLoader protocolLoader;
    @Inject
    private RedissonClient redissonClient;
    @Inject
    private DelayTaskHelper delayTaskHelper;
    @Inject
    private MessageTransfer messageTransfer;

    public void init() {
        cacheMap = redissonClient.getMapCache("protocol_container");
        deviceSessionCache = redissonClient.getMapCache("device_addr");
        sessionDevice = redissonClient.getMapCache("device_session");
    }

    public void start() {
        listenData();
        //listenCmd();
        //listenCmdResp();
    }

    public void listenData() {
        log.debug("listenData");
        messageTransfer.subscribe("DEVICE_DATA_UP", TopicConstant.DEVICE_DATA_UP, this::handleDataUp);
    }

    private void handleDataUp(Message<?> message) {
        log.info("收到消息 {}", message.toString());
        Object body = message.getBody();
        EncodedMessage encodedMessage = Castors.me().castTo(body, EncodedMessage.class);
        log.info("encodedMessage {}", encodedMessage.payloadAsString());
        log.info("getProtocolCode {}", encodedMessage.getProtocolCode());

        DeviceRawDataDTO rawData = new DeviceRawDataDTO();
        rawData.setType("U");
        rawData.setOriginData(ByteConvertUtil.bytesToHex(encodedMessage.getPayload()));
        rawData.setStartTime(System.currentTimeMillis());
        try {
            // 加载编解码类
            MessageCodec messageCodec = getMessageCodec(encodedMessage.getProtocolCode(), encodedMessage.getTransportType());
            log.debug(messageCodec.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MessageCodec getMessageCodec(String protocolCode, TransportType transportType) {
        Protocol protocol = protocolLoader.loadProtocols(protocolCode);
        return protocol.getMessageCodec(transportType);
    }
}

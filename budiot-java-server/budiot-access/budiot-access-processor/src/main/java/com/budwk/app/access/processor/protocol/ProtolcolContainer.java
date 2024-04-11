package com.budwk.app.access.processor.protocol;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.objects.dto.DeviceRawDataDTO;
import com.budwk.app.access.processor.cache.RedissionCacheStore;
import com.budwk.app.access.processor.protocol.impl.DefaultDecodeContext;
import com.budwk.app.access.processor.timer.DelayTaskHelper;
import com.budwk.app.access.protocol.codec.*;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import com.budwk.app.access.storage.DeviceRawDataStorage;
import lombok.extern.slf4j.Slf4j;
import org.nutz.castor.Castors;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
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
    @Inject
    private DeviceRawDataStorage deviceRawDataStorage;
    @Inject
    private DeviceRegistry deviceRegistry;

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
        String protocolCode = encodedMessage.getProtocolCode();

        DeviceRawDataDTO rawData = new DeviceRawDataDTO();
        rawData.setType("U");
        rawData.setOriginData(ByteConvertUtil.bytesToHex(encodedMessage.getPayload()));
        rawData.setStartTime(System.currentTimeMillis());
        try {
            // 加载编解码类
            MessageCodec messageCodec = getMessageCodec(protocolCode, encodedMessage.getTransportType());
            if (null == messageCodec) {
                return;
            }
            String sessionId = message.getHeader("sessionId");
            // 从缓存中尝试获取设备
            String sessionDeviceId = Strings.isNotBlank(sessionId) ? sessionDevice.get(sessionId) : null;
            log.debug("缓存中设备 sessionDeviceId===>{}", sessionDeviceId);
            DeviceOperator deviceOperator = null;
            if (protocolCode.contains("COLLECTOR")) {
                // 采集器/集中器设备
            } else {
                deviceOperator = Strings.isNotBlank(sessionDeviceId) && !"null".equals(sessionDeviceId) ? deviceRegistry.getDeviceOperator("id", sessionDeviceId) : null;
            }
            // 构造解码上下文
            DefaultDecodeContext decodeContext = new DefaultDecodeContext(protocolCode, deviceRegistry, encodedMessage) {
                /**
                 * 提供方法可以让解析包中直接发送消息给设备
                 * @param replyMessage 需要发送的消息
                 */
                @Override
                public void send(EncodedMessage replyMessage) {
                    if (null == replyMessage) {
                        return;
                    }
                    DeviceRawDataDTO rawData = new DeviceRawDataDTO();
                    DeviceOperator device = this.getDevice();
                    rawData.setDeviceId(null != device ? device.getDeviceId() : null);
                    rawData.setType("D");
                    rawData.setOriginData(replyMessage.payloadAsString());
                    rawData.setStartTime(System.currentTimeMillis());
                    if (!replyMessage.isSend()) { // 指令回复
                        Message<EncodedMessage> reply = new Message<>(message.getFrom(), replyMessage);
                        reply.setFrom(TopicConstant.DEVICE_CMD_RESP);
                        reply.getHeaders().putAll(message.getHeaders());
                        messageTransfer.publish(reply);
                    }
                    rawData.setEndTime(System.currentTimeMillis());
                    deviceRawDataStorage.save(rawData);
                }

                /**
                 * 提供缓存实现
                 * @param id 缓存存储的id
                 * @return
                 */
                @Override
                public CacheStore getCacheStore(String id) {
                    return new RedissionCacheStore(id, redissonClient);
                }
            };
            // deviceOperator 重新赋值
            if (null != decodeContext.getDevice()) {
                deviceOperator = decodeContext.getDevice();
            } else if (null != decodeContext.getDeviceById(sessionId)) {
                deviceOperator = decodeContext.getDeviceById(sessionId);
            } else {
                if (null != deviceOperator) {
                    decodeContext.setDevice(deviceOperator);
                }
            }
            // 解析数据
            DecodeResult result = messageCodec.decode(decodeContext);
            log.info("解析数据: {}", result);
            if (null == result) {
                log.warn("数据解析失败 {}", encodedMessage.getProtocolCode());
                // 存储上报的原始数据
                rawData.setDeviceId(null != deviceOperator ? deviceOperator.getDeviceId() : null);
                rawData.setEndTime(System.currentTimeMillis());
                deviceRawDataStorage.save(rawData);
                return;
            }
            if (null == decodeContext.getDevice()) {
                DeviceOperator deviceOperatorTmp = decodeContext.getDeviceById(result.getDeviceId());
                if (null != deviceOperatorTmp) {
                    deviceOperator = deviceOperatorTmp;
                    decodeContext.setDevice(deviceOperatorTmp);
                }
            }
            if (null != decodeContext.getDevice()) {
                if (Strings.isNotBlank(sessionId)) {
                    sessionDevice.put(sessionId, decodeContext.getDevice().getDeviceId());
                }
                // 同时更新状态为在线以及最近通讯时间
                deviceRegistry.updateDeviceOnline(decodeContext.getDevice().getDeviceId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MessageCodec getMessageCodec(String protocolCode, TransportType transportType) {
        Protocol protocol = protocolLoader.loadProtocols(protocolCode);
        return protocol.getMessageCodec(transportType);
    }
}

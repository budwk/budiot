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
import com.budwk.app.access.protocol.codec.context.EncodeContext;
import com.budwk.app.access.protocol.codec.impl.DecodeCmdRespResult;
import com.budwk.app.access.protocol.codec.impl.DecodeReportResult;
import com.budwk.app.access.protocol.codec.impl.GatewayDeviceOperatorImpl;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.EncodeResult;
import com.budwk.app.access.protocol.device.CommandInfo;
import com.budwk.app.access.protocol.message.DeviceMessage;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import com.budwk.app.access.storage.DeviceRawDataStorage;
import lombok.extern.slf4j.Slf4j;
import org.nutz.castor.Castors;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
                deviceOperator = Strings.isNotBlank(sessionDeviceId) && !"null".equals(sessionDeviceId) ? deviceRegistry.getGatewayDevice("id", sessionDeviceId) : null;

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
                GatewayDeviceOperatorImpl deviceOperatorTmp = (GatewayDeviceOperatorImpl) decodeContext.getGatewayDeviceById(result.getDeviceId());
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
            // 处理数据上报
            if (result instanceof DecodeReportResult) {
                DecodeReportResult reportResult = (DecodeReportResult) result;
                String deviceId = reportResult.getDeviceId();
                // 保存一下信息，用于发送指令时的处理
                deviceSessionCache.put(deviceId,
                        NutMap.NEW()
                                .setv("address", message.getFrom())
                                .setv("sessionId", message.getHeader("sessionId"))
                                .setv("transportType", encodedMessage.getTransportType().name())
                                .setv("protocolCode", protocolCode), 600, TimeUnit.SECONDS);
                // 存储上报的原始数据
                rawData.setDeviceId(deviceId);
                rawData.setParsedData(Json.toJson(reportResult.getMessages(), JsonFormat.tidy()));
                rawData.setEndTime(System.currentTimeMillis());
                deviceRawDataStorage.save(rawData);
                if (Lang.isNotEmpty(reportResult.getMessages())) {
                    // 将解析后的数据推送到业务处理模块
                    Message<DecodeReportResult> processorMessage = new Message<>(TopicConstant.SERVICE_PROCESSOR, reportResult);
                    processorMessage.setSender("protocol");
                    processorMessage.addHeader("protocolCode", protocolCode);
                    processorMessage.addHeader("deviceType", deviceOperator.getProduct().getDeviceType());
                    if (deviceOperator instanceof GatewayDeviceOperatorImpl) {
                        // 集中器或采集器等网关设备
                        List<DeviceMessage> messages = reportResult.getMessages();
                        // 如果指令响应中存在业务处理需要的数据，那么也就推送到业务处理去
                        if (Lang.isNotEmpty(messages)) {
                            Iterator<DeviceMessage> iterator = messages.iterator();
                            while (iterator.hasNext()) {
                                DeviceMessage deviceMessage = iterator.next();
                                // 同时更新集中器设备状态为在线以及最近通讯时间
                                log.info("更新集中器 {} 设备状态为在线以及最近通讯时间", deviceMessage.getDeviceId());
                                deviceRegistry.updateDeviceOnline(deviceMessage.getDeviceId());
                            }
                            if (Lang.isNotEmpty(messages)) {
                                // 将解析后的数据推送到业务处理模块
                                Message<DecodeReportResult> processorMessage2 = getSubDeviceMessage(protocolCode, deviceId, messages, deviceOperator);
                                // 将解析后的数据推送到业务处理去
                                messageTransfer.publish(processorMessage2);
                            }
                        }
                    } else {
                        // 将解析后的数据推送到业务处理去
                        messageTransfer.publish(processorMessage);
                    }
                }
                if (reportResult.isLastFrame()) {
                    this.sendCacheCommand(decodeContext.getDevice());
                }
            }
            // 处理指令回复的数据
            else {
                DecodeCmdRespResult cmdRespResult = (DecodeCmdRespResult) result;
                this.processCmdResp(decodeContext.getDevice(), cmdRespResult);
                // 存储上报的原始数据
                rawData.setDeviceId(null != decodeContext.getDevice() ? decodeContext.getDevice().getDeviceId() : null);
                rawData.setParsedData(Json.toJson(cmdRespResult.getMessages(), JsonFormat.tidy()));
                rawData.setEndTime(System.currentTimeMillis());
                deviceRawDataStorage.save(rawData);
            }
            // 刷新设备信息
            deviceOperatorFlushSupport.flush(decodeContext.getDevice());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送待下发指令
     *
     * @param deviceOperator
     */
    private void sendCacheCommand(DeviceOperator deviceOperator) {
        log.debug("获取待下发指令");
        if (null == deviceOperator) {
            return;
        }
        // 获取待下发指令
        CommandInfo deviceCommand = deviceRegistry.getDeviceCommand(deviceOperator.getDeviceId());
        if (null == deviceCommand) {
            // 下发一个结束指令
            delayTaskHelper.delayRun(() -> {
                noMoreCommand(deviceOperator);
            }, 5);
            return;
        }
        // 构造指令
        this.buildCommand(deviceCommand);
    }

    /**
     * 最好回复指令
     * @param deviceOperator
     */
    private void noMoreCommand(DeviceOperator deviceOperator) {
        CommandInfo waitCommand = deviceRegistry.getDeviceCommand(deviceOperator.getDeviceId());
        if (null != waitCommand) {
            this.buildCommand(waitCommand);
            return;
        }
        // 优化以下代码
        CommandInfo end = new CommandInfo();
        end.setDeviceId(deviceOperator.getDeviceId());
        end.setCommandCode("END");
        end.setCommandId("END");
        this.buildCommand(end);
    }

    /**
     * 构造指令
     * @param commandInfo
     */
    private void buildCommand(CommandInfo commandInfo) {
        try {
            if (null == commandInfo) {
                return;
            }
            // 获取设备会话，如果未找到说明设备不在线，那就不继续发了
            String deviceId = commandInfo.getDeviceId();
            NutMap session = deviceSessionCache.get(deviceId);
            DeviceOperator device = null;
            if (null == session) {
                device = deviceRegistry.getDeviceOperator(deviceId);
                if (Lang.isNotEmpty(device) && Lang.isNotEmpty(device.getProduct()) && device.getProperty("protocolCode").equals("LORA")) {
                    session = NutMap.NEW().setv("address", getReplyAddress()).
                            setv("protocolCode", device.getProperty("protocolCode")).
                            setv("transportType", device.getProduct().getProtocolType());
                } else {
                    log.warn("未找到设备会话");
                    return;
                }
            }
            // 从缓存中获取从网关带过来的会话信息
            String sessionId = session.getString("sessionId");
            String address = session.getString("address");
            String protocolCode = session.getString("protocolCode");
            String transportType = session.getString("transportType");

            // 存下发送的指令ID，防止重复下发
            String key;
            if (protocolCode.equals("LORA")) {
                key = "cmd:send:" + commandInfo.getDeviceId() + ":" + commandInfo.getCommandCode();
            } else {
                key = "cmd:send:" + commandInfo.getDeviceId();
            }
            if (Strings.isBlank(commandInfo.getCommandId())) {
                commandInfo.setCommandId(commandInfo.getDeviceId() + "_" + commandInfo.getCommandCode());
            }
            cacheMap.put(key, commandInfo.getCommandId(), 120, TimeUnit.SECONDS);

            DeviceRawDataDTO rawData = new DeviceRawDataDTO();
            rawData.setType("D");
            rawData.setDeviceId(deviceId);
            rawData.setParsedData(Json.toJson(commandInfo, JsonFormat.tidy()));
            rawData.setStartTime(System.currentTimeMillis());
            // 根据网络协议获取编解码类
            MessageCodec messageCodec = getMessageCodec(protocolCode, TransportType.valueOf(transportType.toUpperCase()));
            // 获取设备信息如果已经上报过数据，那么这里可以直接从缓存获取到
            if (Strings.isNotBlank(sessionId)) {
                if (protocolCode.equals("qjCollector")) {
                    String sessionDeviceId = Strings.isNotBlank(sessionId) ? sessionDevice.get(sessionId) : null;
                    log.debug("缓存中设备sessionDeviceId===>{}", sessionDeviceId);
                    device = Strings.isNotBlank(sessionDeviceId) && !"null".equals(sessionDeviceId) ? deviceRegistry.getGatewayDevice("id", sessionDeviceId) : null;
                }
            } else {
                if (protocolCode.equals("LORA")) {
                    sessionId = (String) Objects.requireNonNull(device).getProperty("meterNo");
                }
            }
            if (null == device) {
                device = deviceRegistry.getDeviceOperator("id", deviceId);
            }
            // 构造编码上下文
            EncodeContext encodeCtx = new DefaultEncodeContext(deviceRegistry, commandInfo, device) {
                @Override
                public CacheStore getCacheStore(String id) {
                    // 使用 Redisson的缓存
                    return new RedissionCacheStore(id, redissonClient);
                }
            };
            // 编码指令数据
            EncodeResult encode = messageCodec.encode(encodeCtx);

            if (null != encode) {
                String finalSessionId = sessionId;
                encode.getMessage().forEach(message -> {
                    // 如果需要发送那就发送指令到 网关
                    if (!encode.isSend()) {
                        Message<EncodedMessage> request = new Message<>(address, message);
                        request.addHeader("sessionId", finalSessionId);
                        request.addHeader("commandId", commandInfo.getCommandId());
                        request.addHeader("deviceId", deviceId);
                        request.setFrom(TopicConstant.DEVICE_CMD_RESP);
                        messageTransfer.publish(request);
                    } else {
                        deviceRegistry.makeCommandSend(commandInfo.getCommandId());
                    }
                    // 存储下发的指令原始数据
                    rawData.setOriginData(message.payloadAsString());
                    rawData.setEndTime(System.currentTimeMillis());
                    deviceRawDataStorage.save(rawData);
                });
            }
        } catch (Exception e) {
            log.warn("构造指令失败", e);
        }
    }

    private MessageCodec getMessageCodec(String protocolCode, TransportType transportType) {
        Protocol protocol = protocolLoader.loadProtocols(protocolCode);
        return protocol.getMessageCodec(transportType);
    }

    private static Message<DecodeReportResult> getSubDeviceMessage(String deviceId, String protocolCode, List<DeviceMessage> deviceMessage, DeviceOperator deviceOperator) {
        Message<DecodeReportResult> processorMessage =
                new Message<>(TopicConstant.SERVICE_PROCESSOR, new DecodeReportResult(deviceId, deviceMessage));
        processorMessage.setSender("protocol");
        processorMessage.addHeader("protocolCode", protocolCode);
        if (null != deviceOperator.getProduct()) {
            processorMessage.addHeader("deviceType", deviceOperator.getProduct().getDeviceType());
        }
        if (deviceOperator instanceof GatewayDeviceOperatorImpl) {
            // 是集中器
            processorMessage.addHeader("parentId", deviceOperator.getDeviceId());
        }
        return processorMessage;
    }
}

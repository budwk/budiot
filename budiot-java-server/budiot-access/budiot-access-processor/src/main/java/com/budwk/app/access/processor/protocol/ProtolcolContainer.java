package com.budwk.app.access.processor.protocol;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.objects.dto.DeviceRawDataDTO;
import com.budwk.app.access.processor.cache.RedissionCacheStore;
import com.budwk.app.access.processor.protocol.impl.DefaultDecodeContext;
import com.budwk.app.access.processor.protocol.impl.DefaultEncodeContext;
import com.budwk.app.access.processor.support.DeviceOperatorSupport;
import com.budwk.app.access.processor.timer.DelayTaskHelper;
import com.budwk.app.access.protocol.codec.*;
import com.budwk.app.access.protocol.codec.context.DeviceEventContext;
import com.budwk.app.access.protocol.codec.context.EncodeContext;
import com.budwk.app.access.protocol.codec.impl.DefaultDeviceOperator;
import com.budwk.app.access.protocol.codec.impl.GatewayDeviceOperatorImpl;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultResponseResult;
import com.budwk.app.access.protocol.codec.result.EncodeResult;
import com.budwk.app.access.protocol.device.CommandInfo;
import com.budwk.app.access.protocol.device.ProductInfo;
import com.budwk.app.access.protocol.message.DeviceMessage;
import com.budwk.app.access.protocol.message.DeviceResponseMessage;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import com.budwk.app.access.storage.DeviceRawDataStorage;
import com.budwk.app.iot.enums.DeviceCmdStatus;
import com.budwk.starter.rocketmq.enums.ConsumeMode;
import com.budwk.starter.rocketmq.enums.MessageModel;
import lombok.extern.slf4j.Slf4j;
import org.nutz.castor.Castors;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@IocBean
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
    private final static long SESSION_CACHE_TIMEOUT = 600;
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
    @Inject
    private DeviceOperatorSupport deviceOperatorSupport;
    @Inject
    private DeviceEventContext deviceEventContext;
    @Inject
    private PropertiesProxy conf;
    private String instanceId;

    public void init() {
        cacheMap = redissonClient.getMapCache("protocol_container");
        deviceSessionCache = redissonClient.getMapCache("device_addr");
        sessionDevice = redissonClient.getMapCache("device_session");
        listen();
    }

    public void listen() {
        listenEvent();//设备事件(注册/注销)
        listenData();//数据上报
        listenCmd();//指令发送
        listenCmdDown();//指令下发状态更新(含网关设备)
    }

    private void listenEvent() {
        messageTransfer.subscribe("CONTAINER", TopicConstant.DEVICE_EVENT,
                "*", MessageModel.CLUSTERING, ConsumeMode.ORDERLY,
                message -> {
                    log.debug("监听事件 {}", message.getBody());
                    //延迟10s等待设备新增入库,否则查询为空
                    delayTaskHelper.delayRun(() -> {
                        NutMap map = Lang.obj2nutmap(message.getBody());
                        String event = map.getString("event", "");
                        String protocolCode = map.getString("protocolCode");
                        String deviceId = map.getString("deviceId");
                        String productId = map.getString("productId");
                        switch (event) {
                            // 设备注册事件
                            case "device-register": {
                                DeviceOperator operator = null;
                                if (Strings.isNotBlank(deviceId)) {
                                    operator = deviceRegistry.getDeviceOperator(deviceId);
                                    if (Strings.isBlank(protocolCode)) {
                                        protocolCode = (String) operator.getProperty("protocolCode");
                                    }
                                }
                                Protocol protocol = protocolLoader.loadProtocols(protocolCode);
                                log.info("设备注册事件 协议解析包{} 设备对象{}", protocol, operator);
                                if (null != protocol && null != operator) {
                                    protocol.onDeviceRegistered(deviceEventContext, operator);
                                }
                                break;
                            }
                            // 设备注销事件
                            case "device-unregister": {
                                DeviceOperator operator = null;
                                if (Strings.isNotBlank(deviceId)) {
                                    operator = deviceRegistry.getDeviceOperator(deviceId);
                                    if (Strings.isBlank(protocolCode)) {
                                        protocolCode = (String) operator.getProperty("protocolCode");
                                    }
                                }
                                Protocol protocol = protocolLoader.loadProtocols(protocolCode);
                                if (null != protocol) {
                                    if (null == operator) {
                                        operator = new DefaultDeviceOperator() {
                                            @Override
                                            public ProductInfo getProduct() {
                                                return Strings.isBlank(productId) ? null : deviceRegistry.getProductInfo(productId);
                                            }
                                        };
                                        operator.setProperty("deviceId", deviceId);
                                        operator.setProperty("iotDevId", map.getString("iotDevId"));
                                    }
                                    protocol.onDeviceUnRegistered(deviceEventContext, operator);
                                }
                                break;
                            }
                            // 采集器注册事件
                            case "collector-register": {
                                DeviceOperator operator = null;
                                if (Strings.isNotBlank(deviceId)) {
                                    operator = deviceRegistry.getGatewayDevice("id", deviceId);
                                    if (Strings.isBlank(protocolCode)) {
                                        protocolCode = (String) operator.getProperty("protocolCode");
                                    }
                                }
                                Protocol protocol = protocolLoader.loadProtocols(protocolCode);
                                log.info("设备注册事件 协议解析包{} 设备对象{}", protocol, operator);
                                if (null != protocol && null != operator) {
                                    protocol.onDeviceRegistered(deviceEventContext, operator);
                                }
                                break;
                            }
                            // 采集器注销事件
                            case "collector-unregister": {
                                DeviceOperator operator = null;
                                if (Strings.isNotBlank(deviceId)) {
                                    operator = deviceRegistry.getGatewayDevice("id", deviceId);
                                    if (Strings.isBlank(protocolCode)) {
                                        protocolCode = (String) operator.getProperty("protocolCode");
                                    }
                                }
                                Protocol protocol = protocolLoader.loadProtocols(protocolCode);
                                if (null != protocol) {
                                    if (null == operator) {
                                        operator = new DefaultDeviceOperator() {
                                            @Override
                                            public ProductInfo getProduct() {
                                                return Strings.isBlank(productId) ? null : deviceRegistry.getProductInfo(productId);
                                            }
                                        };
                                        operator.setProperty("deviceId", deviceId);
                                        operator.setProperty("iotDevId", map.getString("iotDevId"));
                                    }
                                    protocol.onDeviceUnRegistered(deviceEventContext, operator);
                                }
                                break;
                            }
                        }
                    }, 10);

                });
    }

    /**
     * 监听指令下发
     */
    private void listenCmdDown() {
        // 指令回复使用 CLUSTERING模式，只需要一个处理即可
        messageTransfer.subscribe("CONTAINER", TopicConstant.DEVICE_CMD_DOWN,
                "*", MessageModel.CLUSTERING, ConsumeMode.ORDERLY,
                this::handleCmdResp);
    }

    private <T extends Serializable> void handleCmdResp(Message<T> message) {
        String deviceId = message.getHeader("deviceId");
        String commandId = message.getHeader("commandId");
        if (Strings.isBlank(commandId)) {
            return;
        }
        log.debug("设备 {} 指令 {} 发送到设备结果 {}", deviceId, commandId, message.getBody());
        // 如果需要指令的中间状态，那么就放开下面的注释
        if (Strings.isBlank(deviceId) || Strings.isBlank(commandId)) {
            return;
        }
        // NutMap nutMap = (NutMap) message.getBody();
        deviceRegistry.makeCommandSend(commandId);
    }

    /**
     * 监听手动下发的指令
     */
    private void listenCmd() {
        // 指令下发使用 BROADCASTING 模式，因为有多个客户端
        messageTransfer.subscribe("CONTAINER", TopicConstant.DEVICE_CMD_TRIGGER,
                "*", MessageModel.BROADCASTING, ConsumeMode.ORDERLY,
                this::handleCmd);
    }

    private <T extends Serializable> void handleCmd(Message<T> message) {
        log.debug("收到指令{}", message.toString());
        CommandInfo commandInfoDTO = (CommandInfo) message.getBody();
        if (null == commandInfoDTO) {
            return;
        }

        String key;
        DeviceOperator device = deviceRegistry.getDeviceOperator(commandInfoDTO.getDeviceId());
        if (Lang.isNotEmpty(device) && Lang.isNotEmpty(device.getProduct()) &&
                ("TCP".equals(device.getProduct().getProtocolType())
                        ||
                        "MQ".equals(device.getProduct().getProtocolType()) ||
                        "MQTT".equals(device.getProduct().getProtocolType()))) {
            key = "cmd:send:" + commandInfoDTO.getDeviceId();
        } else {
            return;
        }
        Object o = cacheMap.get(key);
        if (null != o) {
            log.warn("正在下发其他指令 {}", o);
            return;
        }
        CommandInfo cmdInfo = new CommandInfo();
        cmdInfo.setCommandId(commandInfoDTO.getCommandId());
        cmdInfo.setCommandCode(commandInfoDTO.getCommandCode());
        cmdInfo.setDeviceId(commandInfoDTO.getDeviceId());
        cmdInfo.setParams(commandInfoDTO.getParams());
        if (Strings.isBlank(cmdInfo.getCommandId())) {
            cmdInfo.setCommandId(cmdInfo.getDeviceId() + "_" + cmdInfo.getCommandCode() + "_" + System.currentTimeMillis());
        }
        // 构造指令
        this.buildCommand(cmdInfo);
    }

    public void listenData() {
        log.debug("listenData");
        messageTransfer.subscribe("DEVICE_DATA_UP", TopicConstant.DEVICE_DATA_UP, this::handleDataUp);
    }

    private void handleDataUp(Message<?> message) {
        log.info("收到设备上报消息 {}", message.toString());
        Object body = message.getBody();
        EncodedMessage encodedMessage = Castors.me().castTo(body, EncodedMessage.class);
        String protocolCode = encodedMessage.getProtocolCode();
        log.info("设备协议标识 {}", protocolCode);
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
            log.info(" session deviceId ===> {}", sessionDeviceId);
            DeviceOperator deviceOperator = null;
            if (protocolCode.contains("COLLECTOR") || protocolCode.contains("GATEWAY")) {
                // 采集器/集中器设备
                deviceOperator = Strings.isNotBlank(sessionDeviceId) && !"null".equals(sessionDeviceId) ? deviceRegistry.getGatewayDevice(protocolCode, sessionDeviceId) : null;

            } else {
                deviceOperator = Strings.isNotBlank(sessionDeviceId) && !"null".equals(sessionDeviceId) ? deviceRegistry.getDeviceOperator(sessionDeviceId) : null;
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
                    if (!replyMessage.isSend()) {
                        Message<EncodedMessage> reply = new Message<>(message.getFrom(), replyMessage);
                        reply.setFrom(TopicConstant.DEVICE_CMD_DOWN);
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
            } else {
                if (null != deviceOperator) {
                    decodeContext.setDevice(deviceOperator);
                }
            }
            // 解析数据
            DecodeResult result = messageCodec.decode(decodeContext);
            log.info("协议解析数据: {}", result);
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
                    sessionDevice.put(sessionId, decodeContext.getDevice().getDeviceId(), SESSION_CACHE_TIMEOUT, TimeUnit.SECONDS);
                }
                // 同时更新状态为在线以及最近通讯时间
                deviceRegistry.updateDeviceOnline(decodeContext.getDevice().getDeviceId());
            }

            // 处理数据上报
            if (result instanceof DefaultDecodeResult) {
                DefaultDecodeResult reportResult = (DefaultDecodeResult) result;
                String deviceId = reportResult.getDeviceId();
                // 保存一下信息，用于发送指令时的处理
                deviceSessionCache.put(deviceId,
                        NutMap.NEW()
                                .setv("address", message.getFrom())
                                .setv("sessionId", message.getHeader("sessionId"))
                                .setv("transportType", encodedMessage.getTransportType().name())
                                .setv("protocolCode", protocolCode), SESSION_CACHE_TIMEOUT, TimeUnit.SECONDS);
                // 存储上报的原始数据
                rawData.setDeviceId(deviceId);
                rawData.setParsedData(Json.toJson(reportResult.getMessages(), JsonFormat.tidy()));
                rawData.setEndTime(System.currentTimeMillis());
                deviceRawDataStorage.save(rawData);
                if (Lang.isNotEmpty(reportResult.getMessages())) {
                    // 将解析后的数据推送到业务处理模块
                    Message<DefaultDecodeResult> processorMessage = new Message<>(TopicConstant.DEVICE_DATA_PROCESSOR, reportResult);
                    processorMessage.setSender("protocol");
                    processorMessage.addHeader("protocolCode", protocolCode);
                    if (deviceOperator instanceof GatewayDeviceOperatorImpl) {
                        log.info("66666");
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
                                Message<DefaultDecodeResult> processorMessage2 = getSubDeviceMessage(protocolCode, deviceId, messages, deviceOperator);
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
                DefaultResponseResult cmdRespResult = (DefaultResponseResult) result;
                this.processCmdResp(decodeContext.getDevice(), cmdRespResult);
                // 存储上报的原始数据
                rawData.setDeviceId(null != decodeContext.getDevice() ? decodeContext.getDevice().getDeviceId() : null);
                rawData.setParsedData(Json.toJson(cmdRespResult.getMessages(), JsonFormat.tidy()));
                rawData.setEndTime(System.currentTimeMillis());
                deviceRawDataStorage.save(rawData);
            }
            // 刷新设备信息
            deviceOperatorSupport.flush(decodeContext.getDevice());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processCmdResp(DeviceOperator deviceOperator, DefaultResponseResult cmdRespResult) {
        if (null == deviceOperator) {
            return;
        }
        String deviceId = deviceOperator.getDeviceId();
        String messageId = cmdRespResult.getCommandId();
        if (deviceOperator instanceof GatewayDeviceOperatorImpl) {
            // 是集中器
            deviceId = cmdRespResult.getDeviceId();
        }
        String commandCode = cmdRespResult.getCommandCode();
        String protocolCode = cmdRespResult.getProtocolCode();
        if (Strings.isBlank(messageId)) {
            // 这个就当做是指令id
            if (Strings.isNotBlank(protocolCode) && protocolCode.equals("LORA")) {
                messageId = (String) cacheMap.get("cmd:send:" + deviceId + ":" + commandCode);
            } else {
                messageId = (String) cacheMap.get("cmd:send:" + deviceId);
            }
        }
        List<DeviceMessage> messages = cmdRespResult.getMessages()
                .stream().filter(m -> !(m instanceof DeviceResponseMessage)).collect(Collectors.toList());

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
                Message<DefaultDecodeResult> processorMessage = getSubDeviceMessage(deviceId, protocolCode, messages, deviceOperator);
                // 将解析后的数据推送到业务处理去
                messageTransfer.publish(processorMessage);
            }
        }
        // 为空或者是最后一条自动发送的结束指令，则不处理
        if (Strings.isBlank(messageId) || messageId.contains("END")) {
            return;
        }
        if (cmdRespResult.isLastFrame()) {
            try {
                deviceRegistry.makeCommandFinish(messageId,
                        cmdRespResult.isSuccess() ? DeviceCmdStatus.FINISHED.value() : DeviceCmdStatus.FAILED.value(), Json.toJson(cmdRespResult.getMessages(), JsonFormat.tidy()));
            } finally {
                if (Strings.isNotBlank(protocolCode) && protocolCode.equals("LORA")) {
                    cacheMap.remove("cmd:send:" + deviceId + ":" + commandCode);
                } else {
                    cacheMap.remove("cmd:send:" + deviceId);
                }
                // 获取指令并构造指令
                CommandInfo deviceCommand = deviceRegistry.getDeviceCommand(deviceId);
                if (null != deviceCommand) {
                    this.buildCommand(deviceCommand);
                } else {
                    this.noMoreCommand(deviceOperator);
                }
            }
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
     * 最后回复指令
     *
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
     *
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
                if (device == null) {
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
                device = deviceRegistry.getDeviceOperator(deviceId);
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
                        request.setFrom(TopicConstant.DEVICE_CMD_DOWN);
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

    private static Message<DefaultDecodeResult> getSubDeviceMessage(String deviceId, String protocolCode, List<DeviceMessage> deviceMessage, DeviceOperator deviceOperator) {
        Message<DefaultDecodeResult> processorMessage =
                new Message<>(TopicConstant.DEVICE_DATA_PROCESSOR, new DefaultDecodeResult(deviceId, deviceMessage));
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

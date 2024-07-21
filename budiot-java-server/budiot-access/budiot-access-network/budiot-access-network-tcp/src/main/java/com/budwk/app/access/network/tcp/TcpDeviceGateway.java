package com.budwk.app.access.network.tcp;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import com.budwk.app.access.network.tcp.client.TcpClient;
import com.budwk.app.access.network.tcp.server.TcpServer;
import com.budwk.app.access.network.tcp.server.VertxTcpServer;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import com.budwk.starter.rocketmq.enums.ConsumeMode;
import com.budwk.starter.rocketmq.enums.MessageModel;
import io.netty.util.NetUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.castor.Castors;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TcpDeviceGateway implements DeviceGateway {


    private String instanceId;
    private final MessageTransfer messageTransfer;
    private final TcpServer tcpServer;
    private final DeviceGatewayConfiguration configuration;

    private final Map<String, TcpClient> clientStorage = new ConcurrentHashMap<>();

    private final NutMap hexMap = NutMap.NEW();

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


    public TcpDeviceGateway(DeviceGatewayConfiguration configuration, MessageTransfer messageTransfer) {
        this.configuration = configuration;
        this.messageTransfer = messageTransfer;
        this.tcpServer = new VertxTcpServer(configuration.getProperties());
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.TCP;
    }


    @Override
    public void start() {
        // 启动服务
        tcpServer.start()
                .handleConnection(tcpClient -> {
                    log.info("TcpDeviceGateway 客户端 {} 已连接", tcpClient.getId());
                    clientStorage.put(tcpClient.getId(), tcpClient);
                    tcpClient
                            .onMessage(bytes -> {
                                log.debug("tcpClient接收到数据 {}", bytes);
                                if ("QJ_COLLECTOR".equals(configuration.getProtocolCode())) {
                                    // 千嘉集中器
                                    String hexStr = hexMap.getString(tcpClient.getId(), "") + ByteConvertUtil.bytesToHex(bytes);
                                    hexMap.setv(tcpClient.getId(), hexStr);
                                    if (checkQjCollectorData(hexStr)) {
                                        hexMap.remove(tcpClient.getId());
                                        Message<EncodedMessage> message =
                                                new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(ByteConvertUtil.hexToBytes(hexStr)));
                                        message.setSender(getInstanceId());
                                        message.setFrom(getReplyAddress());
                                        message.addHeader("sessionId", tcpClient.getId());
                                        messageTransfer.publish(message);
                                    }
                                } else if ("JINKA_FORWARD".equals(configuration.getProtocolCode())) {
                                    // 金卡采集器
                                    String hexStr = ByteConvertUtil.bytesToHex(bytes);
                                    if (hexStr.startsWith("17")) {
                                        Message<EncodedMessage> message =
                                                new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(bytes, "CangNan"));
                                        message.setSender(getInstanceId());
                                        message.setFrom(getReplyAddress());
                                        message.addHeader("sessionId", tcpClient.getId());
                                        messageTransfer.publish(message);
                                        log.info("JINKA_FORWARD转发数据 {} 到 {}", hexStr, "CangNan");
                                    } else if (hexStr.startsWith("4040")) {
                                        Message<EncodedMessage> message =
                                                new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(bytes, "TianXin_RTU"));
                                        message.setSender(getInstanceId());
                                        message.setFrom(getReplyAddress());
                                        message.addHeader("sessionId", tcpClient.getId());
                                        messageTransfer.publish(message);
                                        log.info("JINKA_FORWARD转发数据 {} 到 {}", hexStr, "TianXin_RTU");
                                    } else if (hexStr.equals("68300316")) {
                                        Message<EncodedMessage> message =
                                                new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(bytes, "LuoMeiTe"));
                                        message.setSender(getInstanceId());
                                        message.setFrom(getReplyAddress());
                                        message.addHeader("sessionId", tcpClient.getId());
                                        messageTransfer.publish(message);
                                        log.info("JINKA_FORWARD转发数据 {} 到 {}", hexStr, "LuoMeiTe");
                                    } else {
                                        int length = ByteConvertUtil.bytes2Int(bytes, 1, 2);
                                        String start = ByteConvertUtil.bytes2String(bytes, 0, 1);
                                        String dataStart = ByteConvertUtil.bytes2String(bytes, 7, 1);
                                        if (start.equals(dataStart)) {
                                            Message<EncodedMessage> message =
                                                    new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(bytes, "TianXin_RTU"));
                                            message.setSender(getInstanceId());
                                            message.setFrom(getReplyAddress());
                                            message.addHeader("sessionId", tcpClient.getId());
                                            messageTransfer.publish(message);
                                            log.info("JINKA_FORWARD转发数据 {} 到 {}", hexStr, "TianXin_RTU");
                                        } else if (length == bytes.length) {
                                            Message<EncodedMessage> message =
                                                    new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(bytes, "LuoMeiTe"));
                                            message.setSender(getInstanceId());
                                            message.setFrom(getReplyAddress());
                                            message.addHeader("sessionId", tcpClient.getId());
                                            messageTransfer.publish(message);
                                            log.info("JINKA_FORWARD转发数据 {} 到 {}", hexStr, "LuoMeiTe");
                                        }
                                    }
                                } else {
                                    // 默认
                                    Message<EncodedMessage> message =
                                            new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(bytes));
                                    message.setSender(getInstanceId());
                                    message.setFrom(getReplyAddress());
                                    message.addHeader("sessionId", tcpClient.getId());
                                    messageTransfer.publish(message);
                                }
                            })
                            .onClose(unused -> {
                                clientStorage.remove(tcpClient.getId());
                            })
                            .onError(throwable -> {
                                log.error("客户端{}处理数据出错", tcpClient.getId(), throwable);
                            });
                });
        startCmdListener();
    }

    private TcpMessage newTcpMessage(byte[] bytes) {
        TcpMessage tcpMessage = new TcpMessage(bytes, configuration.getProtocolCode());
        tcpMessage.setPayloadType(Strings.sBlank(configuration.getProperties().get("payloadType"), "hex"));
        return tcpMessage;
    }

    private void startCmdListener() {
        // 接收指令进行下发，使用广播模式订阅
        messageTransfer.subscribe(
                this.configuration.getId(),
                getReplyAddress(), "*",
                MessageModel.BROADCASTING,
                ConsumeMode.ORDERLY, message -> {
            Object body = message.getBody();
            EncodedMessage encodedMessage = Castors.me().castTo(body, EncodedMessage.class);
            byte[] bytes = encodedMessage.getPayload();
            TcpClient client = clientStorage.get(message.getHeader("sessionId"));
            NutMap result = NutMap.NEW()
                    .setv("result", 0)
                    .setv("deviceId", message.getHeader("deviceId"))
                    .setv("commandId", message.getHeader("commandId"));
            log.debug("TcpDeviceGateway 发送指令 commandId: {}", message.getHeader("commandId"));
            log.debug("TcpDeviceGateway 指令信息 {}", ByteConvertUtil.bytesToHex(bytes));
            if (null != client && null != bytes) {
                if ("LuoMeiTe".equals(this.configuration.getId())) {
                    executor.schedule(() -> {
                        send(message, client, bytes, result);
                    }, 2, TimeUnit.SECONDS);
                } else if ("CangNan".equals(this.configuration.getId())) {
                    executor.schedule(() -> {
                        send(message, client, bytes, result);
                    }, 10, TimeUnit.SECONDS);
                } else {
                    send(message, client, bytes, result);
                }
            } else {
                log.debug("发送指令失败未找到设备会话信息");
                result.setv("result", -1).setv("msg", "未找到设备会话信息");
                replyCmdSendResult(message.getFrom(), result, message.getHeaders());
            }
        });
    }

    private void send(Message<Serializable> message, TcpClient client, byte[] bytes, NutMap result) {
        client.send(bytes).whenComplete((unused, throwable) -> {
            if (null == throwable) {
                replyCmdSendResult(message.getFrom(), result, message.getHeaders());
            } else {
                replyCmdSendResult(message.getFrom(), result.setv("result", -1).setv("msg", "发送数据到设备失败"), message.getHeaders());
            }
        });
    }

    private void replyCmdSendResult(String replyAddress, NutMap result, Map<String, String> headers) {
        if (Strings.isBlank(replyAddress)) {
            return;
        }
        Message<NutMap> reply = new Message<>(replyAddress, result);
        reply.setSender(getInstanceId());
        reply.getHeaders().putAll(headers);
        messageTransfer.publish(reply);
    }

    private String getReplyAddress() {
        return String.format(this.configuration.getId() + "_%s_%s", TopicConstant.DEVICE_CMD_DOWN, getInstanceId());
    }

    public String getInstanceId() {
        if (Strings.isNotBlank(instanceId)) {
            return instanceId;
        }
        instanceId = Strings.sBlank(this.configuration.getInstanceId());
        if (Strings.isBlank(instanceId)) {
            String id = configuration.getId() + "_" + NetUtil.LOCALHOST + ManagementFactory.getRuntimeMXBean().getName();
            instanceId = Integer.toHexString(id.hashCode());
        }
        return instanceId;
    }

    /**
     * 验证千嘉集中器上报数据是否完整
     *
     * @param hexStr 上报数据
     * @return 是否完整
     */
    private boolean checkQjCollectorData(String hexStr) {
        boolean hasCompleteMessage = false;
        if (hexStr.length() == 42 && hexStr.endsWith("00")) {
            hasCompleteMessage = true;
        } else if ("FE".equals(hexStr)) {
            hasCompleteMessage = true;
        } else if (hexStr.startsWith("DA") && hexStr.endsWith("0D")) {
            hasCompleteMessage = true;
        }
        return hasCompleteMessage;
    }

    private TcpMessage newTcpMessage(byte[] bytes, String protocol) {
        TcpMessage tcpMessage = new TcpMessage(bytes, protocol);
        tcpMessage.setPayloadType(Strings.sBlank(configuration.getProperties().get("payloadType"), "hex"));
        return tcpMessage;
    }
}

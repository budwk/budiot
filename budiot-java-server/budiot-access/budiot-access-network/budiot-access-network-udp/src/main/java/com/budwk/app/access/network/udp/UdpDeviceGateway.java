package com.budwk.app.access.network.udp;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import com.budwk.app.access.network.udp.client.UdpSender;
import com.budwk.app.access.network.udp.server.UdpServer;
import com.budwk.app.access.network.udp.server.VertxUdpServer;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.UdpMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import com.budwk.starter.rocketmq.enums.ConsumeMode;
import com.budwk.starter.rocketmq.enums.MessageModel;
import io.netty.util.NetUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.castor.Castors;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UdpDeviceGateway implements DeviceGateway {

    private static final Map<String, UdpSender> senderMap = new ConcurrentHashMap<>();


    private String instanceId;
    private final MessageTransfer messageTransfer;

    private UdpServer server;
    private final DeviceGatewayConfiguration configuration;

    public UdpDeviceGateway(DeviceGatewayConfiguration configuration, MessageTransfer messageTransfer) {
        this.messageTransfer = messageTransfer;
        this.configuration = configuration;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.UDP;
    }

    @Override
    public void start() {
        startCmdListener();
        server = new VertxUdpServer(configuration.getProperties())
                .start()
                .onMessage((sender, bytes) -> {
                    log.debug("UDP收到来自 {} 的消息 {}", sender.getId(), ByteConvertUtil.bytesToHex(bytes));
                    senderMap.put(sender.getId(), sender);
                    Message<EncodedMessage> message =
                            new Message<>(TopicConstant.DEVICE_DATA_UP,
                                    new UdpMessage(bytes, configuration.getProtocolCode()));
                    message.setSender(getInstanceId());
                    message.setFrom(getReplyAddress());
                    message.addHeader("sessionId", sender.getId());
                    messageTransfer.publish(message);
                });
    }


    private void startCmdListener() {
        messageTransfer.subscribe(this.configuration.getId(), getReplyAddress(), "*", MessageModel.BROADCASTING, ConsumeMode.CONCURRENTLY, message -> {
            Object body = message.getBody();
            EncodedMessage encodedMessage = Castors.me().castTo(body, EncodedMessage.class);
            byte[] bytes = encodedMessage.getPayload();
            UdpSender sender = senderMap.get(message.getHeader("sessionId"));
            NutMap result = NutMap.NEW()
                    .setv("result", 0)
                    .setv("deviceId", message.getHeader("deviceId"))
                    .setv("commandId", message.getHeader("commandId"));
            log.debug("发送指令：{}", message.getHeader("commandId"));
            if (null != sender && null != bytes) {
                server.send(sender, bytes).whenComplete((unused, throwable) -> {
                    if (null == throwable) {
                        replyCmdSendResult(message.getFrom(), result, message.getHeaders());
                    } else {
                        replyCmdSendResult(message.getFrom(), result.setv("result", -1).setv("msg", "发送数据到设备失败"), message.getHeaders());
                    }
                });
            } else {
                result.setv("result", -1).setv("msg", "未找到设备会话信息");
                replyCmdSendResult(message.getFrom(), result, message.getHeaders());
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
        return String.format(this.configuration.getId() + ":%s.%s", TopicConstant.DEVICE_CMD_DOWN, getInstanceId());
    }

    public String getInstanceId() {
        if (Strings.isNotBlank(instanceId)) {
            return instanceId;
        }
        instanceId = Strings.sBlank(this.configuration.getInstanceId());
        if (Strings.isBlank(instanceId)) {
            String id = configuration.getId() + "." + NetUtil.LOCALHOST + ManagementFactory.getRuntimeMXBean().getName();
            instanceId = Integer.toHexString(id.hashCode());
        }
        return instanceId;
    }
}

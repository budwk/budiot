package com.budwk.app.access.network.mqtt;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.MqttMessage;
import com.budwk.starter.rocketmq.enums.ConsumeMode;
import com.budwk.starter.rocketmq.enums.MessageModel;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.NetUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.nutz.castor.Castors;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.lang.management.ManagementFactory;
import java.util.Map;

/**
 * @author wizzer.cn
 */
@Slf4j
public class MqttDeviceGateway implements DeviceGateway {
    private String instanceId;
    private final MessageTransfer messageTransfer;

    private final DeviceGatewayConfiguration configuration;
    private MqttClient client;
    private String topic;

    public MqttDeviceGateway(DeviceGatewayConfiguration configuration, MessageTransfer messageTransfer) {
        this.messageTransfer = messageTransfer;
        this.configuration = configuration;
    }


    @Override
    public TransportType getTransportType() {
        return TransportType.MQTT;
    }

    @Override
    public void start() {
        MqttClientOptions options = new MqttClientOptions();
        // specify broker host
        options.setClientId(Strings.sNull(configuration.getProperties().get("clentId")));
        options.setUsername(Strings.sNull(configuration.getProperties().get("username")));
        options.setPassword(Strings.sNull(configuration.getProperties().get("password")));
        options.setMaxMessageSize(100_000_000);
        topic = Strings.sNull(configuration.getProperties().get("topic"));
        Vertx vertx = Vertx.vertx();
        client = MqttClient.create(vertx, options);
        // 接收到消息的处理
        client.publishHandler(s -> {
            try {
                // 根据实际情况修改
                MqttMessage mqttMessage = new MqttMessage(s.payload().getBytes(),configuration.getProtocolCode());
                mqttMessage.setMessageId(s.messageId() + "");
                mqttMessage.setClientId(client.clientId());
                mqttMessage.setPayloadType(Strings.sNull(configuration.getProperties().get("payloadType")));
                mqttMessage.setTopic(s.topicName());
                Message<EncodedMessage> message =
                        new Message<>(TopicConstant.DEVICE_DATA_UP,
                                mqttMessage);
                message.setSender(getInstanceId());
                message.setFrom(getReplyAddress());
                message.addHeader("sessionId", s.topicName());
                messageTransfer.publish(message);
                log.debug("Receive message with content: {} from topic {}", mqttMessage.payloadAsString(), s.topicName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // 连接到 MQTT 代理服务器
        client.connect(Integer.parseInt(Strings.sNull(configuration.getProperties().get("port"), "5000")), Strings.sNull(configuration.getProperties().get("host")), ar -> {
            if (ar.succeeded()) {
                log.debug("Connected to MQTT broker");
                // 订阅主题 # = 所有设备消息
                client.subscribe(topic, 0, ar2 -> {
                    if (ar2.succeeded()) {
                        log.debug("Subscribed to topic: {} " + topic);
                    } else {
                        log.debug("Failed to subscribe to topic: " + topic);
                    }
                });

            } else {
                System.err.println("Failed to connect to MQTT broker");
            }
        });
        startCmdListener();

    }

    private void startCmdListener() {
        messageTransfer.subscribe(this.configuration.getId(), getReplyAddress(), "*", MessageModel.BROADCASTING, ConsumeMode.CONCURRENTLY, message -> {
            Object body = message.getBody();
            EncodedMessage encodedMessage = Castors.me().castTo(body, EncodedMessage.class);
            byte[] bytes = encodedMessage.getPayload();
            NutMap result = NutMap.NEW()
                    .setv("result", 0)
                    .setv("deviceId", message.getHeader("deviceId"))
                    .setv("commandId", message.getHeader("commandId"));
            log.debug("发送指令：{}", message.getHeader("commandId"));
            if (null != bytes) {
                Buffer payload = Buffer.buffer(bytes);
                client.publish(message.getHeader("sessionId"), payload, MqttQoS.valueOf(0), false, false,
                        ar -> {
                            if (ar.succeeded()) {
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

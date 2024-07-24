# 网络协议开发

##  实例讲解

* 以 `budiot-access-network-tcp` 为例

### `TcpDeviceGatewayBuilder`

* Ioc 对象名称是固定格式：网络协议标识 + GatewayBuilder，以便设备网关服务`budiot-access-gateway`进行加载

```java
@IocBean(name = "tcpGatewayBuilder")
public class TcpDeviceGatewayBuilder implements DeviceGatewayBuilder {

    @Inject
    private MessageTransfer messageTransfer;

    @Override
    public String getId() {
        return "tcp";
    }

    @Override
    public String getName() {
        return "TCP";
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.TCP;
    }

    @Override
    public DeviceGateway buildGateway(DeviceGatewayConfiguration configuration) {
        return new TcpDeviceGateway(configuration, messageTransfer);
    }
}
```
* 网关配置文件中的 `transport` 与上面 Java 代码中的 Ioc 对象名称有对应关系

```yaml
gateway:
  DEMO: # 和设备协议标识一致
    transport: tcp # 固定 tcp
    protocolCode: DEMO  # 设备协议标识
    properties: # 网关的配置参数
      port: 9003 # 端口号
      host: '0.0.0.0' # 绑定的host，默认 0.0.0.0
      payloadType: string # 数据类型，可选 hex 和 string 默认 hex
```      

### `TcpDeviceGateway` 

* 接收设备数据转发到MQ

```java
Message<EncodedMessage> message =
        new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(bytes));
message.setSender(getInstanceId());
message.setFrom(getReplyAddress());
message.addHeader("sessionId", tcpClient.getId());
messageTransfer.publish(message);
```                                    

* 监听MQ消息转发给设备

```java
 private void send(Message<Serializable> message, TcpClient client, byte[] bytes, NutMap result) {
    client.send(bytes).whenComplete((unused, throwable) -> {
        if (null == throwable) {
            replyCmdSendResult(message.getFrom(), result, message.getHeaders());
        } else {
            replyCmdSendResult(message.getFrom(), result.setv("result", -1).setv("msg", "发送数据到设备失败"), message.getHeaders());
        }
    });
}

```


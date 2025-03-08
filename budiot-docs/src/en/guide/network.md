# Network Protocol Development

## Example Explanation

* Using `budiot-access-network-tcp` as an example

### `TcpDeviceGatewayBuilder`

* The IoC object name follows a fixed format: network protocol identifier + GatewayBuilder, so that the device gateway service `budiot-access-gateway` can load it

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
* The `transport` in the gateway configuration file corresponds to the IoC object name in the Java code above

```yaml
gateway:
  DEMO: # Match the device protocol identifier
    transport: tcp # Fixed as tcp
    protocolCode: DEMO  # Device protocol identifier
    properties: # Gateway configuration parameters
      port: 9003 # Port number
      host: '0.0.0.0' # Binding host, default is 0.0.0.0
      payloadType: string # Data type, options are hex and string, default is hex
```      

### `TcpDeviceGateway` 

* Receive device data and forward to MQ

```java
Message<EncodedMessage> message =
        new Message<>(TopicConstant.DEVICE_DATA_UP, newTcpMessage(bytes));
message.setSender(getInstanceId());
message.setFrom(getReplyAddress());
message.addHeader("sessionId", tcpClient.getId());
messageTransfer.publish(message);
```                                    

* Listen to MQ messages and forward to devices

```java
 private void send(Message<Serializable> message, TcpClient client, byte[] bytes, NutMap result) {
    client.send(bytes).whenComplete((unused, throwable) -> {
        if (null == throwable) {
            replyCmdSendResult(message.getFrom(), result, message.getHeaders());
        } else {
            replyCmdSendResult(message.getFrom(), result.setv("result", -1).setv("msg", "Failed to send data to device"), message.getHeaders());
        }
    });
}
``` 
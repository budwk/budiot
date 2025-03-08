# Device Protocol Development

## Example Explanation

* Using `budiot-access-protocol-demo` as an example

### DemoProtocol

```java
public class DemoProtocol implements Protocol {
    public static final String PROTOCOL_CODE = "DEMO";
    public static final String PROTOCOL_NAME = "Demo Protocol";

    @Override
    public String getCode() {
        return PROTOCOL_CODE;
    }

    @Override
    public String getName() {
        return PROTOCOL_NAME;
    }

    @Override
    public List<TransportType> getSupportedTransportTypes() {
        return Collections.singletonList(TransportType.TCP);
    }

    @Override
    public MessageCodec getMessageCodec(TransportType transportType) {
        switch (transportType) {
            case TCP:
            case UDP:
                return new DefaultMessageCodec(getCode());
        }
        return null;
    }

}
```

### DefaultMessageCodec

* DecodeProcessor parses data reported by devices

Parse data, get function code, and determine data types such as data reporting, event reporting, fault alarm, fault recovery, etc. based on the function code

```java
//
List<DeviceMessage> messageList = new ArrayList<>();
            messageList.add(deviceDataMessage);
            messageList.add(deviceEventMessage);
return new DefaultDecodeResult(deviceOperator.getDeviceId(), messageList);
```

Command reply data, command execution result, `commandId` is obtained from cache. Devices generally use a "one receive, one send" mechanism, and the command information in the cache is the one that was sent

```java
DefaultResponseResult cmdRespResult = new DefaultResponseResult(commandId, responseMessage);
                cmdRespResult.setCommandCode(cmd);
return cmdRespResult;
```

* EncodeProcessor converts command data to be sent

After device communication ends, the system will automatically trigger the `END` command. If the device does not support `END`, remember to skip it

```java
public EncodeResult process() {
    CommandInfo commandInfo = context.getCommandInfo();
    String code = commandInfo.getCommandCode();
    // Skip if there is no END command
    if("END".equals(code)){
        log.info("END");
        return null;
    }
    Command command = Command.from(code);
    Function<CommandInfo, EncodeResult> builder = builders.get(command);
    if (null == builder) {
        throw new MessageCodecException("Unsupported command" + code);
    }
    CacheStore cacheStore = context.getCacheStore(DemoProtocol.PROTOCOL_CODE + ":" + deviceOperator.getDeviceId());
    cacheStore.set(DemoProtocol.PROTOCOL_CODE + ":CMD_SEND_ID:" + command.name(), commandInfo.getCommandId());
    return builder.apply(commandInfo);
}
``` 
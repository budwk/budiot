# 设备协议开发

##  实例讲解

* 以 `budiot-access-protocol-demo` 为例

### DemoProtocol

```java
public class DemoProtocol implements Protocol {
    public static final String PROTOCOL_CODE = "DEMO";
    public static final String PROTOCOL_NAME = "演示协议";

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

* DecodeProcessor 解析设备上报的数据

解析数据，获取功能码，根据功能码判断数据上报、事件上报、故障告警、故障恢复等数据类型

```java
//
List<DeviceMessage> messageList = new ArrayList<>();
            messageList.add(deviceDataMessage);
            messageList.add(deviceEventMessage);
return new DefaultDecodeResult(deviceOperator.getDeviceId(), messageList);
```

指令回复数据，指令执行结果，`commandId` 从缓存获取，设备一般是`一收一发`机制，缓存里的指令信息即是下发的

```java
DefaultResponseResult cmdRespResult = new DefaultResponseResult(commandId, responseMessage);
                cmdRespResult.setCommandCode(cmd);
return cmdRespResult;
```

* EncodeProcessor 转换下发的指令数据

设备通信结束后，系统会默认触发  `END` 指令，如果设备不支持 `END` 记得跳过

```java
public EncodeResult process() {
    CommandInfo commandInfo = context.getCommandInfo();
    String code = commandInfo.getCommandCode();
    // 没有END指令则跳过
    if("END".equals(code)){
        log.info("END");
        return null;
    }
    Command command = Command.from(code);
    Function<CommandInfo, EncodeResult> builder = builders.get(command);
    if (null == builder) {
        throw new MessageCodecException("不支持的指令" + code);
    }
    CacheStore cacheStore = context.getCacheStore(DemoProtocol.PROTOCOL_CODE + ":" + deviceOperator.getDeviceId());
    cacheStore.set(DemoProtocol.PROTOCOL_CODE + ":CMD_SEND_ID:" + command.name(), commandInfo.getCommandId());
    return builder.apply(commandInfo);
}

```
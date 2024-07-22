package com.budwk.app.access.protocol.demo.codec;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.budwk.app.access.protocol.codec.CacheStore;
import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.context.EncodeContext;
import com.budwk.app.access.protocol.codec.exception.MessageCodecException;
import com.budwk.app.access.protocol.codec.result.EncodeResult;
import com.budwk.app.access.protocol.demo.DemoProtocol;
import com.budwk.app.access.protocol.demo.enums.Command;
import com.budwk.app.access.protocol.device.CommandInfo;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Lang;
import org.nutz.lang.util.NutMap;


@Slf4j
public class EncodeProcessor {
    private final EncodeContext context;
    private final DeviceOperator deviceOperator;

    private final Map<Command, Function<CommandInfo, EncodeResult>> builders = new HashMap<>();


    public EncodeProcessor(EncodeContext context) {
        this.context = context;
        this.deviceOperator = context.getDeviceOperator();
        this.initBuilders();
    }

    private void initBuilders() {
        builders.put(Command.VALVE_CONTROL, this::valveControl);
    }

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

    private EncodeResult valveControl(CommandInfo commandInfo) {
        String hexStr = "2201";
        NutMap params = Lang.obj2nutmap(commandInfo.getParams());
        String device_no = params.getString("device_no");
        int status = params.getInt("status");
        // 0x01：授权开阀，0x02：强制关阀
        switch (status) {
            case 0:
                hexStr += "01";
                return write(hexStr);
            case 1:
            case 2:
                hexStr += "02";
                return write(hexStr);
            default:
                throw new MessageCodecException("阀控指令status参数错误:" + status);
        }
    }

    private EncodeResult write(String hexStr) {
        String deviceNo = (String) deviceOperator.getProperty("deviceNo");
        hexStr = deviceNo + hexStr;
        //hexStr += ByteConvertUtil.makeChecksum(hexStr) + "16";
        // 演示16进制字节转换
        return EncodeResult.createDefault(false, List.of(new TcpMessage(ByteConvertUtil.hexToBytes(hexStr), DemoProtocol.PROTOCOL_CODE)));
    }

}

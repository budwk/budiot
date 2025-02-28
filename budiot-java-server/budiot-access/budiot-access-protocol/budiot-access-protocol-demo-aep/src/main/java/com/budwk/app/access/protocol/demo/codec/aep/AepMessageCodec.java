package com.budwk.app.access.protocol.demo.codec.aep;


import com.budwk.app.access.protocol.codec.CacheStore;
import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.MessageCodec;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.codec.context.EncodeContext;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.EncodeResult;
import com.budwk.app.access.protocol.demo.AepDemoProtocol;
import com.budwk.app.access.protocol.demo.AepPlatformHelper;
import com.budwk.app.access.protocol.demo.codec.DecodeProcessor;
import com.budwk.app.access.protocol.demo.constants.CacheKeyConstant;
import com.budwk.app.access.protocol.device.CommandInfo;
import com.budwk.app.access.protocol.device.ProductInfo;
import com.budwk.app.access.protocol.message.codec.HttpMessage;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.json.Json;
import org.nutz.lang.Strings;

import java.util.List;

@Slf4j
public class AepMessageCodec implements MessageCodec {
    @Override
    public DecodeResult decode(DecodeContext context) {
        return getDecodeProcessor(context).process();
    }

    private DecodeProcessor getDecodeProcessor(DecodeContext context) {
        return new DecodeProcessor(context) {
            @Override
            public void replyToDeviceHex(String hex, String meaning) {
                DeviceOperator deviceOperator = context.getDevice();
                ProductInfo product = deviceOperator.getProduct();
                AepPlatformHelper aepPlatformHelper = new AepPlatformHelper(product.getProperties());
                String result;
                if (Strings.sBlank(product.getProperties().get("hasProfile")).equals("true")) {
                    result = aepPlatformHelper.createCommandProfile((String) deviceOperator.getProperty("iotPlatformId"), hex);
                } else {
                    result = aepPlatformHelper.createCommand((String) deviceOperator.getProperty("iotPlatformId"), hex);
                }
                // 标记已经发送过了，调用这个用来保存发送日志
                context.send(new HttpMessage(AepDemoProtocol.PROTOCOL_CODE) {
                    @Override
                    public boolean isSend() {
                        return true;
                    }

                    @Override
                    public String payloadAsString() {
                        return hex;
                    }

                    @Override
                    public Boolean getIsSuccess() {
                        return Strings.isNotBlank(result);
                    }
                });
            }

            @Override
            public void replyToDevice(byte[] replyBytes, String meaning, DeviceOperator deviceOperator) {
                log.info("[AEP]发送消息 hex ==> {}", ByteConvertUtil.bytesToHex(replyBytes));
//                DeviceOperator deviceOperator = context.getDevice();
                ProductInfo product = deviceOperator.getProduct();
                AepPlatformHelper aepPlatformHelper = new AepPlatformHelper(product.getProperties());
                String result;
                if (Strings.sBlank(product.getProperties().get("hasProfile")).equals("true")) {
                    result = aepPlatformHelper.createCommandProfile((String) deviceOperator.getProperty("iotPlatformId"), ByteConvertUtil.bytesToHex(replyBytes));
                } else {
                    result = aepPlatformHelper.createCommand((String) deviceOperator.getProperty("iotPlatformId"), ByteConvertUtil.bytesToHex(replyBytes));
                }
                // 标记已经发送过了，调用这个用来保存发送日志
                context.send(new HttpMessage(AepDemoProtocol.PROTOCOL_CODE) {
                    @Override
                    public boolean isSend() {
                        return true;
                    }

                    @Override
                    public String payloadAsString() {
                        return ByteConvertUtil.bytesToHex(replyBytes);
                    }

                    @Override
                    public Boolean getIsSuccess() {
                        return Strings.isNotBlank(result);
                    }
                });
            }
        };
    }

    @Override
    public EncodeResult encode(EncodeContext context) {
        CommandInfo commandInfo = context.getCommandInfo();
        log.debug("[AEP]传入对象" + Json.toJson(commandInfo));
        DeviceOperator deviceOperator = context.getDeviceOperator();
        ProductInfo product = deviceOperator.getProduct();
        AepPlatformHelper aepPlatformHelper = new AepPlatformHelper(product.getProperties());
        CacheStore cacheStore = context.getCacheStore(AepDemoProtocol.PROTOCOL_CODE + ":" + deviceOperator.getDeviceId());
        cacheStore.set(CacheKeyConstant.CMD_SEND_ID + ":" + commandInfo.getCommandCode(), commandInfo.getCommandId(), 600);
        String hex = "";//todo 你的业务代码生成指令hex
        String commandId;
        if (Strings.sBlank(product.getProperties().get("hasProfile")).equals("true")) {
            commandId = aepPlatformHelper.createCommandProfile((String) deviceOperator.getProperty("iotPlatformId"), hex);
        } else {
            commandId = aepPlatformHelper.createCommand((String) deviceOperator.getProperty("iotPlatformId"), hex);
        }
        TcpMessage tcpMessage = new TcpMessage(ByteConvertUtil.hexToBytes(hex), AepDemoProtocol.PROTOCOL_CODE);
        tcpMessage.setMessageId(commandId);

        return EncodeResult.createDefault(true, List.of(tcpMessage));
    }
}

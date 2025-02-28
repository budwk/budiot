package com.budwk.app.access.protocol.demo.codec;

import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.MessageCodec;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.codec.context.EncodeContext;
import com.budwk.app.access.protocol.codec.exception.MessageCodecException;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.EncodeResult;
import com.budwk.app.access.protocol.demo.codec.aep.AepMessageCodec;
import com.budwk.app.access.protocol.device.ProductInfo;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.HttpMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wizzer.cn
 */
public class HttpMessageCodec implements MessageCodec {
    private static final Map<String, MessageCodec> platformCodec = new HashMap<>();

    static {
        platformCodec.put("AEP", new AepMessageCodec());
    }

    @Override
    public DecodeResult decode(DecodeContext context) {
        EncodedMessage message = context.getMessage();
        if (!(message instanceof HttpMessage)) {
            throw new MessageCodecException("消息类型不是HttpMessage");
        }
        HttpMessage httpMessage = (HttpMessage) message;
        String platform = httpMessage.getPlatform();
        // 平台
        MessageCodec messageCodec = platformCodec.get(platform.toUpperCase());
        if (null == messageCodec) {
            throw new MessageCodecException("暂不支持的接入平台 " + platform);
        }
        return messageCodec.decode(context);
    }

    @Override
    public EncodeResult encode(EncodeContext context) {
        DeviceOperator deviceOperator = context.getDeviceOperator();
        ProductInfo product = deviceOperator.getProduct();
        String platform = product.getIotPlatform();
        // 平台
        MessageCodec messageCodec = platformCodec.get(platform.toUpperCase());
        if (null == messageCodec) {
            throw new MessageCodecException("暂不支持的接入平台 " + platform);
        }
        return messageCodec.encode(context);
    }
}

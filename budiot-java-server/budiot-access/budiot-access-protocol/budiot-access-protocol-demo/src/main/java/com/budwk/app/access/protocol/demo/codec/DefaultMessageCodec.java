package com.budwk.app.access.protocol.demo.codec;

import com.budwk.app.access.protocol.codec.MessageCodec;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.codec.context.EncodeContext;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.EncodeResult;
import com.budwk.app.access.protocol.message.codec.TcpMessage;

import java.util.Arrays;

public class DefaultMessageCodec implements MessageCodec {
    private String protocolCode;

    public DefaultMessageCodec(String protocolCode) {
        this.protocolCode = protocolCode;
    }

    @Override
    public DecodeResult decode(DecodeContext context) {
        return new DecodeProcessor(context).process();
    }

    @Override
    public EncodeResult encode(EncodeContext context) {
        byte[] bytes = "hello".getBytes();
        TcpMessage tcpMessage = new TcpMessage(bytes, protocolCode);
        return EncodeResult.createDefault(false, Arrays.asList(tcpMessage));
    }
}
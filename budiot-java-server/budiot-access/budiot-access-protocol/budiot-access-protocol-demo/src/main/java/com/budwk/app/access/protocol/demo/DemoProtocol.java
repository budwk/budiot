package com.budwk.app.access.protocol.demo;

import com.budwk.app.access.protocol.codec.MessageCodec;
import com.budwk.app.access.protocol.codec.Protocol;
import com.budwk.app.access.protocol.demo.codec.DefaultMessageCodec;
import com.budwk.app.access.protocol.enums.TransportType;

import java.util.Collections;
import java.util.List;

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

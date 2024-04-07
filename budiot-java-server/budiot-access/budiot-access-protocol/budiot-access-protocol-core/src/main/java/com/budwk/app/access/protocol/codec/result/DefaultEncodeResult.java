package com.budwk.app.access.protocol.codec.result;

import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import lombok.Data;

import java.util.List;

/**
 * 默认编码结果
 * @author wizzer.cn
 * @author zyang
 */
@Data
public class DefaultEncodeResult implements EncodeResult {

    /**
     * 是否已经发送
     */
    private final boolean send;
    private final List<EncodedMessage> messages;

    @Override
    public boolean isSend() {
        return send;
    }

    @Override
    public List<EncodedMessage> getMessage() {
        return messages;
    }
}

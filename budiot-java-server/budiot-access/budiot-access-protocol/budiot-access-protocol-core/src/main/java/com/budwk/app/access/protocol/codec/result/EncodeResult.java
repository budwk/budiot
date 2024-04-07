package com.budwk.app.access.protocol.codec.result;


import com.budwk.app.access.protocol.message.codec.EncodedMessage;

import java.util.List;

/**
 * 编码结果
 */
public interface EncodeResult {
    /**
     * 是否已经发送过。针对http的，如果已经调用接口发送过了，就设置为true
     */
    boolean isSend();

    /**
     * 编码后的数据
     */
    List<EncodedMessage> getMessage();

    static EncodeResult createDefault(boolean isSend, List<EncodedMessage> messages) {
        return new DefaultEncodeResult(isSend, messages);
    }
}

package com.budwk.app.access.message.impl;

import com.budwk.app.access.message.MessageTransfer;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean
public class MessageTransferServer {
    @Inject("refer:$ioc")
    private Ioc ioc;

    @Inject("java:$conf.getBoolean('rabbitmq.enable')")
    private boolean rabbitEnabled;

    @Inject("java:$conf.getBoolean('rocketmq.enable')")
    private boolean rocketEnabled;

    public MessageTransfer getMessageTransfer() {
        if (rabbitEnabled) {
            return ioc.get(RabbitMQMessageTransfer.class);
        } else if (rocketEnabled) {
            return ioc.get(RMQMessageTransfer.class);
        }
        throw new RuntimeException("未启用任何消息队列服务");
    }
}

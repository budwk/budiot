package com.budwk.app.access.processor;

import com.budwk.app.access.protocol.codec.Protocol;
import com.budwk.app.access.protocol.codec.ProtocolLoader;
import lombok.extern.slf4j.Slf4j;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Mirror;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@IocBean
public class LocalProtocolLoader implements ProtocolLoader {
    @Inject
    private PropertiesProxy conf;
    private final ConcurrentHashMap<String, Protocol> loadProtocols = new ConcurrentHashMap<>();

    @Override
    public Protocol loadProtocols(String protocolCode) {
        log.debug("protocolCode::"+protocolCode);
        try {
            log.debug("aaaa");
            // 如果已经加载过了就直接返回
            Protocol protocol = loadProtocols.get(protocolCode);
            if (null != protocol) {
                return protocol;
            }
            List<String> list = conf.getList("protocol.classes");
            log.debug("list::"+list.size());
            return protocol;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("加载协议包出错", e);
        }
        return null;
    }

}

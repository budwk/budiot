package com.budwk.app.access.processor.protocol;

import com.budwk.app.access.protocol.codec.Protocol;
import com.budwk.app.access.protocol.codec.ProtocolLoader;
import lombok.extern.slf4j.Slf4j;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@IocBean(create = "init")
public class LocalProtocolLoader implements ProtocolLoader {
    @Inject
    private PropertiesProxy conf;
    private final ConcurrentHashMap<String, Protocol> loadProtocols = new ConcurrentHashMap<>();

    public void init() {
        List<String> list = conf.getList("protocol.classes");
        for (String className : list) {
            try {
                Class<?> clazz = Class.forName(className);
                Method getCodeMethod = clazz.getDeclaredMethod("getCode");
                getCodeMethod.setAccessible(true);
                Protocol object = (Protocol) clazz.newInstance();
                String codeValue = (String) getCodeMethod.invoke(object);
                loadProtocols.put(codeValue, object);
            } catch (Exception e) {
                log.error("协议解析类加载出错 {}", className, e);
            }
        }
    }

    @Override
    public Protocol loadProtocols(String protocolCode) {
        return loadProtocols.get(protocolCode);
    }

}

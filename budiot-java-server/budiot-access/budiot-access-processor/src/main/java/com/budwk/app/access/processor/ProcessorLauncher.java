package com.budwk.app.access.processor;

import com.budwk.app.access.processor.core.Processor;
import com.budwk.app.access.processor.core.ProcessorContext;
import com.budwk.app.access.processor.protocol.LocalProtocolLoader;
import lombok.extern.slf4j.Slf4j;
import org.nutz.boot.NbApp;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@IocBean(create = "init")
public class ProcessorLauncher {
    @Inject
    private Ioc ioc;
    @Inject
    private LocalProtocolLoader loader;

    private List<Processor> processors = new ArrayList<>();

    public static void main(String[] args) {
        NbApp nb = new NbApp().setArgs(args).setPrintProcDoc(true);
        nb.getAppContext().setMainPackage("com.budwk");
        nb.run();
    }

    public void init(){
        // 加载设备协议
        loadProtocols();
        // 加载业务处理类
        loadProcessors();
        // 监听解析后的数据
        //listenMessage();
    }

    private void loadProtocols(){
        loader.loadProtocols("DEMO");

    }

    private void runProcessor(Processor processor, ProcessorContext context) {
        try {
            processor.process(context);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("业务{}处理出错{}", processor.getClass().getSimpleName(), e.toString());
        }
    }

    private void loadProcessors() {
        String[] namesByType = ioc.getNamesByType(Processor.class);
        this.processors = Arrays.stream(namesByType)
                .map(p -> ioc.get(Processor.class, p))
                .sorted(Comparator.comparingInt(Processor::getOrder)).
                collect(Collectors.toList());
    }
}

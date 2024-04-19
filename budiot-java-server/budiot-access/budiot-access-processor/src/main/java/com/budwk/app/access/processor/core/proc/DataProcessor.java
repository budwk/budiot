package com.budwk.app.access.processor.core.proc;

import com.budwk.app.access.processor.chain.DeviceChainData;
import com.budwk.app.access.processor.chain.DataProcessingChain;
import com.budwk.app.access.processor.core.Processor;
import com.budwk.app.access.processor.core.ProcessorContext;
import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import lombok.extern.slf4j.Slf4j;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * 数据处理动作链
 */
@IocBean(create = "init")
@Slf4j
public class DataProcessor implements Processor {
    @Inject
    private Ioc ioc;

    private DataProcessingChain processingChain;

    public void init() {
        processingChain = new DataProcessingChain(ioc);
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void process(ProcessorContext context) {
        DeviceProcessCache device = context.getDevice();
        DefaultDecodeResult decodeResult = context.getDecodeResult();
        DeviceChainData chainData = new DeviceChainData();
        chainData.setDeviceProcessCache(device);
        chainData.setDecodeResult(decodeResult);
        processingChain.doChain(chainData);
    }
}

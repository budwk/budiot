package com.budwk.app.access.processor.chain;


import com.budwk.app.access.processor.support.ThreadPoolSupport;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

@Slf4j
public abstract class AbstractDataProcessing implements DataProcessing {
    protected Executor executor = ThreadPoolSupport.getProcessorExecutor();
    protected DataProcessing next;

    public boolean isParallelizable() {
        return false;
    }

    @Override
    public void setNext(DataProcessing processing) {
        this.next = processing;
    }

    @Override
    public DataProcessing getNext() {
        return next;
    }

    protected void doNext(DeviceChainData deviceChainData) {
        if (null != next) {
            if (isParallelizable()) {
                executor.execute(() -> {
                    next.process(deviceChainData);
                });
            } else {
                next.process(deviceChainData);
            }
        }
    }
}
package com.budwk.app.access.processor.core;

public interface Processor {
    /**
     *
     * @return 排序
     */
    int getOrder();

    void process(ProcessorContext context);
}

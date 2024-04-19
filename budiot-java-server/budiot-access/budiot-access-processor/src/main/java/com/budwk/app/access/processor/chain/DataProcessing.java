package com.budwk.app.access.processor.chain;

public interface DataProcessing {
    void process(DeviceChainData deviceChainData);

    void setNext(DataProcessing processing);

    DataProcessing getNext();

    int getOrder();
}

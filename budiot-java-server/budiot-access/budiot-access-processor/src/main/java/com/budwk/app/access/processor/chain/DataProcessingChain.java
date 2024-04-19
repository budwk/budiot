package com.budwk.app.access.processor.chain;

import org.nutz.ioc.Ioc;
import org.nutz.lang.Lang;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class DataProcessingChain {
    private DataProcessing head;

    public DataProcessingChain(Ioc ioc) {
        loadProcessor(ioc);
    }

    protected void makeProcessing(List<DataProcessing> processors) {
        if (null != processors) {
            Iterator<DataProcessing> it = processors.iterator();
            if (it.hasNext()) {
                head = it.next();
                DataProcessing p = head;
                while (it.hasNext()) {
                    DataProcessing next = it.next();
                    p.setNext(next);
                    p = next;
                }
            }
        }
    }

    public void doChain(DeviceChainData deviceChainData) {
        if (null != head)
            try {
                head.process(deviceChainData);
            } catch (Throwable e) {
                throw Lang.wrapThrow(e);
            }
    }

    protected void loadProcessor(Ioc ioc) {
        String[] names = ioc.getNamesByType(DataProcessing.class);
        List<DataProcessing> list = Arrays.stream(names)
                .map(p -> ioc.get(DataProcessing.class, p))
                .sorted(Comparator.comparingInt(DataProcessing::getOrder)).
                collect(Collectors.toList());
        this.makeProcessing(list);
    }

}

package com.budwk.app.access.processor;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.processor.core.Processor;
import com.budwk.app.access.processor.core.ProcessorContext;
import com.budwk.app.access.processor.protocol.ProtolcolContainer;
import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.starter.rocketmq.enums.ConsumeMode;
import com.budwk.starter.rocketmq.enums.MessageModel;
import lombok.extern.slf4j.Slf4j;
import org.nutz.boot.NbApp;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;

import java.io.Serializable;
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
    private PropertiesProxy conf;
    @Inject
    private MessageTransfer messageTransfer;
    @Inject
    private IotDeviceService iotDeviceService;

    private List<Processor> processors = new ArrayList<>();

    public static void main(String[] args) {
        NbApp nb = new NbApp().setArgs(args).setPrintProcDoc(true);
        nb.getAppContext().setMainPackage("com.budwk");
        nb.run();
    }

    public void init() {
        // 设备协议解析
        if (conf.getBoolean("protocol.enable", false)) {
            ioc.get(ProtolcolContainer.class).init();
        }
        if (conf.getBoolean("processor.enable", false)) {
            // 加载业务处理类
            loadProcessors();
            // 监听解析后的数据
            listenMessage();
        }
    }

    private void listenMessage() {
        messageTransfer.subscribe("PROCESSOR", TopicConstant.DEVICE_DATA_PROCESSOR,
                "*", MessageModel.CLUSTERING, ConsumeMode.ORDERLY, this::process);
    }

    private void process(Message<Serializable> message) {
        try {
            log.info("process 解析后的数据 {} ", message);
            DefaultDecodeResult result = (DefaultDecodeResult) message.getBody();
            if (null == result || Lang.isEmpty(result.getMessages())) {
                log.warn("数据为空");
                return;
            }
            // 获取设备缓存
            DeviceProcessCache deviceCache = iotDeviceService.getCache(result.getDeviceId());
            if (null == deviceCache) {
                log.warn("设备 {} 不存在", result.getDeviceId());
                return;
            }

            //重新进行刷新下redis缓存,后续可根据时间判断进行刷新
            if (deviceCache.getRefreshTime() == null) {
                deviceCache = iotDeviceService.doRefreshCache(result.getDeviceId());
            }

            // 判断是否是集中器/采集器等网关设备
            boolean isGatewayDevice = Strings.isNotBlank(message.getHeader("gateway"));
            if (isGatewayDevice) {
                String parentId = message.getHeader("parentId");
                deviceCache.setParentId(parentId);
            }
            // 更新最新接收数据时间
            deviceCache.setLastReceiveTime(System.currentTimeMillis());
            String md5 = Lang.md5(deviceCache.toString());
            ProcessorContext context = new ProcessorContext(deviceCache, result);
            context.addProperties(message.getHeaders());
            //按顺序执行processor
            DeviceProcessCache finalDeviceCache = deviceCache;
            this.processors.stream().forEach(processor -> {
                try {
                    long st = System.currentTimeMillis();
                    this.runProcessor(processor, context);
                    String md5New = Lang.md5(finalDeviceCache.toString());
                    // 刷新设备缓存
                    if (!md5New.equals(md5)) {
                        flushDeviceCache(finalDeviceCache);
                    }
                    long ed = System.currentTimeMillis();
                    log.info("设备 {} 处理 {} 业务处理结束,耗时: {} ms", finalDeviceCache.getDeviceNo(), processor.getOrder(), (ed - st));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("子业务处理出错", e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            log.error("主业务处理出错", e);
        }
    }

    private void flushDeviceCache(DeviceProcessCache device) {
        iotDeviceService.doUpdateCache(device);
        if (device.isAbnormal()) {
            Iot_device update = new Iot_device();
            update.setId(device.getDeviceId());
            update.setAbnormal(true);
            update.setAbnormalTime(System.currentTimeMillis());
            iotDeviceService.updateIgnoreNull(update);
        }

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

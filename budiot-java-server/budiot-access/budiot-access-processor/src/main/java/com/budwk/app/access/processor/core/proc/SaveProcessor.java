package com.budwk.app.access.processor.core.proc;

import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.objects.dto.DeviceDTO;
import com.budwk.app.access.objects.dto.DeviceDataDTO;
import com.budwk.app.access.objects.dto.DeviceEventDataDTO;
import com.budwk.app.access.objects.dto.ValueItemDTO;
import com.budwk.app.access.processor.core.Processor;
import com.budwk.app.access.processor.core.ProcessorContext;
import com.budwk.app.access.processor.support.NamedThreadFactory;
import com.budwk.app.access.protocol.message.DeviceDataMessage;
import com.budwk.app.access.protocol.message.DeviceEventMessage;
import com.budwk.app.access.protocol.message.DeviceMessage;
import com.budwk.app.access.protocol.message.DeviceResponseMessage;
import com.budwk.app.access.storage.DeviceDataStorage;
import com.budwk.app.access.storage.DeviceEventDataStorage;
import com.budwk.app.iot.caches.DeviceCacheStore;
import com.budwk.app.iot.caches.ProductSubCacheStore;
import com.budwk.app.iot.enums.DeviceEventType;
import com.budwk.app.iot.enums.SubscribeType;
import com.budwk.app.iot.models.Iot_product_sub;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.iot.services.IotProductSubService;
import lombok.extern.slf4j.Slf4j;
import org.nutz.aop.interceptor.async.Async;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 数据存储动作链
 */
@IocBean
@Slf4j
public class SaveProcessor implements Processor {
    @Inject
    private MessageTransfer messageTransfer;

    @Inject
    private DeviceDataStorage deviceDataStorage;

    @Inject
    private DeviceEventDataStorage deviceEventDataStorage;

    @Inject
    private IotDeviceService iotDeviceService;
    @Inject
    private DeviceCacheStore deviceCacheStore;
    @Inject
    private IotProductSubService iotProductSubService;

    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4,
                    new NamedThreadFactory("save-processor"));

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public void process(ProcessorContext context) {
        DeviceProcessCache device = context.getDevice();
        DeviceDataDTO dto = new DeviceDataDTO();
        dto.setDevice_id(device.getDeviceId());
        dto.setDevice_no(device.getDeviceNo());
        dto.setMeter_no(device.getMeterNo());
        dto.setProduct_id(device.getProductId());
        dto.setProtocol_code(context.getProperty("protocolCode", String.class));
        dto.setTs(System.currentTimeMillis());
        log.debug("save-processor 数据保存 {} ", Json.toJson(dto));

        executorService.execute(() -> {
            context.getDecodeResult().getMessages().stream().sorted(Comparator.comparingLong(DeviceMessage::getTimestamp))
                    .forEach(message -> {
                        if (message instanceof DeviceDataMessage) {
                            ((DeviceDataMessage) message).setDeviceId(device.getDeviceId());
                            processData(dto, (DeviceDataMessage) message);
                            processSubscribe(device, message, SubscribeType.DATA);

                        } else if (message instanceof DeviceEventMessage) {
                            ((DeviceEventMessage) message).setDeviceId(device.getDeviceId());
                            processEvent(device, (DeviceEventMessage) message);
                            processSubscribe(device, message, SubscribeType.EVENT);

                        }
                    });
        });
    }

    private void processSubscribe(DeviceProcessCache device, DeviceMessage message, SubscribeType type) {
        String productId = device.getProductId();
        Map<String, Object> data = Lang.obj2map(message);
        data.put("timestamp", System.currentTimeMillis());
        data.put("type", type.text());
        data.put("deviceNo", device.getDeviceNo());
        data.put("meterNo", device.getMeterNo());

        List<Iot_product_sub> subscribeList = iotProductSubService.getSubCache(productId);
        for (Iot_product_sub dto : subscribeList) {
            if (type == dto.getSubType()) {
                pushToUrl(data, dto);
            }
        }
    }

    @Async
    private void pushToUrl(Map<String, Object> data, Iot_product_sub dto) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(dto.getUrl()))
                .POST(HttpRequest.BodyPublishers.ofString(Json.toJson(data, JsonFormat.tidy())))
                .header("Content-Type", "application/json")
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((stringHttpResponse, throwable) -> {
            if (null != throwable) {
                log.error("推送数据到 {} 失败", dto.getUrl(), throwable);
            }
        });
    }

    private void processEvent(DeviceProcessCache device, DeviceEventMessage message) {
        DeviceEventDataDTO eventDataDTO = new DeviceEventDataDTO();
        eventDataDTO.setDeviceNo(device.getDeviceNo());
        eventDataDTO.setMeterNo(device.getMeterNo());
        eventDataDTO.setDeviceId(device.getDeviceId());
        eventDataDTO.setProductId(device.getProductId());
        eventDataDTO.setEventData(message.getProperties()
                .stream()
                .map(it -> new ValueItemDTO<>(it.getCode(), it.getName(), it.getValue()))
                .collect(Collectors.toList()));
        eventDataDTO.setEventType(convertEventType(message.getEventType()).value());
        deviceEventDataStorage.save(eventDataDTO);
    }

    private DeviceEventType convertEventType(String type) {
        switch (type) {
            case "ALARM":
            case "ALARM_RECOVER":
                return DeviceEventType.ALARM;
            default:
                return DeviceEventType.INFO;
        }
    }

    private void processData(DeviceDataDTO dto, DeviceDataMessage message) {
        if (Lang.isNotEmpty(message.getProperties())) {
            message.addProperty("ts", message.getTimestamp());
            message.addProperty("receive_time", System.currentTimeMillis());
            deviceDataStorage.save(dto, message.getProperties());
        }
    }
}

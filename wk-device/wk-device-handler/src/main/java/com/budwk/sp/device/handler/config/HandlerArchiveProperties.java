package com.budwk.sp.device.handler.config;

import com.budwk.sp.device.enums.DeviceArchiveStorageType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wk.device.database-ext")
public class HandlerArchiveProperties {
    private Storage storage = new Storage();
    private boolean mongoEnabled = false;
    private String mongoUri = "";
    private String mongoCollectionPrefix = "device";

    public DeviceArchiveStorageType resolveCommandStorage() {
        DeviceArchiveStorageType type = storage.getCommand() == null ? DeviceArchiveStorageType.DEFAULT : storage.getCommand();
        if (type == DeviceArchiveStorageType.MONGODB && mongoEnabled) {
            return DeviceArchiveStorageType.MONGODB;
        }
        return DeviceArchiveStorageType.DEFAULT;
    }

    @Data
    public static class Storage {
        private DeviceArchiveStorageType command = DeviceArchiveStorageType.DEFAULT;
    }
}

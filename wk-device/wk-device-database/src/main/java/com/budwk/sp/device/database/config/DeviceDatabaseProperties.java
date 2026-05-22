package com.budwk.sp.device.database.config;

import com.budwk.sp.device.enums.DeviceArchiveStorageType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("deviceDatabaseProperties")
@ConfigurationProperties(prefix = "wk.device.database-ext")
public class DeviceDatabaseProperties {
    private String rawTopic = "wk.device.uplink.raw";
    private String rawArchiveTopic = "wk.device.uplink.raw.archive";
    private String normalizedTopic = "wk.device.uplink.normalized";
    private String eventTopic = "wk.device.event";
    private String downlinkTopic = "wk.device.downlink.command";
    private Storage storage = new Storage();
    private int messageRetentionDays = 15;
    private int dataRetentionDays = 30;
    private int eventRetentionDays = 150;
    private int commandRetentionDays = 180;
    private boolean mongoEnabled = false;
    private String mongoUri = "";
    private String mongoCollectionPrefix = "device";
    private boolean tdengineEnabled = false;
    private String tdengineUrl = "";
    private String tdengineDriverClassName = "";
    private String tdengineUsername = "root";
    private String tdenginePassword = "taosdata";
    private int tdengineBatchSize = 200;
    private int tdengineFlushSeconds = 5;

    public DeviceArchiveStorageType resolveRawStorage() {
        return resolve(storage.getRaw(), true, true);
    }

    public DeviceArchiveStorageType resolveDataStorage() {
        return resolve(storage.getData(), true, true);
    }

    public DeviceArchiveStorageType resolveEventStorage() {
        return resolve(storage.getEvent(), true, true);
    }

    public DeviceArchiveStorageType resolveCommandStorage() {
        return resolve(storage.getCommand(), true, false);
    }

    private DeviceArchiveStorageType resolve(DeviceArchiveStorageType configured, boolean mongoSupported, boolean tdengineSupported) {
        DeviceArchiveStorageType type = configured == null ? DeviceArchiveStorageType.DEFAULT : configured;
        if (type == DeviceArchiveStorageType.MONGODB && mongoSupported && mongoEnabled) {
            return DeviceArchiveStorageType.MONGODB;
        }
        if (type == DeviceArchiveStorageType.TDENGINE && tdengineSupported && tdengineEnabled) {
            return DeviceArchiveStorageType.TDENGINE;
        }
        return DeviceArchiveStorageType.DEFAULT;
    }

    @Data
    public static class Storage {
        private DeviceArchiveStorageType raw = DeviceArchiveStorageType.DEFAULT;
        private DeviceArchiveStorageType data = DeviceArchiveStorageType.DEFAULT;
        private DeviceArchiveStorageType event = DeviceArchiveStorageType.DEFAULT;
        private DeviceArchiveStorageType command = DeviceArchiveStorageType.DEFAULT;
    }
}

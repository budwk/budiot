package com.budwk.app.access.message.utils;

import com.budwk.app.access.message.Message;
import org.nustaq.serialization.FSTConfiguration;

import java.io.Serializable;

public class FastSerializeUtil {
    private static FSTConfiguration fstConfiguration = FSTConfiguration.createDefaultConfiguration();

    public static <T extends Serializable> byte[] serialize(Message<T> message) {
        return fstConfiguration.asByteArray(message);
    }

    public static <T extends Serializable> Message<T> deserialize(byte[] bytes) {
        if (null == bytes) {
            return null;
        }
        return (Message<T>) fstConfiguration.asObject(bytes);
    }
}

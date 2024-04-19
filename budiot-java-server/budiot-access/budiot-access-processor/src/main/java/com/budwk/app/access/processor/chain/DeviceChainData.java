package com.budwk.app.access.processor.chain;

import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
public class DeviceChainData implements Serializable {
    private static final long serialVersionUID = -17232049118179474L;

    private DefaultDecodeResult decodeResult;
    private DeviceProcessCache deviceProcessCache;
    /**
     * todo 扩展其他采集数据
     */

    /**
     * 存一下上一步处理的一些信息，下一步可以直接从这里获取
     */
    private Map<String, Object> extInfo = new ConcurrentHashMap<>();

    public DeviceChainData addExtInfo(String key, Object o) {
        extInfo.put(key, o);
        return this;
    }
}

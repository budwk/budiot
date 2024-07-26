package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.models.Iot_product_attr;
import com.budwk.app.iot.models.Iot_product_firmware_device;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.database.service.BaseService;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;

import java.util.List;

/**
 * @author wizzer
 */
public interface IotProductFirmwareDeviceService extends BaseService<Iot_product_firmware_device> {
    String importData(String productId, String firmwareId, String fileName, List<Iot_product_firmware_device> list, boolean over, String userId, String loginname);
}
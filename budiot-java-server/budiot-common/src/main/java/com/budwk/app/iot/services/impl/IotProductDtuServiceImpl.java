package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_dtu;
import com.budwk.app.iot.services.IotProductDtuService;
import com.budwk.starter.database.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.lang.util.NutMap;

/**
 * @author wizzer
 */
@IocBean(args = {"refer:dao"})
@Slf4j
public class IotProductDtuServiceImpl extends BaseServiceImpl<Iot_product_dtu> implements IotProductDtuService {
    public IotProductDtuServiceImpl(Dao dao) {
        super(dao);
    }

    public void save(Iot_product_dtu productDtu) {
        Iot_product_dtu dbDtu = this.fetch(Cnd.where("productId", "=", productDtu.getProductId()));
        if (dbDtu != null) {
            int newVersion = dbDtu.getVersion() + 1;
            productDtu.setVersion(newVersion);
            productDtu.setConfig(getDtuConfig(productDtu.getConfig(), String.valueOf(newVersion)));
            productDtu.setEnabled(null);
            productDtu.setId(dbDtu.getId());
            this.updateIgnoreNull(productDtu);
        } else {
            productDtu.setVersion(1);
            productDtu.setConfig(getDtuConfig(productDtu.getConfig(), String.valueOf(1)));
            productDtu.setEnabled(false);
            this.insert(productDtu);
        }
        log.info("config:::" + Json.toJson(productDtu));
    }

    private String getDtuConfig(String config, String version) {
        // 处理config内容，替换版本号
        NutMap nutMap = Json.fromJson(NutMap.class, config);
        nutMap.put("param_ver", version);
        return Json.toJson(nutMap, JsonFormat.tidy());
    }
}
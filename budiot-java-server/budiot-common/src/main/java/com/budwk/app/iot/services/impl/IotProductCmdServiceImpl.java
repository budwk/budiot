package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.enums.DeviceDataType;
import com.budwk.app.iot.models.Iot_product_cmd;
import com.budwk.app.iot.models.Iot_product_cmd_attr;
import com.budwk.app.iot.services.IotProductCmdAttrService;
import com.budwk.app.iot.services.IotProductCmdService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.lang.Lang;
import org.nutz.lang.util.NutMap;

import java.util.ArrayList;
import java.util.List;

@IocBean(args = {"refer:dao"})
public class IotProductCmdServiceImpl extends BaseServiceImpl<Iot_product_cmd> implements IotProductCmdService {
    public IotProductCmdServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private IotProductCmdAttrService iotProductCmdAttrService;

    public String importData(String productId, String json, boolean over, String userId, String loginname) {
        List<NutMap> jsonList = Json.fromJsonAsList(NutMap.class, json);
        List<Iot_product_cmd> list = new ArrayList<>();
        for (NutMap map : jsonList) {
            Iot_product_cmd cmd = Lang.map2Object(map, Iot_product_cmd.class);
            List<NutMap> attrList = map.getAsList("attrList", NutMap.class);
            List<Iot_product_cmd_attr> attrs = new ArrayList<>();
            for (NutMap attrMap : attrList) {
                NutMap dataType = attrMap.getAs("dataType", NutMap.class);
                Iot_product_cmd_attr attr = Lang.map2Object(attrMap, Iot_product_cmd_attr.class);
                attr.setDataType(DeviceDataType.from(dataType.getInt("value")));
                attrs.add(attr);
            }
            cmd.setAttrList(attrs);
            list.add(cmd);
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder resultMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Iot_product_cmd cmd : list) {
            cmd.setProductId(productId);
            cmd.setCreatedBy(userId);
            try {
                if (over) {
                    Iot_product_cmd dbCmd = this.fetch(Cnd.where("code", "=", cmd.getCode()));
                    if (dbCmd != null) {
                        cmd.setId(dbCmd.getId());
                        cmd.setUpdatedBy(userId);
                        this.updateIgnoreNull(cmd);
                        iotProductCmdAttrService.clear(Cnd.where("cmdId", "=", cmd.getId()));
                        this.insertLinks(cmd, "attrList");
                    } else {
                        this.insertLinks(this.insert(cmd), "attrList");
                    }
                } else {
                    this.insertLinks(this.insert(cmd), "attrList");
                }
                successNum++;
            } catch (Exception e) {
                String duplicate = "";
                if (e.getMessage().contains("Duplicate")) {
                    duplicate = "已存在";
                } else {
                    duplicate = e.getMessage();
                }
                failureNum++;
                String msg = "<br/>" + failureNum + "、指令标识：" + cmd.getCode() + " " + duplicate + "<br/>";
                failureMsg.append(msg);
            }
        }
        resultMsg.insert(0, "导入结果：共成功 " + successNum + " 条");
        if (failureNum > 0) {
            resultMsg.append("，失败 " + failureNum + " 条，失败数据如下：<br/>");
            resultMsg.append(failureMsg);
        }
        return resultMsg.toString();

    }
}

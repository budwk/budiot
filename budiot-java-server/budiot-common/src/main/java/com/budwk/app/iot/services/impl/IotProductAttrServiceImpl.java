package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.enums.DeviceAttrType;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.models.Iot_product_attr;
import com.budwk.app.iot.services.IotProductAttrService;
import com.budwk.app.iot.services.IotProductService;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

import java.util.List;

@IocBean(args = {"refer:dao"})
public class IotProductAttrServiceImpl extends BaseServiceImpl<Iot_product_attr> implements IotProductAttrService {
    public IotProductAttrServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private IotProductService iotProductService;

    public String importData(String productId, String fileName, List<Iot_product_attr> list, boolean over, String userId, String loginname) {
        if (list == null || list.size() == 0) {
            throw new BaseException("导入数据不能为空！");
        }
        Iot_product product = iotProductService.fetch(productId);
        if (product == null) {
            throw new BaseException("产品不存在");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder resultMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Iot_product_attr attr : list) {
            attr.setProductId(productId);
            attr.setCreatedBy(userId);
            attr.setUpdatedBy(userId);
            try {
                if (over) {
                    Iot_product_attr dbAttr = this.fetch(Cnd.where("code", "=", attr.getCode()));
                    if(dbAttr!=null) {
                        attr.setId(dbAttr.getId());
                        this.updateIgnoreNull(attr);
                    }else {
                        this.insert(attr);
                    }
                } else {
                    this.insert(attr);
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
                String msg = "<br/>" + failureNum + "、参数标识：" + attr.getCode() + " " + duplicate + "<br/>";
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

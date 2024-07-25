package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.enums.IotPlatform;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.models.Iot_product_menu;
import com.budwk.app.iot.services.IotProductMenuService;
import com.budwk.app.iot.services.IotProductService;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProductServiceImpl extends BaseServiceImpl<Iot_product> implements IotProductService {
    public IotProductServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private IotProductMenuService iotProductMenuService;

    public void create(Iot_product product) {
        if (product.getIotPlatform() == null) {
            throw new BaseException("产品接入平台未设置");
        }
        this.insert(product);
        this.createProductMenu(product.getId());
        if (product.getIotPlatform() == IotPlatform.OPENLUATDTU) {
            // 合宙DTU 打开DTU配置产品菜单
            iotProductMenuService.update(
                    Chain.make("display", true),
                    Cnd.where("productId", "=", product.getId()).
                            and("code", "=", "dtuparam"));
            iotProductMenuService.update(
                    Chain.make("display", false),
                    Cnd.where("productId", "=", product.getId()).
                            and("code", "in", new String[]{"event", "command", "subscribe"}));
        }

    }

    /**
     * 创建默认产品菜单
     *
     * @param productId
     */
    private void createProductMenu(String productId) {
        Iot_product_menu menu = new Iot_product_menu();
        menu.setProductId(productId);
        menu.setName("基本信息");
        menu.setCode("detail");
        menu.setSys(true);
        menu.setDisplay(true);
        iotProductMenuService.insert(menu);
        menu = new Iot_product_menu();
        menu.setProductId(productId);
        menu.setName("设备列表");
        menu.setCode("device");
        menu.setSys(false);
        menu.setDisplay(true);
        iotProductMenuService.insert(menu);
        menu = new Iot_product_menu();
        menu.setProductId(productId);
        menu.setName("事件列表");
        menu.setCode("event");
        menu.setSys(false);
        menu.setDisplay(true);
        iotProductMenuService.insert(menu);
        menu = new Iot_product_menu();
        menu.setProductId(productId);
        menu.setName("指令列表");
        menu.setCode("command");
        menu.setSys(false);
        menu.setDisplay(true);
        iotProductMenuService.insert(menu);
        menu = new Iot_product_menu();
        menu.setProductId(productId);
        menu.setName("订阅管理");
        menu.setCode("subscribe");
        menu.setSys(false);
        menu.setDisplay(true);
        iotProductMenuService.insert(menu);
        menu = new Iot_product_menu();
        menu.setProductId(productId);
        menu.setName("DTU参数管理");
        menu.setCode("dtuparam");
        menu.setSys(false);
        menu.setDisplay(false);
        iotProductMenuService.insert(menu);
        menu = new Iot_product_menu();
        menu.setProductId(productId);
        menu.setName("固件管理");
        menu.setCode("firmware");
        menu.setSys(false);
        menu.setDisplay(false);
        iotProductMenuService.insert(menu);
        menu = new Iot_product_menu();
        menu.setProductId(productId);
        menu.setName("产品配置");
        menu.setCode("config");
        menu.setSys(true);
        menu.setDisplay(true);
        iotProductMenuService.insert(menu);
    }
}

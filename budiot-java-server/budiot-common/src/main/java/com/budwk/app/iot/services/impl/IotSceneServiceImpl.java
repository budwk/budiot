package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_scene;
import com.budwk.app.iot.models.Iot_scene_action;
import com.budwk.app.iot.models.Iot_scene_term;
import com.budwk.app.iot.services.IotSceneService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.aop.interceptor.ioc.TransAop;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.aop.Aop;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;

@IocBean(args = {"refer:dao"})
public class IotSceneServiceImpl extends BaseServiceImpl<Iot_scene> implements IotSceneService {
    public IotSceneServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    @Aop(TransAop.READ_COMMITTED)
    public void save(Iot_scene iotScene) {
        this.insert(iotScene);
        if (!Lang.isEmpty(iotScene.getTerms())) {
            this.dao().insertLinks(iotScene, "terms");
        }
        if (!Lang.isEmpty(iotScene.getActions())) {
            this.dao().insertLinks(iotScene, "actions");
        }
    }

    @Override
    @Aop(TransAop.READ_COMMITTED)
    public void update(Iot_scene iotScene) {
        this.updateIgnoreNull(iotScene);
        this.dao().clear(Iot_scene_term.class, Cnd.where("sceneId", "=", iotScene.getId()));
        this.dao().clear(Iot_scene_action.class, Cnd.where("sceneId", "=", iotScene.getId()));
        if (!Lang.isEmpty(iotScene.getTerms())) {
            this.dao().insertLinks(iotScene, "terms");
        }
        if (!Lang.isEmpty(iotScene.getActions())) {
            this.dao().insertLinks(iotScene, "actions");
        }
    }

    @Override
    @Aop(TransAop.READ_COMMITTED)
    public void delete(Iot_scene iotScene) {
        this.delete(iotScene.getId());
        this.dao().clear(Iot_scene_term.class, Cnd.where("sceneId", "=", iotScene.getId()));
        this.dao().clear(Iot_scene_action.class, Cnd.where("sceneId", "=", iotScene.getId()));
    }
}
package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_classify;
import com.budwk.app.iot.models.Iot_scene_space;
import com.budwk.app.iot.services.IotSceneSpaceService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.aop.interceptor.ioc.TransAop;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.ioc.aop.Aop;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;

@IocBean(args = {"refer:dao"})
public class IotSceneSpaceServiceImpl extends BaseServiceImpl<Iot_scene_space> implements IotSceneSpaceService {
    public IotSceneSpaceServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    @Aop(TransAop.READ_COMMITTED)
    public void save(Iot_scene_space sceneSpace, String pid){
        String path = "";
        if (!Strings.isEmpty(pid)) {
            Iot_scene_space pp = this.fetch(pid);
            path = pp.getPath();
        }
        sceneSpace.setPath(getSubPath("iot_scene_space", "path", path));
        sceneSpace.setParentId(pid);
        dao().insert(sceneSpace);
        if (!Strings.isEmpty(pid)) {
            this.update(Chain.make("hasChildren", true), Cnd.where("id", "=", pid));
        }
    }

    @Override
    @Aop(TransAop.READ_COMMITTED)
    public void deleteAndChild(Iot_scene_space sceneSpace){
        dao().execute(Sqls.create("delete from iot_scene_space where path like @path").setParam("path", sceneSpace.getPath() + "%"));
        if (!Strings.isEmpty(sceneSpace.getParentId())) {
            int count = count(Cnd.where("parentId", "=", sceneSpace.getParentId()));
            if (count < 1) {
                dao().execute(Sqls.create("update iot_scene_space set hasChildren=0 where id=@pid").setParam("pid", sceneSpace.getParentId()));
            }
        }
    }
}
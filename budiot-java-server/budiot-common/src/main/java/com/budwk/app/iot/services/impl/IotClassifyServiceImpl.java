package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_classify;
import com.budwk.app.iot.services.IotClassifyService;
import com.budwk.app.sys.models.Sys_dict;
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
public class IotClassifyServiceImpl extends BaseServiceImpl<Iot_classify> implements IotClassifyService {
    public IotClassifyServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    @Aop(TransAop.READ_COMMITTED)
    public void save(Iot_classify classify, String pid){
        String path = "";
        if (!Strings.isEmpty(pid)) {
            Iot_classify pp = this.fetch(pid);
            path = pp.getPath();
        }
        classify.setPath(getSubPath("iot_classify", "path", path));
        classify.setParentId(pid);
        dao().insert(classify);
        if (!Strings.isEmpty(pid)) {
            this.update(Chain.make("hasChildren", true), Cnd.where("id", "=", pid));
        }
    }

    @Override
    @Aop(TransAop.READ_COMMITTED)
    public void deleteAndChild(Iot_classify classify){
        dao().execute(Sqls.create("delete from iot_classify where path like @path").setParam("path", classify.getPath() + "%"));
        if (!Strings.isEmpty(classify.getParentId())) {
            int count = count(Cnd.where("parentId", "=", classify.getParentId()));
            if (count < 1) {
                dao().execute(Sqls.create("update iot_classify set hasChildren=0 where id=@pid").setParam("pid", classify.getParentId()));
            }
        }
    }
}

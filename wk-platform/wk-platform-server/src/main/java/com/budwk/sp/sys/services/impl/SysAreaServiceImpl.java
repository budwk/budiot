package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_area;
import com.budwk.sp.sys.services.SysAreaService;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wizzer.cn
 */
@Service
@CacheConfig(cacheNames = "sys_area")
public class SysAreaServiceImpl extends BaseServiceImpl<Sys_area> implements SysAreaService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysAreaServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    @Cacheable
    public List<Sys_area> getSubListByCode(String code) {
        if (Strings.isNotBlank(code)) {
            Sys_area area = this.fetch(Cnd.where("code", "=", code));
            return area == null ? new ArrayList<>() : this.query(Cnd.where("parentId", "=", Strings.sNull(area.getId())).asc("location"));
        } else {
            return this.query(Cnd.where("parentId", "=", "").asc("location"));
        }
    }

    @Override
    @Cacheable
    public List<Sys_area> getSubListByCode(String filedName, String code) {
        if (Strings.isNotBlank(code)) {
            Sys_area dict = this.fetch(Cnd.where("code", "=", code));
            return dict == null ? new ArrayList<>() : this.query(filedName, Cnd.where("parentId", "=", Strings.sNull(dict.getId())).asc("location"));
        } else {
            return this.query(filedName, Cnd.where("parentId", "=", "").asc("location"));
        }
    }

    @Override
    @Cacheable
    public NutMap getSubMapByCode(String code) {
        if (Strings.isNotBlank(code)) {
            Sys_area area = this.fetch(Cnd.where("code", "=", code));
            return area == null ? NutMap.NEW() : this.getNutMap(Sqls.create("select code,name from sys_area where parentId = @id order by location asc").setParam("id", area.getId()));
        } else {
            return this.getNutMap(Sqls.create("select code,name from sys_area where parentId = '' order by location asc"));
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void save(Sys_area area, String pid) {
        String path = "";
        if (!Strings.isEmpty(pid)) {
            Sys_area pp = this.fetch(pid);
            path = pp.getPath();
        }
        area.setPath(getSubPath("sys_area", "path", path));
        area.setParentId(pid);
        dao().insert(area);
        if (!Strings.isEmpty(pid)) {
            this.update(Chain.make("hasChildren", true), Cnd.where("id", "=", pid));
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void deleteAndChild(Sys_area area) {
        dao().execute(Sqls.create("delete from sys_area where path like @path").setParam("path", area.getPath() + "%"));
        if (!Strings.isEmpty(area.getParentId())) {
            int count = count(Cnd.where("parentId", "=", area.getParentId()));
            if (count < 1) {
                dao().execute(Sqls.create("update sys_area set hasChildren=0 where id=@pid").setParam("pid", area.getParentId()));
            }
        }
    }

    @Override
    @CacheEvict(allEntries = true)
    @Async
    public void cacheClear() {

    }
}

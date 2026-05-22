package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_dict;
import com.budwk.sp.sys.services.SysDictService;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.lang.Strings;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wizzer@qq.com
 */
@Service
@CacheConfig(cacheNames = "sys_dict")
public class SysDictServiceImpl extends BaseServiceImpl<Sys_dict> implements SysDictService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysDictServiceImpl(Dao dao) {
        super(dao);
    }

    /**
     * 通过code获取name
     *
     * @param code 字典标识
     * @return
     */
    @Override
    @Cacheable
    public String getNameByCode(String code) {
        Sys_dict dict = this.fetch(Cnd.where("code", "=", code));
        return dict == null ? "" : dict.getName();
    }

    /**
     * 通过id获取name
     *
     * @param id ID
     * @return
     */
    @Override
    @Cacheable
    public String getNameById(String id) {
        Sys_dict dict = this.fetch(id);
        return dict == null ? "" : dict.getName();
    }


    @Override
    @Cacheable
    public List<Sys_dict> getSubListByPath(String path) {
        return this.query(Cnd.where("path", "like", Strings.sNull(path) + "____").asc("location"));
    }

    @Override
    @Cacheable
    public List<Sys_dict> getSubListByCode(String code) {
        Sys_dict dict = this.fetch(Cnd.where("code", "=", code));
        return dict == null ? new ArrayList<>() : this.query(Cnd.where("parentId", "=", Strings.sNull(dict.getId())).asc("location"));
    }

    @Override
    @Cacheable
    public List<Sys_dict> getSubListByCode(String filedName, String code) {
        Sys_dict dict = this.fetch(Cnd.where("code", "=", code));
        return dict == null ? new ArrayList<>() : this.query(filedName, Cnd.where("parentId", "=", Strings.sNull(dict.getId())).asc("location"));
    }

    /**
     * 通过父id获取下级列表
     *
     * @param id ID
     * @return
     */
    @Override
    @Cacheable
    public List<Sys_dict> getSubListById(String id) {
        return this.query(Cnd.where("parentId", "=", Strings.sNull(id)).asc("location"));
    }

    @Override
    @Cacheable
    public Map<String, String> getSubMapByPath(String path) {
        return this.getMap(Sqls.create("select code,name from sys_dict where path like @path order by location asc").setParam("path", path + "____"));
    }

    @Override
    @Cacheable
    public Map<String, String> getSubMapByCode(String code) {
        Sys_dict dict = this.fetch(Cnd.where("code", "=", code));
        return dict == null ? new HashMap<String, String>() : this.getMap(Sqls.create("select code,name from sys_dict where parentId = @id order by location asc").setParam("id", dict.getId()));
    }

    /**
     * 通过id获取下级map
     *
     * @param id ID
     * @return
     */
    @Override
    @Cacheable
    public Map<String, String> getSubMapById(String id) {
        return this.getMap(Sqls.create("select code,name from sys_dict where parentId = @id order by location asc").setParam("id", id));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void save(Sys_dict dict, String pid) {
        String path = "";
        if (!Strings.isEmpty(pid)) {
            Sys_dict pp = this.fetch(pid);
            path = pp.getPath();
        }
        dict.setPath(getSubPath("sys_dict", "path", path));
        dict.setParentId(pid);
        dao().insert(dict);
        if (!Strings.isEmpty(pid)) {
            this.update(Chain.make("hasChildren", true), Cnd.where("id", "=", pid));
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void deleteAndChild(Sys_dict dict) {
        dao().execute(Sqls.create("delete from sys_dict where path like @path").setParam("path", dict.getPath() + "%"));
        if (!Strings.isEmpty(dict.getParentId())) {
            int count = count(Cnd.where("parentId", "=", dict.getParentId()));
            if (count < 1) {
                dao().execute(Sqls.create("update sys_dict set hasChildren=0 where id=@pid").setParam("pid", dict.getParentId()));
            }
        }
    }

    @Override
    @CacheEvict(allEntries = true)
    @Async
    public void cacheClear() {

    }
}

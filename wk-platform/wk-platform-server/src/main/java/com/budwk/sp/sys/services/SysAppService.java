package com.budwk.sp.sys.services;

import com.budwk.sp.sys.entity.Sys_app;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;

/**
 * @author wizzer@qq.com
 */
public interface SysAppService extends BaseService<Sys_app> {

    List<Sys_app> listAll();

    List<Sys_app> listEnable();

    void cacheClear();
}

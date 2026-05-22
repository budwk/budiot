package com.budwk.sp.sys.providers;

import com.budwk.sp.sys.entity.Sys_app;

import java.util.List;

/**
 * 系统应用
 * @author wizzer@qq.com
 */
public interface ISysAppProvider {

    /**
     * 获取所有应用列表
     * @return
     */
    List<Sys_app> listAll();

    /**
     * 获取启用的应用列表
     * @return
     */
    List<Sys_app> listEnable();
}
